package com.crudao.kanban.domain.websocket;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WsTicketRepository extends JpaRepository<WsTicket, UUID> {

    /** Purga periódica (TASK-07.2, code review) — tickets expirados nunca são reaproveitáveis. */
    long deleteByExpiraEmBefore(OffsetDateTime limite);
}
