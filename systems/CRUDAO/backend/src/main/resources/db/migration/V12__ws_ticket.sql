-- Ticket de curta duração para autenticar o handshake STOMP/SockJS a partir do browser (TASK-07.2).
--
-- O frontend (Next.js/BFF, TASK-07.1) nunca expõe o access token ao JS do browser; a WebSocket API
-- nativa também não permite enviar header Authorization no handshake. O Next.js troca (server-side,
-- com o Bearer real) esse ticket opaco de uso único e TTL curtíssimo, que o browser usa só para
-- abrir a conexão STOMP (decisão validada com architect+security — ver memory/state.md).
CREATE TABLE ws_ticket (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID NOT NULL REFERENCES usuario (id),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_em   TIMESTAMPTZ NOT NULL,
    usado       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ws_ticket_expira_em ON ws_ticket (expira_em);
