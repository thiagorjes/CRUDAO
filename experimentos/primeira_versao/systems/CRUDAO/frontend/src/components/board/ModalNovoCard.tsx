'use client';

import { FormEvent, useState } from 'react';
import { TipoTarefa } from '@/lib/api/types';
import styles from './ModalNovoCard.module.css';

const ROTULO_TIPO: Record<TipoTarefa, string> = {
  FEATURE: 'Feature',
  BUG: 'Bug',
  CHORE: 'Chore',
};

export type NovoCardValores = {
  titulo: string;
  descricao: string;
  tipo: TipoTarefa;
};

/** Modal "Novo card" (RF-001) — reaproveita o padrão visual dos demais modais do board. */
export function ModalNovoCard({
  onSalvar,
  onFechar,
}: {
  onSalvar: (valores: NovoCardValores) => Promise<void>;
  onFechar: () => void;
}) {
  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [tipo, setTipo] = useState<TipoTarefa>('FEATURE');
  const [erroValidacao, setErroValidacao] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!titulo.trim()) {
      setErroValidacao('Título é obrigatório.');
      return;
    }
    setErroValidacao(null);
    setSalvando(true);
    try {
      await onSalvar({ titulo: titulo.trim(), descricao: descricao.trim(), tipo });
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div role="dialog" aria-modal="true" className={styles.fundo} onClick={onFechar}>
      <form className={styles.modal} onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <strong className={styles.titulo}>Novo card</strong>

        <label className={styles.campo}>
          Título
          <input
            className={styles.input}
            value={titulo}
            onChange={(e) => setTitulo(e.target.value)}
            autoFocus
            disabled={salvando}
          />
        </label>
        {erroValidacao && <span className={styles.erro}>{erroValidacao}</span>}

        <label className={styles.campo}>
          Descrição
          <textarea
            className={styles.textarea}
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            disabled={salvando}
          />
        </label>

        <label className={styles.campo}>
          Tipo
          <select
            className={styles.input}
            value={tipo}
            onChange={(e) => setTipo(e.target.value as TipoTarefa)}
            disabled={salvando}
          >
            {Object.entries(ROTULO_TIPO).map(([valor, rotulo]) => (
              <option key={valor} value={valor}>
                {rotulo}
              </option>
            ))}
          </select>
        </label>

        <div className={styles.acoes}>
          <button type="button" className={styles.botaoSecundario} onClick={onFechar} disabled={salvando}>
            Cancelar
          </button>
          <button type="submit" className={styles.botaoPrimario} disabled={salvando}>
            {salvando ? 'Salvando…' : 'Criar card'}
          </button>
        </div>
      </form>
    </div>
  );
}
