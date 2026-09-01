"use client";

import { useState } from "react";

interface ObservadoresPanelProps {
  observadores: { id: string; nome: string }[];
  usuariosDisponiveis: { id: string; nome: string }[];
  onAdicionar: (usuarioId: string) => Promise<void>;
  onRemover: (usuarioId: string) => Promise<void>;
}

/** Observadores da tarefa (RF-005) — sem protótipo dedicado; usa o design system. */
export default function ObservadoresPanel({
  observadores,
  usuariosDisponiveis,
  onAdicionar,
  onRemover,
}: ObservadoresPanelProps) {
  const [selecionado, setSelecionado] = useState("");
  const [carregando, setCarregando] = useState(false);

  const disponiveis = usuariosDisponiveis.filter(
    (u) => !observadores.some((o) => o.id === u.id)
  );

  const adicionar = async () => {
    if (!selecionado) return;
    setCarregando(true);
    try {
      await onAdicionar(selecionado);
      setSelecionado("");
    } finally {
      setCarregando(false);
    }
  };

  return (
    <div className="card">
      <h3 style={{ fontSize: 14, marginTop: 0 }}>Observadores</h3>
      {observadores.length === 0 ? (
        <p className="text-secondary">Nenhum observador.</p>
      ) : (
        <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 4 }}>
          {observadores.map((o) => (
            <li key={o.id} className="row row--between">
              <span>{o.nome}</span>
              <button type="button" className="btn btn-text" onClick={() => onRemover(o.id)}>
                Remover
              </button>
            </li>
          ))}
        </ul>
      )}

      {disponiveis.length > 0 && (
        <div className="row" style={{ marginTop: "var(--space-sm)" }}>
          <select value={selecionado} onChange={(e) => setSelecionado(e.target.value)} disabled={carregando}>
            <option value="">Adicionar observador…</option>
            {disponiveis.map((u) => (
              <option key={u.id} value={u.id}>
                {u.nome}
              </option>
            ))}
          </select>
          <button type="button" className="btn btn-outline" onClick={adicionar} disabled={!selecionado || carregando}>
            Adicionar
          </button>
        </div>
      )}
    </div>
  );
}
