package com.crudao.kanban.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica eventos de board via {@code pg_notify} no canal {@link
 * PostgresNotificationListener#CANAL} (ADR-004). Cada pod inscrito no canal recebe a notificação e
 * retransmite via STOMP aos clientes conectados localmente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventoBoardPublisher {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Chamado a partir de {@code afterCommit} da transação de negócio (ver {@code TarefaService}) —
   * a mutação já foi persistida quando este método roda. Uma falha aqui não desfaz a operação de
   * negócio (comportamento aceito pelo ADR-004); é logada para permitir diagnóstico, em vez de
   * propagar uma exceção sem contexto para dentro do callback de sincronização de transação.
   */
  public void publicar(TipoEventoBoard tipo, UUID tarefaId, UUID projetoId) {
    try {
      String payload =
          objectMapper.writeValueAsString(new NotificacaoMinima(tipo, tarefaId, projetoId));
      // pg_notify retorna void — usa ConnectionCallback em vez de update()/queryForObject(), que
      // esperam contagem de linhas ou um valor de coluna tipado.
      jdbcTemplate.execute(
          (ConnectionCallback<Void>)
              conexao -> {
                try (PreparedStatement statement =
                    conexao.prepareStatement("SELECT pg_notify(?, ?)")) {
                  statement.setString(1, PostgresNotificationListener.CANAL);
                  statement.setString(2, payload);
                  statement.execute();
                }
                return null;
              });
    } catch (Exception e) {
      log.error(
          "Falha ao publicar evento de board (tipo={}, tarefaId={}, projetoId={}) — clientes"
              + " conectados não serão notificados desta mudança.",
          tipo,
          tarefaId,
          projetoId,
          e);
    }
  }
}
