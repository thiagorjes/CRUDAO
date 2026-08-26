package com.crudao.kanban.websocket;

import java.time.OffsetDateTime;

public record WsTicketResponse(String ticket, OffsetDateTime expiraEm) {}
