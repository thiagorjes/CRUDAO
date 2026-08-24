'use client';

import { Fragment, useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { api, ApiError } from '@/lib/api/client';
import { Etapa, EventoBoard, Projeto, Raia, Tarefa, Transicao, Usuario } from '@/lib/api/types';
import { agruparPorRaiaEEtapa, RAIA_SEM_RAIA_ID } from '@/lib/board/agrupar';
import { conectarBoard } from '@/lib/board/realtime';
import { acoesDoMenu, AcaoTransicao, etapasAlvoValidas } from '@/lib/board/transicoes';
import { ModalErro } from '@/components/ui/ModalErro';
import { Skeleton } from '@/components/ui/Skeleton';
import { mostrarToast } from '@/components/ui/toast';
import { CardTarefa } from './CardTarefa';
import styles from './BoardApp.module.css';

type EstadoBoard = {
  projeto: Projeto;
  etapas: Etapa[];
  transicoes: Transicao[];
  raias: Raia[];
  tarefas: Tarefa[];
};

export function BoardApp() {
  const [projetos, setProjetos] = useState<Projeto[] | null>(null);
  const [projetoId, setProjetoId] = useState<string | null>(null);
  const [estado, setEstado] = useState<EstadoBoard | null>(null);
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [erro, setErro] = useState<string | null>(null);
  const [arrastando, setArrastando] = useState<string | null>(null);
  const [celulaSobre, setCelulaSobre] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Projeto[]>('/projetos')
      .then((lista) => {
        setProjetos(lista);
        const salvo = typeof window !== 'undefined' ? localStorage.getItem('crudao_projeto_id') : null;
        const inicial = lista.find((p) => p.id === salvo)?.id ?? lista[0]?.id ?? null;
        setProjetoId(inicial);
      })
      .catch(() => setErro('Não foi possível carregar os projetos.'));
    api.get<Usuario[]>('/usuarios').then(setUsuarios).catch(() => setUsuarios([]));
  }, []);

  const carregarBoard = useCallback(async (id: string) => {
    const projeto = await api.get<Projeto>(`/projetos/${id}`);
    if (!projeto.workflowAtivoId) {
      setEstado({ projeto, etapas: [], transicoes: [], raias: [], tarefas: [] });
      return;
    }
    const [etapas, transicoes, raias, tarefas] = await Promise.all([
      api.get<Etapa[]>(`/etapas?workflowId=${projeto.workflowAtivoId}`),
      api.get<Transicao[]>(`/transicoes?workflowId=${projeto.workflowAtivoId}`),
      api.get<Raia[]>(`/raias?projetoId=${id}`),
      api.get<Tarefa[]>(`/tarefas?projetoId=${id}`),
    ]);
    setEstado({
      projeto,
      etapas: [...etapas].sort((a, b) => a.ordem - b.ordem),
      transicoes,
      raias,
      tarefas,
    });
  }, []);

  useEffect(() => {
    if (!projetoId) return;
    localStorage.setItem('crudao_projeto_id', projetoId);
    let cancelado = false;
    (async () => {
      try {
        await carregarBoard(projetoId);
      } catch {
        if (!cancelado) setErro('Não foi possível carregar o board deste projeto.');
      }
    })();
    return () => {
      cancelado = true;
    };
  }, [projetoId, carregarBoard]);

  const atualizarTarefaLocal = useCallback((evento: EventoBoard) => {
    setEstado((atual) => {
      if (!atual || atual.projeto.id !== evento.projetoId) return atual;
      const existe = atual.tarefas.some((t) => t.id === evento.tarefaId);
      if (!existe) {
        // Tarefa criada por outro cliente ou movida para este projeto — busca o estado completo.
        // Reconfirma o projeto ao aplicar: o usuário pode ter trocado de board enquanto o fetch
        // estava em voo (finding do code review da TASK-05.1).
        api
          .get<Tarefa>(`/tarefas/${evento.tarefaId}`)
          .then((tarefa) =>
            setEstado((a) =>
              a && a.projeto.id === evento.projetoId ? { ...a, tarefas: [...a.tarefas, tarefa] } : a,
            ),
          )
          .catch(() => undefined);
        return atual;
      }
      return {
        ...atual,
        tarefas: atual.tarefas.map((t) =>
          t.id === evento.tarefaId
            ? { ...t, etapaAtualId: evento.etapaAtualId, impedida: evento.impedida }
            : t,
        ),
      };
    });
  }, []);

  // Tempo real (RF-005, RNF-001): reconsulta o estado da tarefa afetada em até 2s.
  useEffect(() => {
    if (!projetoId) return;
    return conectarBoard(projetoId, atualizarTarefaLocal, () =>
      mostrarToast('Conexão em tempo real perdida — atualize a página para reconectar.', 'info'),
    );
  }, [projetoId, atualizarTarefaLocal]);

  const usuariosPorId = useMemo(() => new Map(usuarios.map((u) => [u.id, u])), [usuarios]);

  const tarefaArrastando = estado?.tarefas.find((t) => t.id === arrastando) ?? null;
  const alvosValidos = useMemo(
    () =>
      tarefaArrastando && estado
        ? etapasAlvoValidas(tarefaArrastando.etapaAtualId, estado.transicoes)
        : new Set<string>(),
    [tarefaArrastando, estado],
  );

  async function mover(tarefaId: string, etapaDestinoId: string) {
    if (!estado) return;
    const tarefaAtual = estado.tarefas.find((t) => t.id === tarefaId);
    if (!tarefaAtual) return;
    // Atualização otimista — o evento STOMP da própria ação confirma (ou o catch reverte).
    setEstado((a) =>
      a
        ? { ...a, tarefas: a.tarefas.map((t) => (t.id === tarefaId ? { ...t, etapaAtualId: etapaDestinoId } : t)) }
        : a,
    );
    try {
      await api.patch(`/tarefas/${tarefaId}/mover`, { etapaDestinoId });
      mostrarToast('Tarefa movida.');
    } catch (e) {
      setEstado((a) =>
        a
          ? {
              ...a,
              tarefas: a.tarefas.map((t) => (t.id === tarefaId ? { ...t, etapaAtualId: tarefaAtual.etapaAtualId } : t)),
            }
          : a,
      );
      setErro(e instanceof ApiError ? e.message : 'Não foi possível mover a tarefa.');
    }
  }

  if (erro && !estado) {
    return (
      <div className={styles.pagina}>
        <p className={styles.vazio}>{erro}</p>
      </div>
    );
  }

  if (!projetos || (projetoId && !estado)) {
    return (
      <div className={styles.pagina}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 260px)', gap: 12 }}>
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} />
          ))}
        </div>
      </div>
    );
  }

  if (projetos.length === 0) {
    return (
      <div className={styles.pagina}>
        <p className={styles.vazio}>Nenhum projeto cadastrado ainda.</p>
      </div>
    );
  }

  return (
    <div className={styles.pagina}>
      <div className={styles.cabecalho}>
        <h1 className={styles.titulo}>{estado?.projeto.nome}</h1>
        <select
          className={styles.seletorProjeto}
          value={projetoId ?? ''}
          onChange={(e) => setProjetoId(e.target.value)}
        >
          {projetos.map((p) => (
            <option key={p.id} value={p.id}>
              {p.nome}
            </option>
          ))}
        </select>
        <Link href="/dashboard" style={{ marginLeft: 'auto', font: 'var(--font-body)' }}>
          Dashboard →
        </Link>
        <Link href="/admin" style={{ font: 'var(--font-body)' }}>
          Configurações do projeto →
        </Link>
      </div>

      {estado && !estado.projeto.workflowAtivoId && (
        <p className={styles.vazio}>Este projeto ainda não tem um workflow ativo definido.</p>
      )}

      {estado && estado.projeto.workflowAtivoId && (
        <BoardGrid
          etapas={estado.etapas}
          raias={estado.raias}
          tarefas={estado.tarefas}
          transicoes={estado.transicoes}
          usuariosPorId={usuariosPorId}
          alvosValidos={alvosValidos}
          arrastando={arrastando}
          celulaSobre={celulaSobre}
          onArrastarInicio={setArrastando}
          onArrastarFim={() => {
            setArrastando(null);
            setCelulaSobre(null);
          }}
          onCelulaSobre={setCelulaSobre}
          onSoltar={(etapaId) => {
            if (arrastando) mover(arrastando, etapaId);
            setArrastando(null);
            setCelulaSobre(null);
          }}
          onExecutarAcao={(tarefaId, acao: AcaoTransicao) => mover(tarefaId, acao.etapaDestinoId)}
        />
      )}

      {erro && <ModalErro mensagem={erro} onFechar={() => setErro(null)} />}
    </div>
  );
}

function BoardGrid({
  etapas,
  raias,
  tarefas,
  transicoes,
  usuariosPorId,
  alvosValidos,
  arrastando,
  celulaSobre,
  onArrastarInicio,
  onArrastarFim,
  onCelulaSobre,
  onSoltar,
  onExecutarAcao,
}: {
  etapas: Etapa[];
  raias: Raia[];
  tarefas: Tarefa[];
  transicoes: Transicao[];
  usuariosPorId: Map<string, Usuario>;
  alvosValidos: Set<string>;
  arrastando: string | null;
  celulaSobre: string | null;
  onArrastarInicio: (tarefaId: string) => void;
  onArrastarFim: () => void;
  onCelulaSobre: (chave: string | null) => void;
  onSoltar: (etapaId: string) => void;
  onExecutarAcao: (tarefaId: string, acao: AcaoTransicao) => void;
}) {
  const grade = useMemo(() => agruparPorRaiaEEtapa(tarefas, raias), [tarefas, raias]);
  const linhasRaia = raias.length > 0 ? raias : [{ id: RAIA_SEM_RAIA_ID, projetoId: null, nome: 'Tarefas', ordem: 0 }];

  if (etapas.length === 0) {
    return <p className={styles.vazio}>O workflow ativo ainda não tem etapas configuradas.</p>;
  }

  return (
    <div
      className={styles.grade}
      style={{ gridTemplateColumns: `180px repeat(${etapas.length}, 260px)` }}
    >
      <div />
      {etapas.map((etapa) => {
        const contagem = tarefas.filter((t) => t.etapaAtualId === etapa.id).length;
        const destacada = arrastando !== null && alvosValidos.has(etapa.id);
        return (
          <div
            key={etapa.id}
            className={`${styles.colunaHeader} ${destacada ? styles.colunaHeaderDestacada : ''}`}
          >
            {etapa.etapaFinal && <span title="Etapa final">🏁</span>}
            <span className={styles.colunaHeaderNome}>{etapa.nome}</span>
            <span className={styles.colunaHeaderContagem}>{contagem}</span>
          </div>
        );
      })}

      {linhasRaia.map((raia) => (
        <Fragment key={raia.id}>
          <div className={styles.cabecalhoRaia}>
            {raia.nome}
          </div>
          {etapas.map((etapa) => {
            const chave = `${raia.id}:${etapa.id}`;
            const valida = arrastando !== null && alvosValidos.has(etapa.id);
            const destacadaAgora = valida && celulaSobre === chave;
            return (
              <div
                key={chave}
                data-testid={`celula-etapa-${etapa.id}`}
                className={`${styles.celula} ${valida ? styles.celulaValida : ''} ${destacadaAgora ? styles.celulaValida : ''}`}
                onDragOver={(e) => {
                  if (!valida) return;
                  e.preventDefault();
                  onCelulaSobre(chave);
                }}
                onDragLeave={() => onCelulaSobre(null)}
                onDrop={(e) => {
                  e.preventDefault();
                  if (valida) onSoltar(etapa.id);
                }}
              >
                {(grade.get(raia.id)?.get(etapa.id) ?? []).map((tarefa) => (
                  <CardTarefa
                    key={tarefa.id}
                    tarefa={tarefa}
                    responsavel={tarefa.responsavelId ? usuariosPorId.get(tarefa.responsavelId) : undefined}
                    acoes={acoesDoMenu(tarefa.etapaAtualId, transicoes, etapas)}
                    onArrastarInicio={onArrastarInicio}
                    onArrastarFim={onArrastarFim}
                    onExecutarAcao={(acao) => onExecutarAcao(tarefa.id, acao)}
                  />
                ))}
              </div>
            );
          })}
        </Fragment>
      ))}
    </div>
  );
}
