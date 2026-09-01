"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { ProjetoResumo } from "@/lib/types";

interface ProjetoAdminFormProps {
  projeto: ProjetoResumo;
  projetoId: string;
}

/** Aba "Projeto" da Admin (TL-08, seção superior do header/ações). */
export default function ProjetoAdminForm({ projeto, projetoId }: ProjetoAdminFormProps) {
  const router = useRouter();
  const finalizado = projeto.status === "FINALIZADO";

  const [nome, setNome] = useState(projeto.nome);
  const [descricao, setDescricao] = useState(projeto.descricao ?? "");
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  const handleSalvar = async (e: React.FormEvent) => {
    e.preventDefault();
    setSalvando(true);
    setErro(null);
    setSucesso(null);
    try {
      const res = await fetch(`/api/admin/projeto/${projetoId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nome, descricao }),
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || `Erro ${res.status}`);
      }
      setSucesso("Projeto salvo com sucesso.");
      router.refresh();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao salvar");
    } finally {
      setSalvando(false);
    }
  };

  const handleFinalizarOuReabrir = async () => {
    setSalvando(true);
    setErro(null);
    setSucesso(null);
    try {
      const acao = finalizado ? "reabrir" : "finalizar";
      const res = await fetch(`/api/admin/projeto/${projetoId}/${acao}`, { method: "POST" });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || `Erro ${res.status}`);
      }
      setSucesso(finalizado ? "Projeto reaberto." : "Projeto finalizado.");
      router.refresh();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao atualizar status");
    } finally {
      setSalvando(false);
    }
  };

  return (
    <form onSubmit={handleSalvar}>
      {erro && (
        <div className="toast toast-error" role="alert" style={{ marginBottom: "var(--space-md)" }}>
          {erro}
        </div>
      )}
      {sucesso && (
        <div className="toast toast-success" role="status" aria-live="polite" style={{ marginBottom: "var(--space-md)" }}>
          {sucesso}
        </div>
      )}
      {finalizado && (
        <div className="toast" style={{ background: "#fff3cd", color: "#997404", marginBottom: "var(--space-md)" }}>
          Projeto finalizado — somente leitura.
        </div>
      )}

      <div className="form-field">
        <label htmlFor="nome">Nome</label>
        <input
          id="nome"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          disabled={salvando || finalizado}
        />
      </div>

      <div className="form-field">
        <label htmlFor="descricao">Descrição</label>
        <textarea
          id="descricao"
          rows={4}
          value={descricao}
          onChange={(e) => setDescricao(e.target.value)}
          disabled={salvando || finalizado}
        />
      </div>

      <div className="row">
        <button type="submit" className="btn btn-primary" disabled={salvando || finalizado}>
          {salvando ? "Salvando…" : "Salvar"}
        </button>
        <button type="button" className="btn btn-danger" disabled={salvando} onClick={handleFinalizarOuReabrir}>
          {finalizado ? "Reabrir projeto" : "Finalizar projeto"}
        </button>
      </div>
    </form>
  );
}
