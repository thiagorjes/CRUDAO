-- Histórico de etapa/impedimento (base de lead-time, RF-006) e auditoria (RF-017) da Tarefa.

CREATE TABLE tarefa_etapa_historico (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tarefa_id   UUID NOT NULL REFERENCES tarefa (id),
    etapa_id    UUID NOT NULL REFERENCES etapa (id),
    entrada_em  TIMESTAMPTZ NOT NULL,
    saida_em    TIMESTAMPTZ
);

-- (tarefaId, entradaEm): histórico da tarefa. (etapaId, saidaEm): agregação de lead-time médio por
-- etapa no dashboard (RF-007) sem seq scan (achado do Comitê de Análise — Database).
CREATE INDEX idx_tarefa_etapa_historico_tarefa ON tarefa_etapa_historico (tarefa_id, entrada_em);
CREATE INDEX idx_tarefa_etapa_historico_etapa ON tarefa_etapa_historico (etapa_id, saida_em);

CREATE TABLE tarefa_impedimento_historico (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tarefa_id       UUID NOT NULL REFERENCES tarefa (id),
    marcado_em      TIMESTAMPTZ NOT NULL,
    desmarcado_em   TIMESTAMPTZ
);

CREATE INDEX idx_tarefa_impedimento_historico_tarefa ON tarefa_impedimento_historico (tarefa_id, marcado_em);

CREATE TABLE tarefa_auditoria (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tarefa_id       UUID NOT NULL REFERENCES tarefa (id),
    autor_id        UUID NOT NULL REFERENCES usuario (id),
    campo           VARCHAR(50) NOT NULL,
    valor_anterior  TEXT,
    valor_novo      TEXT,
    data_hora       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tarefa_auditoria_tarefa ON tarefa_auditoria (tarefa_id, data_hora);
