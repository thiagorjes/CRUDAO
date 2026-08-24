'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api/client';
import { Papel } from '@/lib/api/types';
import { mostrarToast } from '@/components/ui/toast';
import styles from '../AdminApp.module.css';

/** Chaves de permissão do catálogo (RbacSeeder) — `papel:gerenciar` nunca atribuível por projeto (G-RBAC-07). */
const PERMISSOES = [
  'projeto:gerenciar',
  'workflow:gerenciar',
  'tarefa:gerenciar',
  'tarefa:atribuir',
  'tarefa:finalizar',
  'impedimento:marcar',
  'papel:gerenciar',
  'dashboard:visualizar',
];

/** Aba "Papéis e permissões" (RF-013) — visível apenas a `admin` global (nunca a `project_admin`). */
export function PapeisAba({ onErro }: { onErro: (e: unknown, padrao: string) => void }) {
  const [papeis, setPapeis] = useState<Papel[] | null>(null);
  const [nome, setNome] = useState('');
  const [permissoesSelecionadas, setPermissoesSelecionadas] = useState<Set<string>>(new Set());
  const [edicoes, setEdicoes] = useState<Record<string, Set<string>>>({});

  async function carregar() {
    const lista = await api.get<Papel[]>('/papeis');
    setPapeis(lista);
  }

  useEffect(() => {
    (async () => {
      try {
        await carregar();
      } catch (e) {
        onErro(e, 'Não foi possível carregar os papéis.');
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function selecaoEdicao(papel: Papel): Set<string> {
    return edicoes[papel.id] ?? new Set(papel.permissoes);
  }

  function alternarEdicao(papel: Papel, chave: string) {
    const atual = new Set(selecaoEdicao(papel));
    if (atual.has(chave)) atual.delete(chave);
    else atual.add(chave);
    setEdicoes((e) => ({ ...e, [papel.id]: atual }));
  }

  async function salvarEdicao(papel: Papel) {
    try {
      await api.put(`/papeis/${papel.id}`, { nome: papel.nome, permissoes: Array.from(selecaoEdicao(papel)) });
      await carregar();
      mostrarToast('Papel atualizado.');
    } catch (e) {
      onErro(e, 'Não foi possível atualizar este papel.');
    }
  }

  async function excluir(id: string) {
    try {
      await api.delete(`/papeis/${id}`);
      await carregar();
      mostrarToast('Papel excluído.');
    } catch (e) {
      onErro(e, 'Este papel não pode ser excluído — é protegido ou está em uso.');
    }
  }

  function alternarNova(chave: string) {
    const atual = new Set(permissoesSelecionadas);
    if (atual.has(chave)) atual.delete(chave);
    else atual.add(chave);
    setPermissoesSelecionadas(atual);
  }

  async function criar() {
    if (!nome.trim()) return;
    try {
      await api.post('/papeis', { nome, permissoes: Array.from(permissoesSelecionadas) });
      setNome('');
      setPermissoesSelecionadas(new Set());
      await carregar();
      mostrarToast('Papel criado.');
    } catch (e) {
      onErro(e, 'Não foi possível criar o papel.');
    }
  }

  return (
    <>
      <div className={styles.secao}>
        <h2 className={styles.secaoTitulo}>Papéis</h2>
        <div className={styles.lista}>
          {(papeis ?? []).map((p) => (
            <div key={p.id} className={styles.item} style={{ flexWrap: 'wrap' }}>
              <span className={styles.itemNome}>
                {p.nome} {p.protegido && '· protegido'}
              </span>
              {PERMISSOES.map((chave) => (
                <label key={chave} className={styles.checkboxLinha} style={{ margin: 0 }}>
                  <input
                    type="checkbox"
                    disabled={p.protegido}
                    checked={selecaoEdicao(p).has(chave)}
                    onChange={() => alternarEdicao(p, chave)}
                  />
                  {chave}
                </label>
              ))}
              {!p.protegido && (
                <>
                  <button className={styles.botao} onClick={() => salvarEdicao(p)}>
                    Salvar
                  </button>
                  <button className={styles.botaoPerigo} onClick={() => excluir(p.id)}>
                    Excluir
                  </button>
                </>
              )}
            </div>
          ))}
        </div>
      </div>

      <div className={styles.secao}>
        <h2 className={styles.secaoTitulo}>Novo papel</h2>
        <div className={styles.formulario} style={{ flexDirection: 'column', alignItems: 'flex-start' }}>
          <div className={styles.campo}>
            <label htmlFor="papel-nome">Nome</label>
            <input id="papel-nome" value={nome} onChange={(e) => setNome(e.target.value)} />
          </div>
          <div style={{ display: 'flex', gap: 'var(--spacing-sm)', flexWrap: 'wrap' }}>
            {PERMISSOES.map((chave) => (
              <label key={chave} className={styles.checkboxLinha}>
                <input
                  type="checkbox"
                  checked={permissoesSelecionadas.has(chave)}
                  onChange={() => alternarNova(chave)}
                />
                {chave}
              </label>
            ))}
          </div>
          <button className={styles.botao} disabled={!nome.trim()} onClick={criar}>
            Criar papel
          </button>
        </div>
      </div>
    </>
  );
}
