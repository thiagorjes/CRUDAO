"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

/**
 * TL-02 / RF-008 — criação de projeto (visível só para adminGlobal; o backend revalida).
 * Form inline: cria via POST /api/projetos e navega para o Admin do projeto novo.
 */
export default function NovoProjetoButton() {
  const router = useRouter();
  const [aberto, setAberto] = useState(false);
  const [nome, setNome] = useState("");
  const [descricao, setDescricao] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function submeter(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    setSalvando(true);
    try {
      const res = await fetch("/api/projetos", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nome: nome.trim(), descricao: descricao.trim() || null }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setErro(
          data.message ||
            data.error ||
            (res.status === 403
              ? "Apenas o administrador global pode criar projetos."
              : `Falha ao criar projeto (${res.status}).`)
        );
        return;
      }
      setAberto(false);
      setNome("");
      setDescricao("");
      router.push(`/projetos/${data.id}/admin`);
      router.refresh();
    } catch {
      setErro("Erro de rede ao criar projeto.");
    } finally {
      setSalvando(false);
    }
  }

  if (!aberto) {
    return (
      <button type="button" className="btn btn-primary" onClick={() => setAberto(true)}>
        Novo projeto
      </button>
    );
  }

  return (
    <form
      className="card"
      onSubmit={submeter}
      style={{ display: "grid", gap: "var(--space-sm)", maxWidth: 440 }}
    >
      <label style={{ display: "grid", gap: 4 }}>
        Nome
        <input
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          required
          maxLength={120}
          autoFocus
        />
      </label>
      <label style={{ display: "grid", gap: 4 }}>
        Descrição (opcional)
        <textarea
          value={descricao}
          onChange={(e) => setDescricao(e.target.value)}
          rows={3}
          maxLength={500}
        />
      </label>
      {erro && (
        <p role="alert" style={{ color: "var(--color-danger, #c0392b)", margin: 0 }}>
          {erro}
        </p>
      )}
      <div style={{ display: "flex", gap: "var(--space-sm)" }}>
        <button type="submit" className="btn btn-primary" disabled={salvando || !nome.trim()}>
          {salvando ? "Criando…" : "Criar"}
        </button>
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => {
            setAberto(false);
            setErro(null);
          }}
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}
