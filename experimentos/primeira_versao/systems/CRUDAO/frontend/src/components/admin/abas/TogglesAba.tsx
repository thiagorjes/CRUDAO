'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api/client';
import { ConfiguracaoProjeto, Projeto } from '@/lib/api/types';
import { mostrarToast } from '@/components/ui/toast';
import styles from '../AdminApp.module.css';

const TOGGLES: { chave: keyof ConfiguracaoProjeto; label: string }[] = [
  { chave: 'devPodeExcluirTarefa', label: 'Dev pode excluir tarefa' },
  { chave: 'devPodeEditarTarefaIniciada', label: 'Dev pode editar tarefa já iniciada' },
  { chave: 'gestorVeBoard', label: 'Gestor tem acesso ao board' },
];

/** Aba "Configurações" (RF-016) — toggles de projeto. */
export function TogglesAba({
  projeto,
  bloqueado,
  onErro,
}: {
  projeto: Projeto;
  bloqueado: boolean;
  onErro: (e: unknown, padrao: string) => void;
}) {
  const [config, setConfig] = useState<ConfiguracaoProjeto | null>(null);
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    (async () => {
      setConfig(null);
      try {
        setConfig(await api.get<ConfiguracaoProjeto>(`/projetos/${projeto.id}/configuracao`));
      } catch (e) {
        onErro(e, 'Não foi possível carregar as configurações do projeto.');
      }
    })();
  }, [projeto.id, onErro]);

  async function salvar() {
    if (!config) return;
    setSalvando(true);
    try {
      const atualizado = await api.put<ConfiguracaoProjeto>(`/projetos/${projeto.id}/configuracao`, config);
      setConfig(atualizado);
      mostrarToast('Configurações salvas.');
    } catch (e) {
      onErro(e, 'Não foi possível salvar as configurações.');
    } finally {
      setSalvando(false);
    }
  }

  if (!config) return null;

  return (
    <div className={styles.secao}>
      <h2 className={styles.secaoTitulo}>Configurações de {projeto.nome}</h2>
      {TOGGLES.map((t) => (
        <label key={t.chave} className={styles.checkboxLinha}>
          <input
            type="checkbox"
            disabled={bloqueado}
            checked={config[t.chave]}
            onChange={(e) => setConfig({ ...config, [t.chave]: e.target.checked })}
          />
          {t.label}
        </label>
      ))}
      {!bloqueado && (
        <button className={styles.botao} disabled={salvando} onClick={salvar}>
          Salvar
        </button>
      )}
    </div>
  );
}
