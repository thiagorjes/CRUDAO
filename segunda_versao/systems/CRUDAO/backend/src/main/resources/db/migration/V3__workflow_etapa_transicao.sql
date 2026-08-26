-- Workflow, Etapa e Transicao — motor de workflow configurável (RF-002, RF-009, RF-010).

CREATE TABLE workflow (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id  UUID NOT NULL REFERENCES projeto (id),
    nome        VARCHAR(255) NOT NULL
);

CREATE TABLE etapa (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id   UUID NOT NULL REFERENCES workflow (id),
    nome          VARCHAR(255) NOT NULL,
    ordem         INT NOT NULL,
    etapa_final   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_etapa_workflow_ordem UNIQUE (workflow_id, ordem)
);

CREATE TABLE transicao (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etapa_origem_id    UUID NOT NULL REFERENCES etapa (id),
    etapa_destino_id   UUID NOT NULL REFERENCES etapa (id),
    CONSTRAINT uk_transicao_origem_destino UNIQUE (etapa_origem_id, etapa_destino_id)
);
