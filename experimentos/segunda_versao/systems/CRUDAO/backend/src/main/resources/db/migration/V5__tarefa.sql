-- Tarefa (card do board) e TarefaObservador (RF-018, RF-005).

CREATE TABLE tarefa (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id          UUID NOT NULL REFERENCES projeto (id),
    workflow_id         UUID NOT NULL REFERENCES workflow (id),
    etapa_atual_id      UUID NOT NULL REFERENCES etapa (id),
    raia_id             UUID NOT NULL REFERENCES raia (id),
    titulo              VARCHAR(255) NOT NULL,
    descricao_escopo    TEXT,
    responsavel_id      UUID REFERENCES usuario (id),
    criado_por_id       UUID NOT NULL REFERENCES usuario (id),
    iniciada            BOOLEAN NOT NULL DEFAULT FALSE,
    impedida            BOOLEAN NOT NULL DEFAULT FALSE,
    impedida_desde       TIMESTAMPTZ,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Suporte à query do board (projetoId + etapaAtualId) e à listagem por responsável.
CREATE INDEX idx_tarefa_projeto_etapa_atual ON tarefa (projeto_id, etapa_atual_id);
CREATE INDEX idx_tarefa_responsavel ON tarefa (responsavel_id);

-- Observadores explícitos (RF-005) — responsável e criador são observadores implícitos.
CREATE TABLE tarefa_observador (
    tarefa_id   UUID NOT NULL REFERENCES tarefa (id),
    usuario_id  UUID NOT NULL REFERENCES usuario (id),
    PRIMARY KEY (tarefa_id, usuario_id)
);
