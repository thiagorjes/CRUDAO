package com.crudao.kanban.tarefa;

import java.util.UUID;

/**
 * Campos aceitos dependem de {@code Tarefa.iniciada} ({@code contracts/tarefas.md}): com a tarefa
 * iniciada, {@code titulo}/{@code descricaoEscopo} não podem ser enviados (congelados, RN-016) —
 * apenas {@code responsavelId} (RN-012).
 */
public record EditarTarefaRequest(String titulo, String descricaoEscopo, UUID responsavelId) {}
