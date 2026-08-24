package com.crudao.kanban.realtime;

/** Tipos de evento de board transmitidos em tempo real (RF-005). */
public enum TipoEventoBoard {
  TAREFA_CRIADA,
  TAREFA_MOVIDA,
  IMPEDIMENTO_ALTERADO,
  TAREFA_EXCLUIDA
}
