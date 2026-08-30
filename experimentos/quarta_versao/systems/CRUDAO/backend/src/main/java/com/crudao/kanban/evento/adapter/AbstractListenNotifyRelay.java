package com.crudao.kanban.evento.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Base comum dos adapters de LISTEN/NOTIFY (TASK-05.1 + resiliência TASK-05.3).
 *
 * Responsabilidades compartilhadas por board e notificações:
 * <ul>
 *   <li>Publicar {@code NOTIFY <canal>, '<envelope_json>'} após o commit da transação
 *       (via {@link TransactionSynchronization#afterCommit()}), best-effort — nunca falha a transação.</li>
 *   <li>Manter uma thread dedicada em {@code LISTEN <canal>} com <b>reconexão automática infinita</b>
 *       e backoff exponencial (1s → teto de 30s); logs progressivos WARN→ERROR.</li>
 *   <li>Retransmitir cada notificação via STOMP para o destino resolvido pela subclasse.</li>
 *   <li>Expor estado de conexão para o readiness probe e métricas Micrometer
 *       (contador de reconexões e latência NOTIFY→broadcast STOMP) por canal.</li>
 * </ul>
 *
 * <p>Envelope publicado: {@code {"seq":<long>,"ts":<epochMillis>,"data":<payload_cru>}}.
 * O {@code ts} permite medir a latência entre o {@code NOTIFY} e o broadcast STOMP (RNF-001).
 *
 * <p>ADR-004: o {@code LISTEN} de qualquer pod recebe eventos publicados por qualquer outro pod.
 * ADR-002: usa o {@link DataSource} do pool.
 *
 * @param <P> tipo do payload de domínio (record da porta correspondente).
 */
@Slf4j
public abstract class AbstractListenNotifyRelay<P> {

    /** Limite de payload do PostgreSQL para NOTIFY (8000 bytes com folga sobre os 8KB). */
    private static final int MAX_PAYLOAD_BYTES = 8000;

    private static final long BACKOFF_BASE_MS = 1_000L;
    private static final long BACKOFF_MAX_MS = 30_000L;

    /** Acima deste número de tentativas seguidas, o log de falha sobe de WARN para ERROR. */
    private static final int LOG_ESCALATION_THRESHOLD = 3;

    protected final DataSource dataSource;
    protected final SimpMessagingTemplate messagingTemplate;
    protected final ObjectMapper objectMapper;

    private final Counter reconnectionsCounter;
    private final Timer notifyToStompTimer;

    private volatile Connection listenConnection;
    private volatile boolean running = false;
    private volatile boolean conectado = false;
    private final AtomicLong sequenceCounter = new AtomicLong(0);
    private Thread listenerThread;

    protected AbstractListenNotifyRelay(
            DataSource dataSource,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.dataSource = dataSource;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.reconnectionsCounter =
                Counter.builder("kanban.listener.reconnections")
                        .description("Reconexões da conexão JDBC do listener LISTEN/NOTIFY, por canal e pod")
                        .tag("canal", canal())
                        .register(meterRegistry);
        this.notifyToStompTimer =
                Timer.builder("kanban.listener.notify_to_stomp")
                        .description("Latência entre o NOTIFY no Postgres e o broadcast STOMP, por canal")
                        .tag("canal", canal())
                        .register(meterRegistry);
    }

    // ------------------------------------------------------------------
    // Contrato da subclasse
    // ------------------------------------------------------------------

    /** Nome do canal PostgreSQL (ex.: {@code board_events}). Também é a tag {@code canal} das métricas. */
    protected abstract String canal();

    /** Serializa o payload de domínio para o JSON cru que vai dentro do envelope. */
    protected abstract String serializarPayload(P evento) throws JsonProcessingException;

    /** Resolve o destino STOMP a partir do nó {@code data} do envelope recebido. */
    protected abstract String destinoStomp(JsonNode data);

    /**
     * JSON mínimo e válido usado no lugar do payload quando este excede o limite de 8KB.
     * Deve conter os campos que {@link #destinoStomp(JsonNode)} precisa (ex.: {@code projetoId})
     * e um marcador {@code "truncado":true} para o cliente disparar o {@code GET /board} de
     * resincronização (mitigação do trade-off de payload do ADR-004).
     */
    protected abstract String payloadResync(P evento);

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    @PostConstruct
    public void start() {
        log.info("Iniciando listener LISTEN/NOTIFY do canal '{}'", canal());
        running = true;
        listenerThread = new Thread(this::loopListen, canal() + "-listener");
        listenerThread.setDaemon(false);
        listenerThread.start();
    }

    @PreDestroy
    public void stop() {
        log.info("Parando listener do canal '{}'", canal());
        running = false;
        conectado = false;
        closeQuietly(listenConnection);
        if (listenerThread != null) {
            try {
                listenerThread.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Estado consumido pelo readiness probe: {@code true} quando o {@code LISTEN} está ativo. */
    public boolean isConectado() {
        return conectado;
    }

    /** Nome do canal, exposto para health indicators fora do pacote. */
    public String nomeCanal() {
        return canal();
    }

    // ------------------------------------------------------------------
    // Publicação (NOTIFY)
    // ------------------------------------------------------------------

    /**
     * Publica o evento via {@code NOTIFY} após o commit da transação atual (ou imediatamente,
     * se não houver transação ativa). Best-effort: exceções são logadas, nunca propagadas.
     */
    protected void publicar(P evento) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publicarViaNotify(evento);
                        }
                    });
        } else {
            publicarViaNotify(evento);
        }
    }

    private void publicarViaNotify(P evento) {
        long seq = sequenceCounter.incrementAndGet();
        try {
            String payloadJson = serializarPayload(evento);
            if (payloadJson.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
                // Não trunca o JSON (quebraria o envelope): publica um marcador válido de resync.
                log.warn(
                        "Payload do canal '{}' excede {} bytes (seq={}); publicando marcador de resync",
                        canal(), MAX_PAYLOAD_BYTES, seq);
                payloadJson = payloadResync(evento);
            }
            String envelope =
                    String.format(
                            "{\"seq\":%d,\"ts\":%d,\"data\":%s}",
                            seq, Instant.now().toEpochMilli(), payloadJson);
            String sanitized = envelope.replace("\\", "\\\\").replace("'", "''");
            if (sanitized.indexOf('\0') >= 0) {
                log.warn("Payload do canal '{}' contém null byte — descartado (seq={})", canal(), seq);
                return;
            }
            try (Connection conn = dataSource.getConnection();
                    Statement stmt = conn.createStatement()) {
                stmt.execute(String.format("NOTIFY %s, E'%s'", canal(), sanitized));
                log.debug("NOTIFY publicado no canal '{}' seq={}", canal(), seq);
            }
        } catch (JsonProcessingException e) {
            log.error("Falha ao serializar evento do canal '{}' (seq={})", canal(), seq, e);
        } catch (SQLException e) {
            log.error("Falha ao executar NOTIFY no canal '{}' (seq={})", canal(), seq, e);
        }
    }

    // ------------------------------------------------------------------
    // Escuta (LISTEN) com reconexão resiliente
    // ------------------------------------------------------------------

    private void loopListen() {
        int tentativasFalha = 0;
        boolean reconexao = false; // primeira conexão não conta como reconexão
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                escutar(reconexao);
                tentativasFalha = 0; // saída limpa de escutar() (running=false)
            } catch (SQLException e) {
                conectado = false;
                if (!running) {
                    break;
                }
                tentativasFalha++;
                reconexao = true;
                long delay = backoffDelayMs(tentativasFalha);
                if (tentativasFalha <= LOG_ESCALATION_THRESHOLD) {
                    log.warn(
                            "Conexão LISTEN do canal '{}' caiu (tentativa {}). Reconectando em {}ms",
                            canal(), tentativasFalha, delay, e);
                } else {
                    log.error(
                            "Conexão LISTEN do canal '{}' segue indisponível (tentativa {}). Reconectando em {}ms",
                            canal(), tentativasFalha, delay, e);
                }
                if (!sleep(delay)) {
                    break;
                }
            }
        }
        conectado = false;
        log.info("Listener do canal '{}' finalizado", canal());
    }

    /**
     * Abre a conexão, emite {@code LISTEN} e drena notificações até {@code running} virar false.
     *
     * @param reconexao {@code true} se esta é uma reconexão após queda — conta em
     *     {@code kanban.listener.reconnections} (uma vez por reconexão bem-sucedida, não por tentativa).
     */
    private void escutar(boolean reconexao) throws SQLException {
        try {
            listenConnection = dataSource.getConnection();
            PGConnection pgConn = listenConnection.unwrap(PGConnection.class);
            try (Statement stmt = listenConnection.createStatement()) {
                stmt.execute("LISTEN " + canal());
            }
            conectado = true;
            if (reconexao) {
                reconnectionsCounter.increment();
            }
            log.info("Listener do canal '{}' {}", canal(), reconexao ? "reconectado" : "conectado");

            while (running) {
                PGNotification[] notifications = pgConn.getNotifications(100);
                if (notifications != null) {
                    for (PGNotification notif : notifications) {
                        retransmitir(notif);
                    }
                }
            }
        } finally {
            conectado = false;
            closeQuietly(listenConnection);
            listenConnection = null;
        }
    }

    private void retransmitir(PGNotification notif) {
        try {
            JsonNode envelope = objectMapper.readTree(notif.getParameter());
            JsonNode data = envelope.path("data");
            long ageMs = System.currentTimeMillis() - envelope.path("ts").asLong(System.currentTimeMillis());
            if (ageMs >= 0) {
                notifyToStompTimer.record(ageMs, TimeUnit.MILLISECONDS);
            }
            String destino = destinoStomp(data);
            messagingTemplate.convertAndSend(destino, envelope);
            log.debug(
                    "Evento do canal '{}' retransmitido para {} seq={} latencia={}ms",
                    canal(), destino, envelope.path("seq").asLong(), ageMs);
        } catch (Exception e) {
            log.error("Falha ao retransmitir notificação do canal '{}'", canal(), e);
        }
    }

    // ------------------------------------------------------------------
    // Utilitários
    // ------------------------------------------------------------------

    /**
     * Backoff exponencial com teto: {@code min(1s * 2^(n-1), 30s)}. Cresce 1s, 2s, 4s… até 30s
     * e permanece em 30s enquanto a conexão não voltar (reconexão infinita — rede de segurança).
     */
    static long backoffDelayMs(int tentativasFalha) {
        if (tentativasFalha <= 0) {
            return 0L;
        }
        int shift = Math.min(tentativasFalha - 1, 30);
        long delay = BACKOFF_BASE_MS << shift;
        return Math.min(Math.max(delay, BACKOFF_BASE_MS), BACKOFF_MAX_MS);
    }

    private boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.debug("Erro ao fechar conexão LISTEN do canal '{}'", canal(), e);
            }
        }
    }
}
