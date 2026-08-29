CREATE TABLE notificacao (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    tarefa_id UUID NOT NULL REFERENCES tarefa(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,
    lida BOOLEAN NOT NULL DEFAULT false,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    lido_em TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notificacao_usuario_lida ON notificacao (usuario_id, lida);
CREATE INDEX idx_notificacao_tarefa_criado ON notificacao (tarefa_id, criado_em);
