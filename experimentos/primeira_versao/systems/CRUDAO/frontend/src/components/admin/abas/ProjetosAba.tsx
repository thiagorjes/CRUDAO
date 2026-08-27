'use client';

import { useState } from 'react';
import { api } from '@/lib/api/client';
import { Projeto } from '@/lib/api/types';
import { mostrarToast } from '@/components/ui/toast';
import styles from '../AdminApp.module.css';

/**
 * Aba "Projeto" — editar nome/descrição, finalizar/reabrir (RN-015) e, se admin, criar novo
 * projeto. `projeto` é opcional: sem nenhum projeto cadastrado ainda, não há o que editar, mas o
 * admin precisa continuar vendo "Novo projeto" para poder criar o primeiro (bootstrap).
 */
export function ProjetosAba({
  projeto,
  admin,
  podeGerenciar,
  bloqueado,
  onProjetoAtualizado,
  onProjetoCriado,
  onErro,
}: {
  projeto: Projeto | null;
  admin: boolean;
  podeGerenciar: boolean;
  bloqueado: boolean;
  onProjetoAtualizado: (p: Projeto) => void;
  onProjetoCriado: (p: Projeto) => void;
  onErro: (e: unknown, padrao: string) => void;
}) {
  const [nome, setNome] = useState(projeto?.nome ?? '');
  const [descricao, setDescricao] = useState(projeto?.descricao ?? '');
  const [salvando, setSalvando] = useState(false);
  const [novoNome, setNovoNome] = useState('');
  const [criando, setCriando] = useState(false);

  async function salvar() {
    if (!projeto) return;
    setSalvando(true);
    try {
      const atualizado = await api.put<Projeto>(`/projetos/${projeto.id}`, { nome, descricao: descricao || null });
      onProjetoAtualizado(atualizado);
      mostrarToast('Projeto atualizado.');
    } catch (e) {
      onErro(e, 'Não foi possível atualizar o projeto.');
    } finally {
      setSalvando(false);
    }
  }

  async function finalizar() {
    if (!projeto) return;
    try {
      await api.put(`/projetos/${projeto.id}/finalizar`);
      onProjetoAtualizado({ ...projeto, dataFinalizacao: new Date().toISOString() });
      mostrarToast('Projeto finalizado.');
    } catch (e) {
      onErro(e, 'Não foi possível finalizar o projeto.');
    }
  }

  async function reabrir() {
    if (!projeto) return;
    try {
      await api.delete(`/projetos/${projeto.id}/finalizar`);
      onProjetoAtualizado({ ...projeto, dataFinalizacao: null });
      mostrarToast('Projeto reaberto.');
    } catch (e) {
      onErro(e, 'Não foi possível reabrir o projeto.');
    }
  }

  async function criar() {
    if (!novoNome.trim()) return;
    setCriando(true);
    try {
      const criado = await api.post<Projeto>('/projetos', { nome: novoNome, descricao: null });
      setNovoNome('');
      onProjetoCriado(criado);
      mostrarToast('Projeto criado.');
    } catch (e) {
      onErro(e, 'Não foi possível criar o projeto.');
    } finally {
      setCriando(false);
    }
  }

  return (
    <>
      {projeto && (
        <div className={styles.secao}>
          <h2 className={styles.secaoTitulo}>Dados do projeto</h2>
          <div className={styles.formulario}>
            <div className={styles.campo}>
              <label htmlFor="proj-nome">Nome</label>
              <input
                id="proj-nome"
                value={nome}
                disabled={!podeGerenciar || bloqueado}
                onChange={(e) => setNome(e.target.value)}
              />
            </div>
            <div className={styles.campo}>
              <label htmlFor="proj-descricao">Descrição</label>
              <input
                id="proj-descricao"
                value={descricao}
                disabled={!podeGerenciar || bloqueado}
                onChange={(e) => setDescricao(e.target.value)}
              />
            </div>
            {podeGerenciar && !bloqueado && (
              <button className={styles.botao} disabled={salvando || !nome.trim()} onClick={salvar}>
                Salvar
              </button>
            )}
          </div>

          {podeGerenciar && (
            <div style={{ marginTop: 'var(--spacing-md)' }}>
              {bloqueado ? (
                <button className={styles.botao} onClick={reabrir}>
                  Reabrir projeto
                </button>
              ) : (
                <button className={styles.botaoPerigo} onClick={finalizar}>
                  Finalizar projeto
                </button>
              )}
            </div>
          )}
        </div>
      )}

      {!projeto && admin && (
        <p className={styles.vazio}>Nenhum projeto cadastrado ainda — crie o primeiro abaixo.</p>
      )}

      {admin && (
        <div className={styles.secao}>
          <h2 className={styles.secaoTitulo}>Novo projeto</h2>
          <div className={styles.formulario}>
            <div className={styles.campo}>
              <label htmlFor="proj-novo-nome">Nome</label>
              <input id="proj-novo-nome" value={novoNome} onChange={(e) => setNovoNome(e.target.value)} />
            </div>
            <button className={styles.botao} disabled={criando || !novoNome.trim()} onClick={criar}>
              Criar projeto
            </button>
          </div>
        </div>
      )}
    </>
  );
}
