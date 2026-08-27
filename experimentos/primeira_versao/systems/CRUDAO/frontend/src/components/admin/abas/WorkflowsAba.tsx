'use client';

import { useCallback, useEffect, useState } from 'react';
import { api } from '@/lib/api/client';
import { Etapa, Projeto, TipoTransicao, Transicao, Workflow } from '@/lib/api/types';
import { mostrarToast } from '@/components/ui/toast';
import styles from '../AdminApp.module.css';

/** Aba "Workflows e etapas" — CRUD de Workflow (RF-009), Etapa (RF-010) e Transição (RF-002/RF-012). */
export function WorkflowsAba({
  projeto,
  bloqueado,
  onProjetoAtualizado,
  onErro,
}: {
  projeto: Projeto;
  bloqueado: boolean;
  onProjetoAtualizado: (p: Projeto) => void;
  onErro: (e: unknown, padrao: string) => void;
}) {
  const [workflows, setWorkflows] = useState<Workflow[] | null>(null);
  const [workflowId, setWorkflowId] = useState<string | null>(null);
  const [etapas, setEtapas] = useState<Etapa[]>([]);
  const [transicoes, setTransicoes] = useState<Transicao[]>([]);
  const [novoWorkflowNome, setNovoWorkflowNome] = useState('');
  const [novaEtapaNome, setNovaEtapaNome] = useState('');
  const [novaEtapaOrdem, setNovaEtapaOrdem] = useState(0);
  const [novaEtapaFinal, setNovaEtapaFinal] = useState(false);
  const [origemId, setOrigemId] = useState('');
  const [destinoId, setDestinoId] = useState('');
  const [tipoTransicao, setTipoTransicao] = useState<TipoTransicao>('NORMAL');

  const carregarWorkflows = useCallback(async () => {
    const lista = await api.get<Workflow[]>(`/workflows?projetoId=${projeto.id}`);
    setWorkflows(lista);
    setWorkflowId((atual) => (atual && lista.some((w) => w.id === atual) ? atual : (lista[0]?.id ?? null)));
  }, [projeto.id]);

  useEffect(() => {
    (async () => {
      try {
        await carregarWorkflows();
      } catch (e) {
        onErro(e, 'Não foi possível carregar os workflows.');
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projeto.id]);

  const carregarDetalhes = useCallback(async () => {
    if (!workflowId) {
      setEtapas([]);
      setTransicoes([]);
      return;
    }
    const [listaEtapas, listaTransicoes] = await Promise.all([
      api.get<Etapa[]>(`/etapas?workflowId=${workflowId}`),
      api.get<Transicao[]>(`/transicoes?workflowId=${workflowId}`),
    ]);
    setEtapas([...listaEtapas].sort((a, b) => a.ordem - b.ordem));
    setTransicoes(listaTransicoes);
  }, [workflowId]);

  useEffect(() => {
    (async () => {
      try {
        await carregarDetalhes();
      } catch (e) {
        onErro(e, 'Não foi possível carregar as etapas do workflow.');
      }
    })();
  }, [carregarDetalhes, onErro]);

  async function criarWorkflow() {
    if (!novoWorkflowNome.trim()) return;
    try {
      const criado = await api.post<Workflow>('/workflows', { projetoId: projeto.id, nome: novoWorkflowNome });
      setNovoWorkflowNome('');
      await carregarWorkflows();
      setWorkflowId(criado.id);
      mostrarToast('Workflow criado.');
    } catch (e) {
      onErro(e, 'Não foi possível criar o workflow.');
    }
  }

  async function excluirWorkflow(id: string) {
    try {
      await api.delete(`/workflows/${id}`);
      await carregarWorkflows();
      mostrarToast('Workflow excluído.');
    } catch (e) {
      onErro(e, 'Este workflow não pode ser excluído — verifique se ainda está em uso (RN-005).');
    }
  }

  async function definirAtivo(id: string) {
    try {
      await api.put(`/projetos/${projeto.id}/workflow-ativo`, { workflowId: id });
      onProjetoAtualizado({ ...projeto, workflowAtivoId: id });
      mostrarToast('Workflow ativo definido.');
    } catch (e) {
      onErro(e, 'Não foi possível definir o workflow ativo.');
    }
  }

  async function criarEtapa() {
    if (!workflowId || !novaEtapaNome.trim()) return;
    try {
      await api.post<Etapa>('/etapas', {
        workflowId,
        nome: novaEtapaNome,
        ordem: novaEtapaOrdem,
        etapaFinal: novaEtapaFinal,
      });
      setNovaEtapaNome('');
      setNovaEtapaOrdem(0);
      setNovaEtapaFinal(false);
      await carregarDetalhes();
      mostrarToast('Etapa criada.');
    } catch (e) {
      onErro(e, 'Não foi possível criar a etapa.');
    }
  }

  async function excluirEtapa(id: string) {
    try {
      await api.delete(`/etapas/${id}`);
      await carregarDetalhes();
      mostrarToast('Etapa excluída.');
    } catch (e) {
      onErro(e, 'Esta etapa não pode ser excluída — verifique tarefas ou transições associadas (RN-005).');
    }
  }

  async function criarTransicao() {
    if (!origemId || !destinoId) return;
    try {
      await api.post<Transicao>('/transicoes', {
        etapaOrigemId: origemId,
        etapaDestinoId: destinoId,
        tipo: tipoTransicao,
      });
      await carregarDetalhes();
      mostrarToast('Transição criada.');
    } catch (e) {
      onErro(e, 'Não foi possível criar a transição.');
    }
  }

  async function excluirTransicao(id: string) {
    try {
      await api.delete(`/transicoes/${id}`);
      await carregarDetalhes();
      mostrarToast('Transição excluída.');
    } catch (e) {
      onErro(e, 'Não foi possível excluir a transição.');
    }
  }

  const nomeEtapa = (id: string) => etapas.find((e) => e.id === id)?.nome ?? id;

  return (
    <>
      <div className={styles.secao}>
        <h2 className={styles.secaoTitulo}>Workflows</h2>
        <div className={styles.lista}>
          {(workflows ?? []).length === 0 && <p className={styles.vazio}>Nenhum workflow cadastrado.</p>}
          {(workflows ?? []).map((w) => (
            <div key={w.id} className={styles.item}>
              <span className={styles.itemNome}>
                {w.nome} {projeto.workflowAtivoId === w.id && '· ativo'}
              </span>
              <button className={styles.botao} onClick={() => setWorkflowId(w.id)}>
                Ver etapas
              </button>
              {projeto.workflowAtivoId !== w.id && !bloqueado && (
                <button className={styles.botaoSecundario} onClick={() => definirAtivo(w.id)}>
                  Definir como ativo
                </button>
              )}
              {!bloqueado && (
                <button className={styles.botaoPerigo} onClick={() => excluirWorkflow(w.id)}>
                  Excluir
                </button>
              )}
            </div>
          ))}
        </div>
        {!bloqueado && (
          <div className={styles.formulario}>
            <div className={styles.campo}>
              <label htmlFor="wf-nome">Novo workflow</label>
              <input id="wf-nome" value={novoWorkflowNome} onChange={(e) => setNovoWorkflowNome(e.target.value)} />
            </div>
            <button className={styles.botao} disabled={!novoWorkflowNome.trim()} onClick={criarWorkflow}>
              Criar
            </button>
          </div>
        )}
      </div>

      {workflowId && (
        <>
          <div className={styles.secao}>
            <h2 className={styles.secaoTitulo}>Etapas</h2>
            <div className={styles.lista}>
              {etapas.length === 0 && <p className={styles.vazio}>Nenhuma etapa cadastrada.</p>}
              {etapas.map((e) => (
                <div key={e.id} className={styles.item}>
                  <span className={styles.itemNome}>
                    {e.ordem}. {e.nome} {e.etapaFinal && '🏁'}
                  </span>
                  {!bloqueado && (
                    <button className={styles.botaoPerigo} onClick={() => excluirEtapa(e.id)}>
                      Excluir
                    </button>
                  )}
                </div>
              ))}
            </div>
            {!bloqueado && (
              <div className={styles.formulario}>
                <div className={styles.campo}>
                  <label htmlFor="et-nome">Nome</label>
                  <input id="et-nome" value={novaEtapaNome} onChange={(e) => setNovaEtapaNome(e.target.value)} />
                </div>
                <div className={styles.campo}>
                  <label htmlFor="et-ordem">Ordem</label>
                  <input
                    id="et-ordem"
                    type="number"
                    value={novaEtapaOrdem}
                    onChange={(e) => setNovaEtapaOrdem(Number(e.target.value))}
                  />
                </div>
                <label className={styles.checkboxLinha}>
                  <input type="checkbox" checked={novaEtapaFinal} onChange={(e) => setNovaEtapaFinal(e.target.checked)} />
                  Etapa final
                </label>
                <button className={styles.botao} disabled={!novaEtapaNome.trim()} onClick={criarEtapa}>
                  Criar etapa
                </button>
              </div>
            )}
          </div>

          <div className={styles.secao}>
            <h2 className={styles.secaoTitulo}>Transições</h2>
            <div className={styles.lista}>
              {transicoes.length === 0 && <p className={styles.vazio}>Nenhuma transição cadastrada.</p>}
              {transicoes.map((t) => (
                <div key={t.id} className={styles.item}>
                  <span className={styles.itemNome}>
                    {nomeEtapa(t.etapaOrigemId)} → {nomeEtapa(t.etapaDestinoId)} ({t.tipo})
                  </span>
                  {!bloqueado && (
                    <button className={styles.botaoPerigo} onClick={() => excluirTransicao(t.id)}>
                      Excluir
                    </button>
                  )}
                </div>
              ))}
            </div>
            {!bloqueado && (
              <div className={styles.formulario}>
                <div className={styles.campo}>
                  <label htmlFor="tr-origem">Origem</label>
                  <select id="tr-origem" value={origemId} onChange={(e) => setOrigemId(e.target.value)}>
                    <option value="">—</option>
                    {etapas.map((e) => (
                      <option key={e.id} value={e.id}>
                        {e.nome}
                      </option>
                    ))}
                  </select>
                </div>
                <div className={styles.campo}>
                  <label htmlFor="tr-destino">Destino</label>
                  <select id="tr-destino" value={destinoId} onChange={(e) => setDestinoId(e.target.value)}>
                    <option value="">—</option>
                    {etapas.map((e) => (
                      <option key={e.id} value={e.id}>
                        {e.nome}
                      </option>
                    ))}
                  </select>
                </div>
                <div className={styles.campo}>
                  <label htmlFor="tr-tipo">Tipo</label>
                  <select
                    id="tr-tipo"
                    value={tipoTransicao}
                    onChange={(e) => setTipoTransicao(e.target.value as TipoTransicao)}
                  >
                    <option value="NORMAL">Normal</option>
                    <option value="REABERTURA">Reabertura</option>
                  </select>
                </div>
                <button className={styles.botao} disabled={!origemId || !destinoId} onClick={criarTransicao}>
                  Criar transição
                </button>
              </div>
            )}
          </div>
        </>
      )}
    </>
  );
}
