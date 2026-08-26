"use client";

import { FormEvent, useState } from "react";
import { RaiaResponse } from "@/lib/admin";

/** TL-08 — criar/editar raia (RF-011). */
export function RaiaModal({
  projetoId,
  raia,
  onFechar,
  onSalvo,
  onErro,
}: {
  projetoId: string;
  raia: RaiaResponse | null;
  onFechar: () => void;
  onSalvo: () => void;
  onErro: (mensagem: string) => void;
}) {
  const [nome, setNome] = useState(raia?.nome ?? "");
  const [ordem, setOrdem] = useState(raia?.ordem ?? 1);
  const [erroNome, setErroNome] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function salvar(e: FormEvent) {
    e.preventDefault();
    if (!nome.trim()) {
      setErroNome("Informe o nome da raia.");
      return;
    }
    setErroNome(null);
    setSalvando(true);

    try {
      const url = raia ? `/api/raias/${raia.id}` : `/api/projetos/${projetoId}/raias`;
      const res = await fetch(url, {
        method: raia ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nome: nome.trim(), ordem }),
      });

      if (!res.ok) {
        onErro("Não foi possível salvar a raia.");
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
        aria-labelledby="raia-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="page-header">
          <h1 id="raia-titulo" style={{ fontSize: "18px" }}>
            {raia ? "Editar raia" : "Nova raia"}
          </h1>
          <button className="btn btn-text" type="button" onClick={onFechar} aria-label="Fechar">
            ✕
          </button>
        </div>

        <form aria-label="Formulário de raia" onSubmit={salvar}>
          <div className="form-field">
            <label htmlFor="raia-nome">Nome *</label>
            <input
              id="raia-nome"
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
            <label htmlFor="raia-ordem">Ordem</label>
            <input
              id="raia-ordem"
              type="number"
              min={0}
              value={ordem}
              onChange={(e) => setOrdem(Number(e.target.value))}
            />
          </div>

          <button className="btn btn-primary full" type="submit" disabled={salvando} aria-busy={salvando}>
            {salvando ? "Salvando…" : "Salvar"}
          </button>
        </form>
      </div>
    </div>
  );
}
