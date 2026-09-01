"use client";

import { useState } from "react";
import type { BoardRaia } from "@/lib/types";

interface CreateCardModalProps {
  raias: BoardRaia[];
  usuarios: { id: string; nome: string }[];
  onCriar: (dados: { titulo: string; descricao?: string; raiaId?: string; responsavelId?: string }) => Promise<void>;
  onFechar: () => void;
}

/** TL-05 — Nova Tarefa (modal). */
export default function CreateCardModal({ raias, usuarios, onCriar, onFechar }: CreateCardModalProps) {
  const [titulo, setTitulo] = useState("");
  const [descricao, setDescricao] = useState("");
  const [raiaId, setRaiaId] = useState("");
  const [responsavelId, setResponsavelId] = useState("");
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!titulo.trim()) {
      setErro("Informe o título da tarefa.");
      return;
    }
    setErro(null);
    setLoading(true);
    try {
      await onCriar({
        titulo: titulo.trim(),
        descricao: descricao.trim() || undefined,
        raiaId: raiaId || undefined,
        responsavelId: responsavelId || undefined,
      });
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao criar card.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-nova-tarefa-title">
        <div className="page-header">
          <h1 id="modal-nova-tarefa-title" style={{ fontSize: 18 }}>
            Novo card
          </h1>
          <button type="button" className="btn btn-text" aria-label="Fechar" onClick={onFechar}>
            ✕
          </button>
        </div>

        <form aria-label="Formulário de nova tarefa" onSubmit={handleSubmit}>
          <div className="form-field">
            <label htmlFor="titulo">Título *</label>
            <input
              id="titulo"
              name="titulo"
              type="text"
              required
              aria-required="true"
              aria-invalid={!!erro}
              placeholder="Ex.: Corrigir timeout no gateway"
              value={titulo}
              onChange={(e) => setTitulo(e.target.value)}
              disabled={loading}
              autoFocus
            />
            {erro && (
              <span className="form-error" role="alert">
                {erro}
              </span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="descricao">Descrição</label>
            <textarea
              id="descricao"
              name="descricao"
              rows={3}
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              disabled={loading}
            />
          </div>

          <div className="form-field">
            <label htmlFor="raia">Raia (opcional)</label>
            <select id="raia" name="raia" value={raiaId} onChange={(e) => setRaiaId(e.target.value)} disabled={loading}>
              <option value="">Raia padrão do projeto</option>
              {raias.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="responsavel">Responsável (opcional)</label>
            <select
              id="responsavel"
              name="responsavel"
              value={responsavelId}
              onChange={(e) => setResponsavelId(e.target.value)}
              disabled={loading}
            >
              <option value="">Sem responsável</option>
              {usuarios.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.nome}
                </option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: "100%", justifyContent: "center" }}
            disabled={loading}
            aria-busy={loading}
          >
            {loading ? "Criando…" : "Criar card"}
          </button>
        </form>
      </div>
    </div>
  );
}
