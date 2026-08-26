"use client";

import { FormEvent, useState } from "react";
import { PapelResponse } from "@/lib/papeis";

/** TL-09 — criar papel custom / editar nome (RF-013). `chave` é imutável após criada. */
export function PapelModal({
  projetoId,
  papel,
  onFechar,
  onSalvo,
  onErro,
}: {
  projetoId: string;
  papel: PapelResponse | null;
  onFechar: () => void;
  onSalvo: () => void;
  onErro: (mensagem: string) => void;
}) {
  const [chave, setChave] = useState(papel?.chave ?? "");
  const [nome, setNome] = useState(papel?.nome ?? "");
  const [erroCampo, setErroCampo] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function salvar(e: FormEvent) {
    e.preventDefault();
    if (!nome.trim() || (!papel && !chave.trim())) {
      setErroCampo(!papel ? "Informe chave e nome do papel." : "Informe o nome do papel.");
      return;
    }
    setErroCampo(null);
    setSalvando(true);

    try {
      const url = papel ? `/api/papeis/${papel.id}` : `/api/projetos/${projetoId}/papeis`;
      const body = papel ? { nome: nome.trim() } : { chave: chave.trim(), nome: nome.trim() };
      const res = await fetch(url, {
        method: papel ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (!res.ok) {
        onErro(
          res.status === 422
            ? "Chave 'admin' é reservada."
            : res.status === 409
              ? "Já existe um papel com essa chave neste projeto."
              : "Não foi possível salvar o papel.",
        );
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
        aria-labelledby="papel-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="page-header">
          <h1 id="papel-titulo" style={{ fontSize: "18px" }}>
            {papel ? "Editar papel" : "Novo papel"}
          </h1>
          <button className="btn btn-text" type="button" onClick={onFechar} aria-label="Fechar">
            ✕
          </button>
        </div>

        <form aria-label="Formulário de papel" onSubmit={salvar}>
          {!papel && (
            <div className="form-field">
              <label htmlFor="papel-chave">Chave *</label>
              <input
                id="papel-chave"
                type="text"
                required
                aria-required="true"
                value={chave}
                onChange={(e) => setChave(e.target.value)}
              />
            </div>
          )}

          <div className="form-field">
            <label htmlFor="papel-nome">Nome *</label>
            <input
              id="papel-nome"
              type="text"
              required
              aria-required="true"
              aria-invalid={erroCampo ? "true" : undefined}
              value={nome}
              onChange={(e) => setNome(e.target.value)}
            />
            {erroCampo && (
              <span className="form-error" role="alert">
                {erroCampo}
              </span>
            )}
          </div>

          <button className="btn btn-primary full" type="submit" disabled={salvando} aria-busy={salvando}>
            {salvando ? "Salvando…" : "Salvar"}
          </button>
        </form>
      </div>
    </div>
  );
}
