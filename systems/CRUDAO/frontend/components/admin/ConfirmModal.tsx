"use client";

import { useState } from "react";

/** Confirmação genérica de exclusão do admin (workflow/etapa/raia — RN-005, TASK-07.4). */
export function ConfirmModal({
  titulo,
  mensagem,
  onCancelar,
  onConfirmar,
}: {
  titulo: string;
  mensagem: string;
  onCancelar: () => void;
  onConfirmar: () => Promise<void>;
}) {
  const [executando, setExecutando] = useState(false);

  async function confirmar() {
    setExecutando(true);
    try {
      await onConfirmar();
    } finally {
      setExecutando(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={onCancelar}>
      <div
        className="modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-titulo"
        aria-describedby="confirm-desc"
        onClick={(e) => e.stopPropagation()}
      >
        <h1 id="confirm-titulo" style={{ fontSize: "18px" }}>
          {titulo}
        </h1>
        <p id="confirm-desc">{mensagem}</p>
        <div className="modal-actions">
          <button className="btn btn-outline" type="button" onClick={onCancelar} disabled={executando}>
            Cancelar
          </button>
          <button
            className="btn btn-danger"
            type="button"
            onClick={confirmar}
            disabled={executando}
            aria-busy={executando}
          >
            {executando ? "Excluindo…" : "Excluir"}
          </button>
        </div>
      </div>
    </div>
  );
}
