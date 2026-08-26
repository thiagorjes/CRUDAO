"use client";

import { useState } from "react";

/** TL-06 — Confirmação de Exclusão (RF-019). */
export function ConfirmExcluirModal({
  titulo,
  onCancelar,
  onConfirmar,
}: {
  titulo: string;
  onCancelar: () => void;
  onConfirmar: () => Promise<void>;
}) {
  const [excluindo, setExcluindo] = useState(false);

  async function confirmar() {
    setExcluindo(true);
    try {
      await onConfirmar();
    } finally {
      setExcluindo(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={onCancelar}>
      <div
        className="modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="excluir-titulo"
        aria-describedby="excluir-desc"
        onClick={(e) => e.stopPropagation()}
      >
        <h1 id="excluir-titulo" style={{ fontSize: "18px" }}>
          Excluir card
        </h1>
        <p id="excluir-desc">
          Tem certeza de que deseja excluir o card &quot;{titulo}&quot;? Esta ação não pode ser
          desfeita.
        </p>
        <div className="modal-actions">
          <button className="btn btn-outline" type="button" onClick={onCancelar} disabled={excluindo}>
            Cancelar
          </button>
          <button
            className="btn btn-danger"
            type="button"
            onClick={confirmar}
            disabled={excluindo}
            aria-busy={excluindo}
          >
            {excluindo ? "Excluindo…" : "Excluir"}
          </button>
        </div>
      </div>
    </div>
  );
}
