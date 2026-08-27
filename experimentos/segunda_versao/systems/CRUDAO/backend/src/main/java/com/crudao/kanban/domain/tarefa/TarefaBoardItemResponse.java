package com.crudao.kanban.domain.tarefa;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Card do board (RF-001) — projeção de {@link Tarefa} via JPQL {@code SELECT NEW}
 * ({@link TarefaRepository#buscarItensDoBoard}), sem tocar em associações {@code lazy}.
 *
 * <p>Vive em {@code domain.tarefa} (não em {@code tarefa}, camada de serviço/web) porque é
 * construído diretamente pelo JPQL do repositório — colocá-lo na camada de serviço inverteria a
 * dependência {@code domain} → serviço (achado de code review, agent QA, TASK-04.5).
 * {@code BoardService} apenas repassa estas instâncias dentro de {@link
 * com.crudao.kanban.tarefa.BoardResponse}.
 */
public record TarefaBoardItemResponse(
        UUID id,
        String titulo,
        UUID etapaAtualId,
        UUID raiaId,
        UUID responsavelId,
        boolean impedida,
        OffsetDateTime impedidaDesde,
        boolean iniciada) {}
