package com.crudao.kanban.notificacao;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificacaoResponse(
        UUID id, UUID tarefaId, String tipo, String mensagem, boolean lida, OffsetDateTime criadoEm) {}
