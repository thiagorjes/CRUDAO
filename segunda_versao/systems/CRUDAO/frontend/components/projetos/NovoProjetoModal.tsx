"use client";

import { FormEvent, useState } from "react";

/** TL-02 — Lista de Projetos: modal "+ Novo projeto" (RF-008, ADR-007 — exige adminGlobal). */
export function NovoProjetoModal({
  onFechar,
  onCriado,
  onErro,
}: {
  onFechar: () => void;
  onCriado: () => void;
  onErro: (mensagem: string) => void;
}) {
  const [nome, setNome] = useState("");
  const [descricao, setDescricao] = useState("");
  const [erroNome, setErroNome] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    if (!nome.trim()) {
      setErroNome("Informe o nome do projeto.");
      return;
    }
    setErroNome(null);
    setEnviando(true);

    try {
      const res = await fetch("/api/projetos", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nome: nome.trim(), descricao: descricao.trim() || undefined }),
      });

      if (!res.ok) {
        onErro(
          res.status === 403
            ? "Você não tem permissão para criar projetos (exige administrador global)."
            : "Não foi possível criar o projeto.",
        );
        return;
      }

      onCriado();
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={onFechar}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="novo-projeto-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="page-header">
          <h1 id="novo-projeto-titulo" style={{ fontSize: "18px" }}>
            Novo projeto
          </h1>
          <button className="btn btn-text" type="button" onClick={onFechar} aria-label="Fechar">
            ✕
          </button>
        </div>

        <form aria-label="Formulário de novo projeto" onSubmit={enviar}>
          <div className="form-field">
            <label htmlFor="projeto-nome-novo">Nome *</label>
            <input
              id="projeto-nome-novo"
              type="text"
              required
              aria-required="true"
              aria-invalid={erroNome ? "true" : undefined}
              aria-describedby={erroNome ? "projeto-nome-novo-erro" : undefined}
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              placeholder="Ex.: Plataforma de Pagamentos"
            />
            {erroNome && (
              <span id="projeto-nome-novo-erro" className="form-error" role="alert">
                {erroNome}
              </span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="projeto-descricao-novo">Descrição</label>
            <textarea
              id="projeto-descricao-novo"
              rows={3}
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
            />
          </div>

          <button
            className="btn btn-primary full"
            type="submit"
            disabled={enviando}
            aria-busy={enviando}
          >
            {enviando ? "Criando…" : "Criar projeto"}
          </button>
        </form>
      </div>
    </div>
  );
}
