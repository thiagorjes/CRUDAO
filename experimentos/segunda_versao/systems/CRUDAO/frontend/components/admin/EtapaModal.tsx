"use client";

import { FormEvent, useState } from "react";
import { EtapaResponse } from "@/lib/admin";

/** TL-08 — criar/editar coluna do workflow (RF-009, RN-003). */
export function EtapaModal({
  workflowId,
  etapa,
  onFechar,
  onSalvo,
  onErro,
}: {
  workflowId: string;
  etapa: EtapaResponse | null;
  onFechar: () => void;
  onSalvo: () => void;
  onErro: (mensagem: string) => void;
}) {
  const [nome, setNome] = useState(etapa?.nome ?? "");
  const [ordem, setOrdem] = useState(etapa?.ordem ?? 1);
  const [etapaFinal, setEtapaFinal] = useState(etapa?.etapaFinal ?? false);
  const [erroNome, setErroNome] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function salvar(e: FormEvent) {
    e.preventDefault();
    if (!nome.trim()) {
      setErroNome("Informe o nome da coluna.");
      return;
    }
    setErroNome(null);
    setSalvando(true);

    try {
      const url = etapa ? `/api/etapas/${etapa.id}` : `/api/workflows/${workflowId}/etapas`;
      const res = await fetch(url, {
        method: etapa ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nome: nome.trim(), ordem, etapaFinal }),
      });

      if (!res.ok) {
        onErro(res.status === 422 ? "Dados inválidos para a coluna." : "Não foi possível salvar a coluna.");
        return;
      }

      onSalvo();
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={onFechar}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="etapa-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="page-header">
          <h1 id="etapa-titulo" style={{ fontSize: "18px" }}>
            {etapa ? "Editar coluna" : "Nova coluna"}
          </h1>
          <button className="btn btn-text" type="button" onClick={onFechar} aria-label="Fechar">
            ✕
          </button>
        </div>

        <form aria-label="Formulário de coluna" onSubmit={salvar}>
          <div className="form-field">
            <label htmlFor="etapa-nome">Nome *</label>
            <input
              id="etapa-nome"
              type="text"
              required
              aria-required="true"
              aria-invalid={erroNome ? "true" : undefined}
              value={nome}
              onChange={(e) => setNome(e.target.value)}
            />
            {erroNome && (
              <span className="form-error" role="alert">
                {erroNome}
              </span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="etapa-ordem">Ordem</label>
            <input
              id="etapa-ordem"
              type="number"
              min={0}
              value={ordem}
              onChange={(e) => setOrdem(Number(e.target.value))}
            />
          </div>

          <div className="form-field">
            <label htmlFor="etapa-final" style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <input
                id="etapa-final"
                type="checkbox"
                checked={etapaFinal}
                onChange={(e) => setEtapaFinal(e.target.checked)}
              />
              Etapa final (não exige transição de saída)
            </label>
          </div>

          <button className="btn btn-primary full" type="submit" disabled={salvando} aria-busy={salvando}>
            {salvando ? "Salvando…" : "Salvar"}
          </button>
        </form>
      </div>
    </div>
  );
}
