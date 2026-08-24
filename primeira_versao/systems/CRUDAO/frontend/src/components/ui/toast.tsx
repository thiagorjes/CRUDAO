'use client';

import { useEffect, useState } from 'react';

type TipoToast = 'sucesso' | 'info';
type Toast = { id: number; mensagem: string; tipo: TipoToast };

type Ouvinte = (toasts: Toast[]) => void;

let proximoId = 1;
let toasts: Toast[] = [];
const ouvintes = new Set<Ouvinte>();

function notificar() {
  ouvintes.forEach((ouvinte) => ouvinte(toasts));
}

/** Dispara um toast/snackbar (feedback de sucesso ou informativo, auto-dismiss — design brief §6). */
export function mostrarToast(mensagem: string, tipo: TipoToast = 'sucesso') {
  const id = proximoId++;
  toasts = [...toasts, { id, mensagem, tipo }];
  notificar();
  setTimeout(() => {
    toasts = toasts.filter((t) => t.id !== id);
    notificar();
  }, 3500);
}

/** Monte uma vez, no layout raiz — renderiza todos os toasts ativos. */
export function ToastHost() {
  const [lista, setLista] = useState<Toast[]>([]);

  useEffect(() => {
    ouvintes.add(setLista);
    return () => {
      ouvintes.delete(setLista);
    };
  }, []);

  if (lista.length === 0) {
    return null;
  }

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 'var(--spacing-lg)',
        right: 'var(--spacing-lg)',
        display: 'flex',
        flexDirection: 'column',
        gap: 'var(--spacing-sm)',
        zIndex: 1000,
      }}
    >
      {lista.map((toast) => (
        <div
          key={toast.id}
          style={{
            padding: '10px 16px',
            borderRadius: 6,
            color: '#fff',
            font: 'var(--font-body)',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            background: toast.tipo === 'sucesso' ? 'var(--color-success)' : 'var(--color-text-secondary)',
          }}
        >
          {toast.mensagem}
        </div>
      ))}
    </div>
  );
}
