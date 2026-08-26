"use client";

import { useState } from "react";
import { EtapaResponse } from "@/lib/admin";

/** TL-08 — aba Transições: substitui o conjunto de transições de saída da etapa (RF-010, RN-003). */
export function TransicoesModal({
  etapa,
  etapas,
  onFechar,
  onSalvo,
  onErro,
}: {
  etapa: EtapaResponse;
  etapas: EtapaResponse[];
  onFechar: () => void;
  onSalvo: () => void;
  onErro: (mensagem: string) => void;
}) {
  const [selecionadas, setSelecionadas] = useState<Set<string>>(new Set(etapa.transicoesSaida));
  const [salvando, setSalvando] = useState(false);

  function alternar(destinoId: string) {
    setSelecionadas((atual) => {
      const nova = new Set(atual);
      if (nova.has(destinoId)) {
        nova.delete(destinoId);
      } else {
        nova.add(destinoId);
      }
      return nova;
    });
  }

  async function salvar() {
    setSalvando(true);
    try {
      const res = await fetch(`/api/etapas/${etapa.id}/transicoes`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ etapasDestinoIds: [...selecionadas] }),
      });

      if (!res.ok) {
        onErro(
          res.status === 422
            ? "Etapa não-final precisa de ao menos uma transição de saída."
            : "Não foi possível salvar as transições.",
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
        aria-labelledby="transicoes-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="page-header">
          <h1 id="transicoes-titulo" style={{ fontSize: "18px" }}>
            Transições de &quot;{etapa.nome}&quot;
          </h1>
          <button className="btn btn-text" type="button" onClick={onFechar} aria-label="Fechar">
            ✕
          </button>
        </div>

        <fieldset className="form-field" style={{ border: "none", padding: 0 }}>
          <legend>Etapas de destino permitidas</legend>
          <div className="checkbox-list">
            {etapas
              .filter((e) => e.id !== etapa.id)
              .map((destino) => (
                <label key={destino.id}>
                  <input
                    type="checkbox"
                    checked={selecionadas.has(destino.id)}
                    onChange={() => alternar(destino.id)}
                  />
                  {destino.nome}
                </label>
              ))}
          </div>
        </fieldset>

        <button
          className="btn btn-primary full"
          type="button"
          onClick={salvar}
          disabled={salvando}
          aria-busy={salvando}
        >
          {salvando ? "Salvando…" : "Salvar"}
        </button>
      </div>
    </div>
  );
}
