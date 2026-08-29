package com.crudao.kanban.notificacao;

import com.crudao.kanban.domain.notificacao.Notificacao;
import com.crudao.kanban.domain.notificacao.NotificacaoRepository;
import com.crudao.kanban.domain.notificacao.TipoNotificacao;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaObservadorRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.evento.NotificacaoEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TASK-05.2: Serviço de notificações internas para observadores de tarefas.
 *
 * Responsabilidades:
 * - Resolver lista de observadores (responsável + criador + observadores explícitos)
 * - Criar notificações por observador
 * - Publicar eventos de notificação via EventPublisher
 * - Fornecer endpoints de leitura e marcação de lida
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final TarefaObservadorRepository tarefaObservadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Cria notificações para observadores quando tarefa muda de etapa.
     * Observadores: responsável + criador + observadores explícitos.
     * TASK-05.2: RF-005 (notificações internas).
     */
    @Transactional
    public void criarNotificacoesPorTransicaoEtapa(Tarefa tarefa, UUID etapaOrigemId, UUID etapaDestinoId) {
        try {
            Set<UUID> observadores = resolverObservadores(tarefa);

            for (UUID usuarioId : observadores) {
                // Ignora notificação para o próprio usuário que fez a ação (será recuperado do contexto auth se necessário)
                Usuario usuario = usuarioRepository.findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + usuarioId));

                Notificacao notif = Notificacao.builder()
                        .usuario(usuario)
                        .tarefa(tarefa)
                        .tipo(TipoNotificacao.TRANSICAO_ETAPA)
                        .lida(false)
                        .criadoEm(Instant.now())
                        .build();

                notificacaoRepository.save(notif);

                // Publica evento para retransmissão via STOMP
                publicarEventoNotificacao(
                    "TRANSICAO_ETAPA",
                    usuarioId,
                    tarefa.getId(),
                    etapaOrigemId,
                    etapaDestinoId
                );
            }

            log.debug("Notificações de transição criadas para tarefa {}: {} observadores",
                tarefa.getId(), observadores.size());
        } catch (Exception e) {
            log.error("Erro ao criar notificações de transição para tarefa {}", tarefa.getId(), e);
            // Não falha a transação principal (best effort, conforme ADR-004)
        }
    }

    /**
     * Cria notificações quando impedimento é marcado.
     */
    @Transactional
    public void criarNotificacoesPorImpedimentoMarcado(Tarefa tarefa) {
        try {
            Set<UUID> observadores = resolverObservadores(tarefa);

            for (UUID usuarioId : observadores) {
                Usuario usuario = usuarioRepository.findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + usuarioId));

                Notificacao notif = Notificacao.builder()
                        .usuario(usuario)
                        .tarefa(tarefa)
                        .tipo(TipoNotificacao.IMPEDIMENTO_MARCADO)
                        .lida(false)
                        .criadoEm(Instant.now())
                        .build();

                notificacaoRepository.save(notif);

                publicarEventoNotificacao(
                    "IMPEDIMENTO_MARCADO",
                    usuarioId,
                    tarefa.getId(),
                    null,
                    null
                );
            }

            log.debug("Notificações de impedimento marcado criadas para tarefa {}: {} observadores",
                tarefa.getId(), observadores.size());
        } catch (Exception e) {
            log.error("Erro ao criar notificações de impedimento marcado para tarefa {}", tarefa.getId(), e);
        }
    }

    /**
     * Cria notificações quando impedimento é desmarcado.
     */
    @Transactional
    public void criarNotificacoesPorImpedimentoDesmarcado(Tarefa tarefa) {
        try {
            Set<UUID> observadores = resolverObservadores(tarefa);

            for (UUID usuarioId : observadores) {
                Usuario usuario = usuarioRepository.findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + usuarioId));

                Notificacao notif = Notificacao.builder()
                        .usuario(usuario)
                        .tarefa(tarefa)
                        .tipo(TipoNotificacao.IMPEDIMENTO_DESMARCADO)
                        .lida(false)
                        .criadoEm(Instant.now())
                        .build();

                notificacaoRepository.save(notif);

                publicarEventoNotificacao(
                    "IMPEDIMENTO_DESMARCADO",
                    usuarioId,
                    tarefa.getId(),
                    null,
                    null
                );
            }

            log.debug("Notificações de impedimento desmarcado criadas para tarefa {}: {} observadores",
                tarefa.getId(), observadores.size());
        } catch (Exception e) {
            log.error("Erro ao criar notificações de impedimento desmarcado para tarefa {}", tarefa.getId(), e);
        }
    }

    /**
     * Resolve a lista de observadores de uma tarefa.
     * Observadores: responsável (se houver) + criador (sempre) + observadores explícitos.
     */
    private Set<UUID> resolverObservadores(Tarefa tarefa) {
        Set<UUID> observadores = new HashSet<>();

        // Responsável (se atribuído)
        if (tarefa.getResponsavel() != null && tarefa.getResponsavel().getId() != null) {
            observadores.add(tarefa.getResponsavel().getId());
        }

        // Criador (sempre)
        if (tarefa.getCriadoPor() != null && tarefa.getCriadoPor().getId() != null) {
            observadores.add(tarefa.getCriadoPor().getId());
        }

        // Observadores explícitos
        var observadoresExplicitos = tarefaObservadorRepository.findByTarefaId(tarefa.getId());
        for (var obs : observadoresExplicitos) {
            if (obs.getUsuario() != null && obs.getUsuario().getId() != null) {
                observadores.add(obs.getUsuario().getId());
            }
        }

        return observadores;
    }

    /**
     * Publica evento de notificação para retransmissão via STOMP.
     */
    private void publicarEventoNotificacao(
            String tipo,
            UUID usuarioId,
            UUID tarefaId,
            UUID etapaOrigemId,
            UUID etapaDestinoId) {
        try {
            // HashMap (não Map.of) porque etapaOrigemId/etapaDestinoId são null no fluxo de
            // impedimento — Map.of lança NPE com valores nulos.
            Map<String, Object> payload = new HashMap<>();
            payload.put("tipo", tipo);
            payload.put("usuarioId", usuarioId.toString());
            payload.put("tarefaId", tarefaId.toString());
            payload.put("etapaOrigemId", etapaOrigemId != null ? etapaOrigemId.toString() : null);
            payload.put("etapaDestinoId", etapaDestinoId != null ? etapaDestinoId.toString() : null);
            payload.put("timestamp", Instant.now().toEpochMilli());

            String payloadJson = objectMapper.writeValueAsString(payload);

            NotificacaoEventPublisher.NotificacaoEventPayload evento =
                new NotificacaoEventPublisher.NotificacaoEventPayload(
                    tipo,
                    usuarioId,
                    tarefaId,
                    0L, // Sequência será atribuída pelo adapter
                    payloadJson
                );

            eventPublisher.publicar(evento);
        } catch (Exception e) {
            log.error("Erro ao publicar evento de notificação", e);
            // Best effort: não falha a transação
        }
    }

    /**
     * Retorna notificações não lidas do usuário autenticado.
     */
    @Transactional(readOnly = true)
    public List<Notificacao> obterNaoLidas(UUID usuarioId) {
        return notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByCriadoEmDesc(usuarioId);
    }

    /**
     * Marca notificação como lida com validação de autorização.
     * RNF-003: Usuário só pode marcar suas próprias notificações como lidas.
     */
    @Transactional
    public void marcarComoLidaComAutorizacao(UUID notificacaoId, UUID usuarioIdAutenticado) {
        var notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada"));

        // Valida que notificação pertence ao usuário autenticado
        if (!notificacao.getUsuario().getId().equals(usuarioIdAutenticado)) {
            throw new IllegalArgumentException("Usuário não tem permissão para acessar esta notificação");
        }

        notificacao.setLida(true);
        notificacao.setLidoEm(Instant.now());
        notificacaoRepository.save(notificacao);
    }

    /**
     * Marca notificação como lida (sem validação — para uso interno).
     */
    @Transactional
    public void marcarComoLida(UUID notificacaoId) {
        var notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada"));

        notificacao.setLida(true);
        notificacao.setLidoEm(Instant.now());
        notificacaoRepository.save(notificacao);
    }
}
