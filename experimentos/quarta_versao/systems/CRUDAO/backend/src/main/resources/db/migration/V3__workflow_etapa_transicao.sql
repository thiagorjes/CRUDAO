CREATE TABLE workflow (
    id UUID PRIMARY KEY,
    projeto_id UUID NOT NULL REFERENCES projeto(id) ON DELETE CASCADE,
    nome VARCHAR(255) NOT NULL
);

CREATE TABLE etapa (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES workflow(id) ON DELETE CASCADE,
    nome VARCHAR(255) NOT NULL,
    ordem INT NOT NULL,
    etapa_final BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_etapa_workflow_ordem ON etapa (workflow_id, ordem);

CREATE TABLE transicao (
    id UUID PRIMARY KEY,
    etapa_origem_id UUID NOT NULL REFERENCES etapa(id) ON DELETE CASCADE,
    etapa_destino_id UUID NOT NULL REFERENCES etapa(id) ON DELETE CASCADE,
    CONSTRAINT uk_transicao_origem_destino UNIQUE (etapa_origem_id, etapa_destino_id)
);

