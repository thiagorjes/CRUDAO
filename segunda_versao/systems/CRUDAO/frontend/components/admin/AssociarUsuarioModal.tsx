"use client";

import { useEffect, useRef, useState } from "react";
import { PapelResponse, UsuarioResumoResponse } from "@/lib/papeis";

/**
 * TL-10 — associar usuário↔projeto↔papel (RF-015). Autocomplete via
 * `GET /api/projetos/{id}/usuarios/buscar?q=` (debounce 300ms, mínimo 3 caracteres — mesmo limite
 * do backend, TASK-07.5).
 */
export function AssociarUsuarioModal({
  projetoId,
  papeisAssociaveis,
  onFechar,
  onSalvo,
  onErro,
}: {
  projetoId: string;
  papeisAssociaveis: PapelResponse[];
  onFechar: () => void;
  onSalvo: () => void;
  onErro: (mensagem: string) => void;
}) {
  const [termo, setTermo] = useState("");
  const [resultados, setResultados] = useState<UsuarioResumoResponse[]>([]);
  const [buscando, setBuscando] = useState(false);
  const [selecionado, setSelecionado] = useState<UsuarioResumoResponse | null>(null);
  const [papelId, setPapelId] = useState(papeisAssociaveis[0]?.id ?? "");
  const [erroCampo, setErroCampo] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (selecionado || termo.trim().length < 3) {
      setResultados([]);
      return;
    }
    debounceRef.current = setTimeout(async () => {
      setBuscando(true);
      try {
        const res = await fetch(`/api/projetos/${projetoId}/usuarios/buscar?q=${encodeURIComponent(termo.trim())}`);
        setResultados(res.ok ? await res.json() : []);
      } finally {
        setBuscando(false);
      }
    }, 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [termo, selecionado, projetoId]);

  async function associar() {
    if (!selecionado || !papelId) {
      setErroCampo("Busque e selecione um usuário e um papel.");
      return;
    }
    setErroCampo(null);
    setSalvando(true);
    try {
      const res = await fetch(`/api/projetos/${projetoId}/usuarios`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ usuarioId: selecionado.id, papelId }),
      });
      if (!res.ok) {
        onErro(
          res.status === 422 ? "Papel protegido não pode ser associado." : "Não foi possível associar o usuário.",
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
        aria-labelledby="associar-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="page-header">
          <h1 id="associar-titulo" style={{ fontSize: "18px" }}>
            Associar usuário
          </h1>
          <button className="btn btn-text" type="button" onClick={onFechar} aria-label="Fechar">
            ✕
          </button>
        </div>

        <div className="form-field">
          <label htmlFor="associar-busca">Buscar usuário (nome ou e-mail) *</label>
          <input
            id="associar-busca"
            type="text"
            aria-required="true"
            value={selecionado ? `${selecionado.nome} <${selecionado.email}>` : termo}
            onChange={(e) => {
              setSelecionado(null);
              setTermo(e.target.value);
            }}
            placeholder="Ao menos 3 caracteres"
          />
          {buscando && <span className="text-secondary">Buscando…</span>}
          {!selecionado && resultados.length > 0 && (
            <ul className="checkbox-list" aria-label="Resultados da busca">
              {resultados.map((u) => (
                <li key={u.id}>
                  <button
                    className="btn btn-text"
                    type="button"
                    onClick={() => {
                      setSelecionado(u);
                      setResultados([]);
                    }}
                  >
                    {u.nome} &lt;{u.email}&gt;
                  </button>
                </li>
              ))}
            </ul>
          )}
          {!selecionado && !buscando && termo.trim().length >= 3 && resultados.length === 0 && (
            <span className="text-secondary">Nenhum usuário encontrado (já associado ou inexistente).</span>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="associar-papel">Papel *</label>
          <select id="associar-papel" value={papelId} onChange={(e) => setPapelId(e.target.value)}>
            {papeisAssociaveis.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nome}
              </option>
            ))}
          </select>
        </div>

        {erroCampo && (
          <span className="form-error" role="alert">
            {erroCampo}
          </span>
        )}

        <button
          className="btn btn-primary full"
          type="button"
          onClick={associar}
          disabled={salvando}
          aria-busy={salvando}
        >
          {salvando ? "Associando…" : "Associar"}
        </button>
      </div>
    </div>
  );
}
