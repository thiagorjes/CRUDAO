CREATE TABLE tarefa_etapa_historico (
    id UUID PRIMARY KEY,
    tarefa_id UUID NOT NULL REFERENCES tarefa(id) ON DELETE CASCADE,
    etapa_id UUID NOT NULL REFERENCES etapa(id) ON DELETE RESTRICT,
    entrada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    saida_em TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_tarefa_etapa_hist_tarefa_entrada ON tarefa_etapa_historico (tarefa_id, entrada_em);
CREATE INDEX idx_tarefa_etapa_hist_etapa_saida ON tarefa_etapa_historico (etapa_id, saida_em);

CREATE TABLE tarefa_impedimento_historico (
    id UUID PRIMARY KEY,
    tarefa_id UUID NOT NULL REFERENCES tarefa(id) ON DELETE CASCADE,
    marcado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    desmarcado_em TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_tarefa_impedimento_hist_marcado ON tarefa_impedimento_historico (tarefa_id, marcado_em);

CREATE TABLE tarefa_auditoria (
    id UUID PRIMARY KEY,
    tarefa_id UUID NOT NULL REFERENCES tarefa(id) ON DELETE CASCADE,
    autor_id UUID NOT NULL REFERENCES usuario(id),
    campo VARCHAR(50) NOT NULL,
    valor_anterior TEXT,
    valor_novo TEXT,
    data_hora TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_tarefa_auditoria_tarefa_data ON tarefa_auditoria (tarefa_id, data_hora);

