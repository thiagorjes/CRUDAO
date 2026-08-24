'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '@/lib/api/client';
import { Membro, Papel, Projeto, Usuario } from '@/lib/api/types';
import { mostrarToast } from '@/components/ui/toast';
import styles from '../AdminApp.module.css';

/**
 * Aba "Membros" (RF-015) — associação usuário↔projeto↔papel. `papel:gerenciar` nunca aparece como
 * opção atribuível aqui (G-RBAC-07) — o backend rejeita qualquer tentativa (422), esta lista já
 * filtra por segurança em profundidade.
 */
export function MembrosAba({
  projeto,
  bloqueado,
  onErro,
}: {
  projeto: Projeto;
  bloqueado: boolean;
  onErro: (e: unknown, padrao: string) => void;
}) {
  const [membros, setMembros] = useState<Membro[] | null>(null);
  const [papeis, setPapeis] = useState<Papel[]>([]);
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [novoUsuarioId, setNovoUsuarioId] = useState('');
  const [papeisNovoMembro, setPapeisNovoMembro] = useState<Set<string>>(new Set());
  const [selecaoPorMembro, setSelecaoPorMembro] = useState<Record<string, Set<string>>>({});

  const carregar = useCallback(async () => {
    const lista = await api.get<Membro[]>(`/projetos/${projeto.id}/membros`);
    setMembros(lista);
    setSelecaoPorMembro((atual) => {
      const proximo = { ...atual };
      for (const m of lista) {
        if (!proximo[m.usuarioId]) {
          proximo[m.usuarioId] = new Set();
        }
      }
      return proximo;
    });
  }, [projeto.id]);

  useEffect(() => {
    (async () => {
      try {
        await carregar();
      } catch (e) {
        onErro(e, 'Não foi possível carregar os membros do projeto.');
      }
      try {
        setPapeis(await api.get<Papel[]>('/papeis'));
      } catch {
        setPapeis([]);
      }
      try {
        setUsuarios(await api.get<Usuario[]>('/usuarios'));
      } catch {
        setUsuarios([]);
      }
    })();
  }, [carregar, onErro]);

  // Papéis atribuíveis por projeto — `papel:gerenciar` só via Usuario.admin (G-RBAC-07).
  const papeisAtribuiveis = useMemo(
    () => papeis.filter((p) => !p.permissoes.includes('papel:gerenciar')),
    [papeis],
  );
  const papeisPorNome = useMemo(() => new Map(papeis.map((p) => [p.nome, p.id])), [papeis]);
  const usuariosDisponiveis = useMemo(
    () => usuarios.filter((u) => !membros?.some((m) => m.usuarioId === u.id)),
    [usuarios, membros],
  );

  function selecaoDe(usuarioId: string, papeisAtuais: string[]): Set<string> {
    return (
      selecaoPorMembro[usuarioId] ??
      new Set(papeisAtuais.map((nome) => papeisPorNome.get(nome)).filter((id): id is string => Boolean(id)))
    );
  }

  function alternar(usuarioId: string, papeisAtuais: string[], papelId: string) {
    const atual = new Set(selecaoDe(usuarioId, papeisAtuais));
    if (atual.has(papelId)) atual.delete(papelId);
    else atual.add(papelId);
    setSelecaoPorMembro((s) => ({ ...s, [usuarioId]: atual }));
  }

  async function salvar(usuarioId: string, papeisAtuais: string[]) {
    try {
      await api.put(`/projetos/${projeto.id}/membros/${usuarioId}`, {
        papeis: Array.from(selecaoDe(usuarioId, papeisAtuais)),
      });
      await carregar();
      mostrarToast('Papéis atualizados.');
    } catch (e) {
      onErro(e, 'Não foi possível atualizar os papéis deste membro.');
    }
  }

  async function remover(usuarioId: string) {
    try {
      await api.put(`/projetos/${projeto.id}/membros/${usuarioId}`, { papeis: [] });
      await carregar();
      mostrarToast('Membro removido do projeto.');
    } catch (e) {
      onErro(e, 'Não foi possível remover este membro.');
    }
  }

  function alternarNovoMembro(papelId: string) {
    setPapeisNovoMembro((s) => {
      const proximo = new Set(s);
      if (proximo.has(papelId)) proximo.delete(papelId);
      else proximo.add(papelId);
      return proximo;
    });
  }

  async function adicionar() {
    if (!novoUsuarioId || papeisNovoMembro.size === 0) return;
    try {
      await api.put(`/projetos/${projeto.id}/membros/${novoUsuarioId}`, {
        papeis: Array.from(papeisNovoMembro),
      });
      setNovoUsuarioId('');
      setPapeisNovoMembro(new Set());
      await carregar();
      mostrarToast('Membro adicionado.');
    } catch (e) {
      onErro(e, 'Não foi possível adicionar este membro.');
    }
  }

  return (
    <div className={styles.secao}>
      <h2 className={styles.secaoTitulo}>Membros de {projeto.nome}</h2>

      <div className={styles.lista}>
        {(membros ?? []).length === 0 && <p className={styles.vazio}>Nenhum membro associado ainda.</p>}
        {(membros ?? []).map((m) => {
          const selecao = selecaoDe(m.usuarioId, m.papeis);
          return (
            <div key={m.usuarioId} className={styles.item} style={{ flexWrap: 'wrap' }}>
              <span className={styles.itemNome}>{m.nome}</span>
              {papeisAtribuiveis.map((p) => (
                <label key={p.id} className={styles.checkboxLinha} style={{ margin: 0 }}>
                  <input
                    type="checkbox"
                    disabled={bloqueado}
                    checked={selecao.has(p.id)}
                    onChange={() => alternar(m.usuarioId, m.papeis, p.id)}
                  />
                  {p.nome}
                </label>
              ))}
              {!bloqueado && (
                <>
                  <button className={styles.botao} onClick={() => salvar(m.usuarioId, m.papeis)}>
                    Salvar
                  </button>
                  <button className={styles.botaoPerigo} onClick={() => remover(m.usuarioId)}>
                    Remover
                  </button>
                </>
              )}
            </div>
          );
        })}
      </div>

      {!bloqueado && (
        <div className={styles.formulario}>
          <div className={styles.campo}>
            <label htmlFor="membro-novo">Adicionar usuário</label>
            <select id="membro-novo" value={novoUsuarioId} onChange={(e) => setNovoUsuarioId(e.target.value)}>
              <option value="">—</option>
              {usuariosDisponiveis.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.nome}
                </option>
              ))}
            </select>
          </div>
          {papeisAtribuiveis.map((p) => (
            <label key={p.id} className={styles.checkboxLinha} style={{ margin: 0 }}>
              <input
                type="checkbox"
                checked={papeisNovoMembro.has(p.id)}
                onChange={() => alternarNovoMembro(p.id)}
              />
              {p.nome}
            </label>
          ))}
          <button
            className={styles.botao}
            disabled={!novoUsuarioId || papeisNovoMembro.size === 0}
            onClick={adicionar}
          >
            Adicionar
          </button>
        </div>
      )}
    </div>
  );
}
