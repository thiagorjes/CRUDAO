-- PapelPermissaoAuditoria — auditoria de alterações em toggles de permissão (RF-016, RN-017,
-- achado do Comitê de Análise — Security). TASK-02.3 é a dona única desta migration; TASK-04.4 e
-- TASK-06.1 apenas referenciam a tabela já existente.

CREATE TABLE papel_permissao_auditoria (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    papel_id        UUID NOT NULL REFERENCES papel (id),
    permissao_id    UUID NOT NULL REFERENCES permissao (id),
    autor_id        UUID NOT NULL REFERENCES usuario (id),
    valor_anterior  BOOLEAN NOT NULL,
    valor_novo      BOOLEAN NOT NULL,
    data_hora       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_papel_permissao_auditoria_papel_data ON papel_permissao_auditoria (papel_id, data_hora);
