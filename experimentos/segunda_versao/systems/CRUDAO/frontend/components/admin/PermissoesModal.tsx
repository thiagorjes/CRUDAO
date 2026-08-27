"use client";

import { useState } from "react";
import { PapelResponse } from "@/lib/papeis";
import { mensagemErroPapeis } from "@/lib/papeis-logic";

/**
 * TL-09 — toggles de permissão do papel (RF-016). Cada checkbox é um `PUT` imediato (o backend
 * modela toggle a toggle, não em lote) — reverte visualmente em caso de erro.
 */
export function PermissoesModal({
  papel,
  bloqueadoPorAutoconcessao,
  onFechar,
  onErro,
}: {
  papel: PapelResponse;
  bloqueadoPorAutoconcessao: boolean;
  onFechar: () => void;
  onErro: (mensagem: string) => void;
}) {
  const [permissoes, setPermissoes] = useState(papel.permissoes);
  const [alterando, setAlterando] = useState<string | null>(null);

  async function alternar(chave: string, habilitada: boolean) {
    setAlterando(chave);
    setPermissoes((atual) => atual.map((p) => (p.chave === chave ? { ...p, habilitada } : p)));
    try {
      const res = await fetch(`/api/papeis/${papel.id}/permissoes/${chave}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ habilitada }),
      });
      if (!res.ok) {
        setPermissoes((atual) => atual.map((p) => (p.chave === chave ? { ...p, habilitada: !habilitada } : p)));
        onErro(mensagemErroPapeis(res.status, `Não foi possível alterar "${chave}".`));
      }
    } finally {
      setAlterando(null);
    }
  }

  return (
    <div className="modal-overlay" onClick={onFechar}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="permissoes-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="page-header">
          <h1 id="permissoes-titulo" style={{ fontSize: "18px" }}>
            Permissões de &quot;{papel.nome}&quot;
          </h1>
          <button className="btn btn-text" type="button" onClick={onFechar} aria-label="Fechar">
            ✕
          </button>
        </div>

        {bloqueadoPorAutoconcessao && (
          <div className="toast toast-error" role="status" style={{ marginBottom: "16px" }}>
            Você possui este papel neste projeto — não pode alterar suas próprias permissões (RN-017). Peça a outro
            administrador.
          </div>
        )}

        <div className="checkbox-list">
          {permissoes.map((p) => (
            <label key={p.chave}>
              <input
                type="checkbox"
                checked={p.habilitada}
                disabled={papel.protegido || bloqueadoPorAutoconcessao || alterando === p.chave}
                onChange={(e) => alternar(p.chave, e.target.checked)}
              />
              {p.chave}
            </label>
          ))}
        </div>
      </div>
    </div>
  );
}
