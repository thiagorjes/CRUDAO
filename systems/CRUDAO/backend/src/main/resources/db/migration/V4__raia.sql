-- Raia (swimlane) — agrupamento visual de tarefas no board (RF-011, RN-CB-005).

CREATE TABLE raia (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id  UUID REFERENCES projeto (id),
    nome        VARCHAR(255) NOT NULL,
    ordem       INT NOT NULL,
    CONSTRAINT uk_raia_projeto_ordem UNIQUE (projeto_id, ordem)
);

-- Raia default global — projeto_id NULL, usada quando o card é criado sem raia própria (RN-CB-005).
INSERT INTO raia (id, projeto_id, nome, ordem)
VALUES (gen_random_uuid(), NULL, 'Padrão', 0);
