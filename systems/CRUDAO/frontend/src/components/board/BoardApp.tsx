'use client';

import { Fragment, useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { api, ApiError } from '@/lib/api/client';
import {
  ConfiguracaoProjeto,
  Etapa,
  EventoBoard,
  Projeto,
  Raia,
  Tarefa,
  Transicao,
  Usuario,
  UsuarioMe,
} from '@/lib/api/types';
import { agruparPorRaiaEEtapa, RAIA_SEM_RAIA_ID } from '@/lib/board/agrupar';
import { resolverDefaults } from '@/lib/board/defaults';
import { aplicarTarefaExcluida } from '@/lib/board/eventos';
import { conectarBoard } from '@/lib/board/realtime';
import { acoesDoMenu, AcaoTransicao, etapasAlvoValidas } from '@/lib/board/transicoes';
import { ehDevTier, permissoesDoProjeto } from '@/lib/rbac';
import { ModalErro } from '@/components/ui/ModalErro';
import { Skeleton } from '@/components/ui/Skeleton';
import { mostrarToast } from '@/components/ui/toast';
import { CardTarefa } from './CardTarefa';
import { ModalConfirmacao } from './ModalConfirmacao';
import { ModalNovoCard, NovoCardValores } from './ModalNovoCard';
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
  const [usuarioMe, setUsuarioMe] = useState<UsuarioMe | null>(null);
  const [configuracao, setConfiguracao] = useState<{ projetoId: string; dados: ConfiguracaoProjeto | null } | null>(
    null,
  );
  const [erro, setErro] = useState<string | null>(null);
  const [arrastando, setArrastando] = useState<string | null>(null);
  const [celulaSobre, setCelulaSobre] = useState<string | null>(null);
  const [modalNovoCardAberto, setModalNovoCardAberto] = useState(false);
  const [tarefaParaExcluir, setTarefaParaExcluir] = useState<Tarefa | null>(null);

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
    api.get<UsuarioMe>('/usuarios/me').then(setUsuarioMe).catch(() => setUsuarioMe(null));
  }, []);

  // RBAC gating (RF-001/RF-002, TASK-02.1) — recalculado a cada troca de projeto; backend revalida
  // tudo (RNF-003/ADR-006). Falha ao buscar a configuração é fallback seguro: nunca expõe a lixeira.
  useEffect(() => {
    if (!projetoId) return;
    let cancelado = false;
    api
      .get<ConfiguracaoProjeto>(`/projetos/${projetoId}/configuracao`)
      .then((config) => {
        if (!cancelado) setConfiguracao({ projetoId, dados: config });
      })
      .catch(() => {
        // Falha de rede/erro: fallback seguro — nunca expõe a exclusão por erro de fetch.
        if (!cancelado) setConfiguracao({ projetoId, dados: null });
      });
    return () => {
      cancelado = true;
    };
  }, [projetoId]);

  // Só usa `configuracao` se pertencer ao projeto atual — evita, na janela entre trocar de
  // projeto e a resposta do fetch chegar, calcular podeExcluirTarefa com o toggle do projeto
  // anterior (finding do code review da TASK-02.1).
  const configuracaoAtual = configuracao?.projetoId === projetoId ? configuracao.dados : null;

  const permissoesProjeto = useMemo(
    () => permissoesDoProjeto(usuarioMe, projetoId),
    [usuarioMe, projetoId],
  );
  const podeGerenciarTarefa = permissoesProjeto.has('tarefa:gerenciar');
  const podeExcluirTarefa =
    podeGerenciarTarefa &&
    (!ehDevTier(permissoesProjeto) || (configuracaoAtual?.devPodeExcluirTarefa ?? false));

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
    if (evento.tipo === 'TAREFA_EXCLUIDA') {
      setEstado((atual) => {
        if (!atual) return atual;
        const resultado = aplicarTarefaExcluida({ projetoId: atual.projeto.id, tarefas: atual.tarefas }, evento);
        return resultado === null || resultado.tarefas === atual.tarefas
          ? atual
          : { ...atual, tarefas: resultado.tarefas };
      });
      return;
    }
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

  async function criarCard(valores: NovoCardValores) {
    if (!estado || !projetoId) return;
    const { etapaInicialId, raiaId } = resolverDefaults(estado.etapas, estado.raias);
    if (!etapaInicialId) return; // botão fica desabilitado nesse estado — guard defensivo
    try {
      const tarefa = await api.post<Tarefa>('/tarefas', {
        projetoId,
        etapaInicialId,
        raiaId,
        tipo: valores.tipo,
        titulo: valores.titulo,
        descricao: valores.descricao || null,
        responsavelId: null,
      });
      // Atualização direta (sem esperar o próprio evento STOMP) — mesmo padrão de `mover()`;
      // inserida ordenada por `criadoEm`, consistente com o que um refetch produziria.
      setEstado((a) =>
        a ? { ...a, tarefas: [...a.tarefas, tarefa].sort((x, y) => x.criadoEm.localeCompare(y.criadoEm)) } : a,
      );
      setModalNovoCardAberto(false);
      mostrarToast('Card criado.');
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Não foi possível criar o card.');
    }
  }

  async function excluirCard() {
    if (!tarefaParaExcluir) return;
    const tarefaId = tarefaParaExcluir.id;
    try {
      await api.delete(`/tarefas/${tarefaId}`);
      // Atualização direta (sem esperar o próprio evento STOMP) — mesmo padrão de `criarCard()`.
      setEstado((a) => (a ? { ...a, tarefas: a.tarefas.filter((t) => t.id !== tarefaId) } : a));
      setTarefaParaExcluir(null);
      mostrarToast('Card excluído.');
    } catch (e) {
      setTarefaParaExcluir(null);
      // 404 = card já excluído por outro cliente — remove localmente também, senão fica órfão
      // no estado até o evento STOMP tardio ou reload (finding do code review da TASK-02.3).
      if (e instanceof ApiError && e.status === 404) {
        setEstado((a) => (a ? { ...a, tarefas: a.tarefas.filter((t) => t.id !== tarefaId) } : a));
      }
      setErro(e instanceof ApiError ? e.message : 'Não foi possível excluir o card.');
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
        {/* Sem projeto algum, esta era a única tela sem link para o /admin — quem tem
            projeto:gerenciar precisa chegar lá para criar o primeiro (achado de uso real). */}
        <Link href="/admin" style={{ font: 'var(--font-body)' }}>
          Ir para Configurações →
        </Link>
      </div>
    );
  }

  return (
    <div className={styles.pagina}>
      <div className={styles.cabecalho} data-pode-gerenciar-tarefa={podeGerenciarTarefa}>
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
        {podeGerenciarTarefa && (
          <button
            className={styles.botaoNovoCard}
            data-testid="botao-novo-card"
            style={{ marginLeft: 'auto' }}
            disabled={!estado || estado.etapas.length === 0}
            title={estado && estado.etapas.length === 0 ? 'O workflow ativo ainda não tem etapas configuradas.' : undefined}
            onClick={() => setModalNovoCardAberto(true)}
          >
            + Novo card
          </button>
        )}
        <Link
          href="/dashboard"
          style={{ marginLeft: podeGerenciarTarefa ? undefined : 'auto', font: 'var(--font-body)' }}
        >
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
          podeExcluirTarefa={podeExcluirTarefa}
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
          onExcluir={(tarefa) => setTarefaParaExcluir(tarefa)}
        />
      )}

      {modalNovoCardAberto && (
        <ModalNovoCard onSalvar={criarCard} onFechar={() => setModalNovoCardAberto(false)} />
      )}

      {tarefaParaExcluir && (
        <ModalConfirmacao
          titulo="Excluir card"
          mensagem={`Tem certeza que deseja excluir o card "${tarefaParaExcluir.titulo}"? Esta ação não pode ser desfeita.`}
          rotuloConfirmar="Excluir"
          onConfirmar={excluirCard}
          onFechar={() => setTarefaParaExcluir(null)}
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
  podeExcluirTarefa,
  alvosValidos,
  arrastando,
  celulaSobre,
  onArrastarInicio,
  onArrastarFim,
  onCelulaSobre,
  onSoltar,
  onExecutarAcao,
  onExcluir,
}: {
  etapas: Etapa[];
  raias: Raia[];
  tarefas: Tarefa[];
  transicoes: Transicao[];
  usuariosPorId: Map<string, Usuario>;
  podeExcluirTarefa: boolean;
  alvosValidos: Set<string>;
  arrastando: string | null;
  celulaSobre: string | null;
  onArrastarInicio: (tarefaId: string) => void;
  onArrastarFim: () => void;
  onCelulaSobre: (chave: string | null) => void;
  onSoltar: (etapaId: string) => void;
  onExecutarAcao: (tarefaId: string, acao: AcaoTransicao) => void;
  onExcluir: (tarefa: Tarefa) => void;
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
                    podeExcluirTarefa={podeExcluirTarefa}
                    onArrastarInicio={onArrastarInicio}
                    onArrastarFim={onArrastarFim}
                    onExecutarAcao={(acao) => onExecutarAcao(tarefa.id, acao)}
                    onExcluir={() => onExcluir(tarefa)}
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
