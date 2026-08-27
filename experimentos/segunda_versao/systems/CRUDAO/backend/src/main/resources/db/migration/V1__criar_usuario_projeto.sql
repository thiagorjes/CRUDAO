-- Usuario e Projeto — base fundacional para autenticação e RBAC (RF-008, RF-014).

CREATE TABLE usuario (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_sub   VARCHAR(255) NOT NULL,
    nome           VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_usuario_keycloak_sub UNIQUE (keycloak_sub),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE projeto (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome           VARCHAR(255) NOT NULL,
    descricao      TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    criado_por_id  UUID NOT NULL REFERENCES usuario (id),
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    finalizado_em  TIMESTAMPTZ,
    CONSTRAINT ck_projeto_status CHECK (status IN ('ATIVO', 'FINALIZADO'))
);

CREATE INDEX idx_projeto_status ON projeto (status);
