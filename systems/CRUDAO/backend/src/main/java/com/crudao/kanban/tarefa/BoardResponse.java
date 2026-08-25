package com.crudao.kanban.tarefa;

import com.crudao.kanban.domain.tarefa.TarefaBoardItemResponse;
import com.crudao.kanban.raia.RaiaResponse;
import com.crudao.kanban.workflow.EtapaResponse;
import java.util.List;

/** Estado completo do board — etapas × raias × cards (RF-001, {@code contracts/tarefas.md}). */
public record BoardResponse(
        List<EtapaResponse> etapas, List<RaiaResponse> raias, List<TarefaBoardItemResponse> tarefas) {}
