-- Notificação interna (RF-005) — gerada ao mudar etapa/impedimento de tarefa.

CREATE TABLE notificacao (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID NOT NULL REFERENCES usuario (id),
    tarefa_id   UUID NOT NULL REFERENCES tarefa (id),
    tipo        VARCHAR(30) NOT NULL,
    mensagem    VARCHAR(500) NOT NULL,
    lida        BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Query da lista de notificações não lidas do usuário (GET /api/notificacoes), mais recentes
-- primeiro (data-model.md).
CREATE INDEX idx_notificacao_usuario_lida ON notificacao (usuario_id, lida, criado_em);
