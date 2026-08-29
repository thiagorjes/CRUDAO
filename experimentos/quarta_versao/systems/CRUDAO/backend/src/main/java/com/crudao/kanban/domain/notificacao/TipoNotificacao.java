package com.crudao.kanban.domain.notificacao;

/**
 * Tipos de notificações internas para observadores de tarefas.
 * TASK-05.2: Notificações geradas quando etapa ou impedimento mudam.
 */
public enum TipoNotificacao {
    TRANSICAO_ETAPA,
    IMPEDIMENTO_MARCADO,
    IMPEDIMENTO_DESMARCADO
}
