"use client";

import { FormEvent, useState } from "react";
import { MembroProjeto, RaiaResponse } from "@/lib/board";

/** TL-05 — Nova Tarefa. Etapa inicial é sempre a de menor ordem (RN-CB-004/005, decidido no backend). */
export function NovoCardModal({
  projetoId,
  raias,
  membros,
  onFechar,
  onCriado,
  onErro,
}: {
  projetoId: string;
  raias: RaiaResponse[];
  membros: MembroProjeto[];
  onFechar: () => void;
  onCriado: () => void;
  onErro: (mensagem: string) => void;
}) {
  const [titulo, setTitulo] = useState("");
  const [descricaoEscopo, setDescricaoEscopo] = useState("");
  const [raiaId, setRaiaId] = useState("");
  const [responsavelId, setResponsavelId] = useState("");
  const [erroTitulo, setErroTitulo] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    if (!titulo.trim()) {
      setErroTitulo("Informe o título da tarefa.");
      return;
    }
    setErroTitulo(null);
    setEnviando(true);

    try {
      const res = await fetch(`/api/projetos/${projetoId}/tarefas`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          titulo: titulo.trim(),
          descricaoEscopo: descricaoEscopo.trim() || undefined,
          raiaId: raiaId || undefined,
          responsavelId: responsavelId || undefined,
        }),
      });

      if (!res.ok) {
        onErro(
          res.status === 403
            ? "Você não tem permissão para criar cards neste projeto."
            : "Não foi possível criar o card.",
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
        aria-labelledby="novo-card-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="page-header">
          <h1 id="novo-card-titulo" style={{ fontSize: "18px" }}>
            Novo card
          </h1>
          <button className="btn btn-text" type="button" onClick={onFechar} aria-label="Fechar">
            ✕
          </button>
        </div>

        <form aria-label="Formulário de nova tarefa" onSubmit={enviar}>
          <div className="form-field">
            <label htmlFor="titulo">Título *</label>
            <input
              id="titulo"
              type="text"
              required
              aria-required="true"
              aria-invalid={erroTitulo ? "true" : undefined}
              aria-describedby={erroTitulo ? "titulo-erro-msg" : undefined}
              value={titulo}
              onChange={(e) => setTitulo(e.target.value)}
              placeholder="Ex.: Corrigir timeout no gateway"
            />
            {erroTitulo && (
              <span id="titulo-erro-msg" className="form-error" role="alert">
                {erroTitulo}
              </span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="descricao">Descrição</label>
            <textarea
              id="descricao"
              rows={3}
              value={descricaoEscopo}
              onChange={(e) => setDescricaoEscopo(e.target.value)}
            />
          </div>

          <div className="form-field">
            <label htmlFor="raia">Raia (opcional)</label>
            <select id="raia" value={raiaId} onChange={(e) => setRaiaId(e.target.value)}>
              <option value="">Raia padrão do projeto</option>
              {raias.map((raia) => (
                <option key={raia.id} value={raia.id}>
                  {raia.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="responsavel">Responsável (opcional)</label>
            <select
              id="responsavel"
              value={responsavelId}
              onChange={(e) => setResponsavelId(e.target.value)}
            >
              <option value="">Sem responsável</option>
              {membros.map((membro) => (
                <option key={membro.usuarioId} value={membro.usuarioId}>
                  {membro.nome}
                </option>
              ))}
            </select>
          </div>

          <button
            className="btn btn-primary full"
            type="submit"
            disabled={enviando}
            aria-busy={enviando}
          >
            {enviando ? "Criando…" : "Criar card"}
          </button>
        </form>
      </div>
    </div>
  );
}
