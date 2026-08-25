package com.crudao.kanban.evento;

/** Tipos de evento de board propagados via {@link EventoBoardPublisher} (RNF-001, ADR-004). */
public enum TipoEventoBoard {
    TAREFA_CRIADA,
    TAREFA_MOVIDA,
    TAREFA_EXCLUIDA
}
