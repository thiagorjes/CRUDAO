package com.crudao.kanban.domain.websocket;

import com.crudao.kanban.domain.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * Ticket opaco de uso único e TTL curtíssimo para autenticar o handshake STOMP/SockJS a partir do
 * browser (TASK-07.2) — ver {@code V12__ws_ticket.sql} e {@code WsTicketService}.
 */
@Entity
@Table(name = "ws_ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WsTicket {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    @Column(nullable = false)
    private boolean usado = false;
}
