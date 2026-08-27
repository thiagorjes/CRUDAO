'use client';

import { useCallback, useEffect, useState } from 'react';
import { api } from '@/lib/api/client';
import { Projeto, Raia } from '@/lib/api/types';
import { mostrarToast } from '@/components/ui/toast';
import styles from '../AdminApp.module.css';

/**
 * Aba "Raias" — CRUD de raia (swimlane) do projeto (RF-011). Raias default globais
 * (`projetoId=null`) são gerenciadas só por `admin` global (nota TASK-04.2, sem projeto para escopar).
 */
export function RaiasAba({
  projeto,
  admin,
  bloqueado,
  onErro,
}: {
  projeto: Projeto;
  admin: boolean;
  bloqueado: boolean;
  onErro: (e: unknown, padrao: string) => void;
}) {
  const [globais, setGlobais] = useState(false);
  const [raias, setRaias] = useState<Raia[]>([]);
  const [nome, setNome] = useState('');
  const [ordem, setOrdem] = useState(0);

  const carregar = useCallback(async () => {
    const query = globais ? '' : `?projetoId=${projeto.id}`;
    const lista = await api.get<Raia[]>(`/raias${query}`);
    setRaias([...lista].filter((r) => (globais ? r.projetoId === null : true)).sort((a, b) => a.ordem - b.ordem));
  }, [projeto.id, globais]);

  useEffect(() => {
    (async () => {
      try {
        await carregar();
      } catch (e) {
        onErro(e, 'Não foi possível carregar as raias.');
      }
    })();
  }, [carregar, onErro]);

  async function criar() {
    if (!nome.trim()) return;
    try {
      await api.post<Raia>('/raias', { projetoId: globais ? null : projeto.id, nome, ordem });
      setNome('');
      setOrdem(0);
      await carregar();
      mostrarToast('Raia criada.');
    } catch (e) {
      onErro(e, 'Não foi possível criar a raia.');
    }
  }

  async function excluir(id: string) {
    try {
      await api.delete(`/raias/${id}`);
      await carregar();
      mostrarToast('Raia excluída.');
    } catch (e) {
      onErro(e, 'Esta raia não pode ser excluída — verifique tarefas associadas (RN-005).');
    }
  }

  return (
    <div className={styles.secao}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 className={styles.secaoTitulo}>Raias {globais ? 'globais (default)' : `de ${projeto.nome}`}</h2>
        {admin && (
          <label className={styles.checkboxLinha}>
            <input type="checkbox" checked={globais} onChange={(e) => setGlobais(e.target.checked)} />
            Gerenciar raias globais
          </label>
        )}
      </div>

      <div className={styles.lista}>
        {raias.length === 0 && <p className={styles.vazio}>Nenhuma raia cadastrada.</p>}
        {raias.map((r) => (
          <div key={r.id} className={styles.item}>
            <span className={styles.itemNome}>
              {r.ordem}. {r.nome}
            </span>
            {!bloqueado && (
              <button className={styles.botaoPerigo} onClick={() => excluir(r.id)}>
                Excluir
              </button>
            )}
          </div>
        ))}
      </div>

      {!bloqueado && (
        <div className={styles.formulario}>
          <div className={styles.campo}>
            <label htmlFor="raia-nome">Nome</label>
            <input id="raia-nome" value={nome} onChange={(e) => setNome(e.target.value)} />
          </div>
          <div className={styles.campo}>
            <label htmlFor="raia-ordem">Ordem</label>
            <input id="raia-ordem" type="number" value={ordem} onChange={(e) => setOrdem(Number(e.target.value))} />
          </div>
          <button className={styles.botao} disabled={!nome.trim()} onClick={criar}>
            Criar raia
          </button>
        </div>
      )}
    </div>
  );
}
