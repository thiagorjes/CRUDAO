'use client';

import { useState } from 'react';
import styles from './ModalConfirmacao.module.css';

/** Modal de confirmação genérico (variante ação destrutiva) — RF-002, reaproveitado por outras ações futuras. */
export function ModalConfirmacao({
  titulo,
  mensagem,
  rotuloConfirmar = 'Confirmar',
  onConfirmar,
  onFechar,
}: {
  titulo: string;
  mensagem: string;
  rotuloConfirmar?: string;
  onConfirmar: () => Promise<void>;
  onFechar: () => void;
}) {
  const [confirmando, setConfirmando] = useState(false);

  async function handleConfirmar() {
    setConfirmando(true);
    try {
      await onConfirmar();
    } finally {
      setConfirmando(false);
    }
  }

  return (
    <div role="dialog" aria-modal="true" className={styles.fundo} onClick={confirmando ? undefined : onFechar}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <strong className={styles.titulo}>{titulo}</strong>
        <p className={styles.mensagem}>{mensagem}</p>

        <div className={styles.acoes}>
          <button type="button" className={styles.botaoSecundario} onClick={onFechar} disabled={confirmando}>
            Cancelar
          </button>
          <button type="button" className={styles.botaoDestrutivo} onClick={handleConfirmar} disabled={confirmando}>
            {confirmando ? 'Excluindo…' : rotuloConfirmar}
          </button>
        </div>
      </div>
    </div>
  );
}
