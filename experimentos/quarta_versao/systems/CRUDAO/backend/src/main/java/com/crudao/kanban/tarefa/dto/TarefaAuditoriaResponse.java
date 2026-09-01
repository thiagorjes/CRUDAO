package com.crudao.kanban.tarefa.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Projeção de {@link com.crudao.kanban.domain.tarefa.TarefaAuditoria} para a API (RF-017).
 *
 * <p>Não serializa a entidade JPA diretamente: {@code TarefaAuditoria.tarefa}/{@code .autor} são
 * associações lazy e o app roda com {@code spring.jpa.open-in-view=false} — serializar a entidade
 * fora da transação dispara {@code LazyInitializationException}. Este DTO resolve o nome do autor
 * dentro da transação de leitura.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarefaAuditoriaResponse {
    private UUID id;
    private String campo;
    private String valorAnterior;
    private String valorNovo;
    private Instant dataHora;
    private UUID autorId;
    private String autorNome;
}
