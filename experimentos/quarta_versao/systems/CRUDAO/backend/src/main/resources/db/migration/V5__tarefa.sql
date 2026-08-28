CREATE TABLE tarefa (
    id UUID PRIMARY KEY,
    projeto_id UUID NOT NULL REFERENCES projeto(id) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES workflow(id) ON DELETE RESTRICT,
    etapa_atual_id UUID NOT NULL REFERENCES etapa(id) ON DELETE RESTRICT,
    raia_id UUID NOT NULL REFERENCES raia(id) ON DELETE RESTRICT,
    titulo VARCHAR(255) NOT NULL,
    descricao_escopo TEXT,
    responsavel_id UUID REFERENCES usuario(id) ON DELETE SET NULL,
    criado_por_id UUID NOT NULL REFERENCES usuario(id),
    iniciada BOOLEAN NOT NULL DEFAULT FALSE,
    impedida BOOLEAN NOT NULL DEFAULT FALSE,
    impedida_desde TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tarefa_projeto_etapa ON tarefa (projeto_id, etapa_atual_id);
CREATE INDEX idx_tarefa_responsavel ON tarefa (responsavel_id);

CREATE TABLE tarefa_observador (
    tarefa_id UUID NOT NULL REFERENCES tarefa(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    PRIMARY KEY (tarefa_id, usuario_id)
);

