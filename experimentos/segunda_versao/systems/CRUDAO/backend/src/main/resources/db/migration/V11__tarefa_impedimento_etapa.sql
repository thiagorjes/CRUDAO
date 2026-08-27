-- Atribui o impedimento à etapa em que ocorreu (RF-007, dashboard de lead-time por etapa).
-- Nullable: registros de impedimento anteriores a esta migration ficam sem etapa e são
-- excluídos da agregação por etapa em DashboardService (não é possível reconstituir o
-- histórico retroativamente).

ALTER TABLE tarefa_impedimento_historico ADD COLUMN etapa_id UUID REFERENCES etapa (id);

CREATE INDEX idx_tarefa_impedimento_historico_etapa ON tarefa_impedimento_historico (etapa_id, desmarcado_em);
