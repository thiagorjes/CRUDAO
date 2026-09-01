"use client";

import { useState } from "react";
import type { TarefaDetalhe } from "@/lib/types";

interface EditarTarefaFormProps {
  tarefa: TarefaDetalhe;
  usuariosDisponiveis: { id: string; nome: string }[];
  onSalvar: (dados: { descricaoEscopo?: string; responsavelId?: string }) => Promise<void>;
  onToggleImpedimento: () => Promise<void>;
  loading: boolean;
}

/** TL-04 — seção "preenchido/idle" do drawer (descrição travada pós-início, responsável, impedimento). */
export default function EditarTarefaForm({
  tarefa,
  usuariosDisponiveis,
  onSalvar,
  onToggleImpedimento,
  loading,
}: EditarTarefaFormProps) {
  const [descricao, setDescricao] = useState(tarefa.descricaoEscopo ?? "");
  const [responsavelId, setResponsavelId] = useState(tarefa.responsavelId ?? "");
  const [salvo, setSalvo] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSalvo(false);
    await onSalvar({
      descricaoEscopo: tarefa.iniciada ? tarefa.descricaoEscopo : descricao,
      responsavelId: responsavelId || undefined,
    });
    setSalvo(true);
  };

  return (
    <section aria-label="Detalhes da tarefa" style={{ marginTop: "var(--space-md)" }}>
      <form onSubmit={handleSubmit}>
        <div className="form-field">
          <label htmlFor="descricao">
            Descrição{tarefa.iniciada ? " (campo travado pós-início)" : ""}
          </label>
          {tarefa.iniciada ? (
            <div id="descricao" className="field-locked" aria-readonly="true">
              {tarefa.descricaoEscopo || "Sem descrição."}
            </div>
          ) : (
            <textarea
              id="descricao"
              rows={3}
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              disabled={loading}
            />
          )}
        </div>

        <div className="form-field">
          <label htmlFor="responsavel">Responsável</label>
          <select
            id="responsavel"
            value={responsavelId}
            onChange={(e) => setResponsavelId(e.target.value)}
            disabled={loading}
          >
            <option value="">Sem responsável</option>
            {usuariosDisponiveis.map((u) => (
              <option key={u.id} value={u.id}>
                {u.nome}
              </option>
            ))}
          </select>
        </div>

        <div className="form-field">
          <span>Etapa atual</span>
          <p className="text-secondary">{tarefa.etapaAtualNome}</p>
        </div>

        <div className="form-field toggle">
          <label htmlFor="impedido">Marcado como impedido</label>
          <input
            id="impedido"
            type="checkbox"
            checked={tarefa.impedida}
            onChange={onToggleImpedimento}
            disabled={loading}
            aria-describedby="impedido-desc"
          />
          <span id="impedido-desc" className="text-secondary">
            Inicia contagem de lead-time de impedimento (RF-004)
          </span>
        </div>

        {salvo && (
          <div className="toast toast-success" role="status" aria-live="polite">
            Alterações salvas com sucesso.
          </div>
        )}

        <button type="submit" className="btn btn-primary" disabled={loading} aria-busy={loading}>
          {loading ? "Salvando…" : "Salvar"}
        </button>
      </form>
    </section>
  );
}
