-- Auditoria de alteração de PapelPermissao (RF-016, RN-017 — achado do Comitê de Análise/Security:
-- previne autoconcessão de privilégio, registra quem alterou o toggle de qual permissão/papel).
CREATE TABLE papel_permissao_auditoria (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    papel_id       UUID NOT NULL REFERENCES papel (id) ON DELETE CASCADE,
    permissao_id   UUID NOT NULL REFERENCES permissao (id),
    autor_id       UUID NOT NULL REFERENCES usuario (id),
    valor_anterior BOOLEAN NOT NULL,
    valor_novo     BOOLEAN NOT NULL,
    data_hora      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_papel_permissao_auditoria_papel ON papel_permissao_auditoria (papel_id);
