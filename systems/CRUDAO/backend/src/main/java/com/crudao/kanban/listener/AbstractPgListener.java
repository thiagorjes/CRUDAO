package com.crudao.kanban.listener;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;

/**
 * Base comum dos listeners por pod de canal {@code LISTEN} do Postgres (ADR-004) — extraída em
 * TASK-05.3 a partir de {@code BoardEventListener} (TASK-05.1) e {@code NotificacaoEventListener}
 * (TASK-05.2), que replicavam o mesmo loop de conexão dedicada, reconexão com backoff exponencial
 * (1s→30s) e métricas Micrometer (achado de code review, agent QA — duplicação sem extração).
 *
 * <p>Conexão JDBC própria via {@link DriverManager} (fora do pool Hikari, mesmo motivo documentado
 * em {@code ListenNotifyPublisher}). Cada subclasse só implementa {@link #processarNotificacao},
 * que decodifica o payload do seu canal e chama {@link #registrarLatencia} com o instante do
 * {@code pg_notify}.
 */
public abstract class AbstractPgListener {

    private final Logger log;
    private final String canal;
    private final long backoffInicialMs;
    private final long backoffMaximoMs;
    private final String url;
    private final String username;
    private final String password;
    private final Counter reconexoesCounter;
    private final Timer latenciaTimer;

    private volatile boolean ativo = true;
    private volatile boolean conectado = false;
    private volatile boolean primeiraConexao = true;
    // Protected (não private) para permitir simular queda de conexão em teste — ver
    // BoardEventoNotifyIT/NotificacaoEventoNotifyIT.
    protected volatile Connection conexaoAtual;
    private Thread thread;

    protected AbstractPgListener(
            Logger log,
            String canal,
            MeterRegistry meterRegistry,
            String url,
            String username,
            String password) {
        this(log, canal, 1000, 30_000, meterRegistry, url, username, password);
    }

    protected AbstractPgListener(
            Logger log,
            String canal,
            long backoffInicialMs,
            long backoffMaximoMs,
            MeterRegistry meterRegistry,
            String url,
            String username,
            String password) {
        this.log = log;
        this.canal = canal;
        this.backoffInicialMs = backoffInicialMs;
        this.backoffMaximoMs = backoffMaximoMs;
        this.url = url;
        this.username = username;
        this.password = password;
        this.reconexoesCounter =
                Counter.builder("kanban.evento.listener.reconexoes")
                        .tag("canal", canal)
                        .description("Reconexões do listener LISTEN após queda da conexão JDBC (TASK-05.3)")
                        .register(meterRegistry);
        this.latenciaTimer =
                Timer.builder("kanban.evento.listener.latencia")
                        .tag("canal", canal)
                        .description("Latência entre pg_notify e o broadcast STOMP correspondente (TASK-05.3)")
                        .register(meterRegistry);
    }

    @PostConstruct
    public void iniciar() {
        thread = new Thread(this::loop, canal + "-listener");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    public void parar() {
        ativo = false;
        // getNotifications(timeout) é I/O de socket bloqueante que não responde a
        // thread.interrupt() — fechar a conexão diretamente é o que de fato desbloqueia a leitura
        // (IOException imediata), em vez de esperar até o timeout do poll (achado de code review,
        // agent QA, TASK-05.1).
        Connection conexao = conexaoAtual;
        if (conexao != null) {
            try {
                conexao.close();
            } catch (Exception e) {
                // ignorado — a conexão está sendo descartada de qualquer forma
            }
        }
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void loop() {
        long backoffMs = backoffInicialMs;
        while (ativo) {
            try (Connection connection = DriverManager.getConnection(url, username, password)) {
                conexaoAtual = connection;
                try (Statement statement = connection.createStatement()) {
                    statement.execute("LISTEN " + canal);
                }
                PGConnection pgConnection = connection.unwrap(PGConnection.class);
                conectado = true;
                if (!primeiraConexao) {
                    reconexoesCounter.increment();
                }
                primeiraConexao = false;
                backoffMs = backoffInicialMs;
                log.info("Listener LISTEN {} conectado", canal);

                while (ativo) {
                    PGNotification[] notificacoes = pgConnection.getNotifications(5000);
                    if (notificacoes != null) {
                        for (PGNotification notificacao : notificacoes) {
                            processarNotificacao(notificacao.getParameter());
                        }
                    }
                }
            } catch (Exception e) {
                conectado = false;
                conexaoAtual = null;
                if (!ativo) {
                    return;
                }
                log.warn("Listener LISTEN {} desconectado — reconectando em {}ms", canal, backoffMs, e);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                backoffMs = Math.min(backoffMs * 2, backoffMaximoMs);
                if (backoffMs >= backoffMaximoMs) {
                    log.error("Listener LISTEN {} não reconecta há {}ms — verificar disponibilidade do Postgres", canal, backoffMs);
                }
            }
        }
    }

    /** Decodifica e retransmite o payload recebido do canal — implementado por cada subclasse. */
    protected abstract void processarNotificacao(String payloadJson);

    /** Registra a latência NOTIFY→broadcast a partir do instante do {@code pg_notify} no payload. */
    protected void registrarLatencia(long publicadoEmEpochMillis) {
        long latenciaMs = System.currentTimeMillis() - publicadoEmEpochMillis;
        latenciaTimer.record(Math.max(latenciaMs, 0), TimeUnit.MILLISECONDS);
    }

    public boolean isConectado() {
        return conectado;
    }

    /**
     * Fecha a conexão JDBC atual, se houver — não é parte do contrato operacional, existe só para
     * simular uma queda de conexão externa em teste (ver {@code BoardEventoNotifyIT}/{@code
     * NotificacaoEventoNotifyIT}), sem expor o campo {@code Connection} em si fora do pacote.
     */
    public void fecharConexaoAtualParaTeste() {
        Connection conexao = conexaoAtual;
        if (conexao != null) {
            try {
                conexao.close();
            } catch (Exception e) {
                // ignorado — só interessa provocar a queda
            }
        }
    }
}
