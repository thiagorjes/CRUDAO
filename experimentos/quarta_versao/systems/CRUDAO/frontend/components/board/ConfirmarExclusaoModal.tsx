"use client";

import { useState } from "react";

interface ConfirmarExclusaoModalProps {
  tituloTarefa: string;
  onConfirmar: () => Promise<void>;
  onCancelar: () => void;
}

/** TL-06 — Confirmação de exclusão de card. */
export default function ConfirmarExclusaoModal({
  tituloTarefa,
  onConfirmar,
  onCancelar,
}: ConfirmarExclusaoModalProps) {
  const [excluindo, setExcluindo] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const handleExcluir = async () => {
    setErro(null);
    setExcluindo(true);
    try {
      await onConfirmar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao excluir card.");
      setExcluindo(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div
        className="modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="modal-excluir-title"
        aria-describedby="modal-excluir-desc"
      >
        <h1 id="modal-excluir-title" style={{ fontSize: 18 }}>
          Excluir card
        </h1>

        <p id="modal-excluir-desc">
          Tem certeza de que deseja excluir o card <strong>&quot;{tituloTarefa}&quot;</strong>?
          Esta ação não pode ser desfeita.
        </p>

        {erro && (
          <div className="toast toast-error" role="alert" style={{ marginTop: "var(--space-sm)" }}>
            {erro}
          </div>
        )}

        <div className="modal-actions">
          <button type="button" className="btn btn-outline" onClick={onCancelar} disabled={excluindo}>
            Cancelar
          </button>
          <button
            type="button"
            className="btn btn-danger"
            onClick={handleExcluir}
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
