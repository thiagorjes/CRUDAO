package com.crudao.kanban.tarefa;

import java.util.UUID;

/**
 * Campos aceitos dependem de {@code Tarefa.iniciada} ({@code contracts/tarefas.md}): com a tarefa
 * iniciada, {@code titulo}/{@code descricaoEscopo} não podem ser enviados (congelados, RN-016) —
 * apenas {@code responsavelId} (RN-012).
 *
 * <p>{@code removerResponsavel=true} desatribui o responsável explicitamente — sem ele, {@code
 * responsavelId=null} é indistinguível de "campo não enviado" (achado de code review, agent QA,
 * TASK-04.2). Exige {@code tarefa:gerenciar}.
 */
public record EditarTarefaRequest(
        String titulo, String descricaoEscopo, UUID responsavelId, boolean removerResponsavel) {}
