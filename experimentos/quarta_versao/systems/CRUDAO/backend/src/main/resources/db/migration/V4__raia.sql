CREATE TABLE raia (
    id UUID PRIMARY KEY,
    projeto_id UUID REFERENCES projeto(id) ON DELETE CASCADE,
    nome VARCHAR(255) NOT NULL,
    ordem INT NOT NULL
);

CREATE INDEX idx_raia_projeto_ordem ON raia (projeto_id, ordem);

-- Seed da raia default global (projeto_id = NULL) - RN-CB-005
INSERT INTO raia (id, projeto_id, nome, ordem)
VALUES ('00000000-0000-0000-0000-000000000001', NULL, 'Padrão', 1);

