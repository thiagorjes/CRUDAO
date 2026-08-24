'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import { api, ApiError } from '@/lib/api/client';
import { DashboardResultado, Etapa, Projeto } from '@/lib/api/types';
import { conectarDashboard } from '@/lib/board/realtime';
import { formatarDuracao } from '@/lib/board/tempo';
import { ModalErro } from '@/components/ui/ModalErro';
import { Skeleton } from '@/components/ui/Skeleton';
import styles from './DashboardApp.module.css';

const INTERVALO_POLLING_MS = 2500;

function dataIsoInicioDoDia(data: string): string {
  return new Date(`${data}T00:00:00`).toISOString();
}

function dataIsoFimDoDia(data: string): string {
  return new Date(`${data}T23:59:59.999`).toISOString();
}

function periodoPadrao(): { inicio: string; fim: string } {
  const hoje = new Date();
  const trintaDiasAtras = new Date(hoje);
  trintaDiasAtras.setDate(hoje.getDate() - 30);
  return { inicio: trintaDiasAtras.toISOString().slice(0, 10), fim: hoje.toISOString().slice(0, 10) };
}

export function DashboardApp() {
  const [projetos, setProjetos] = useState<Projeto[] | null>(null);
  const [projetoId, setProjetoId] = useState<string | null>(null);
  const [etapas, setEtapas] = useState<Etapa[]>([]);
  const [{ inicio, fim }, setPeriodo] = useState(periodoPadrao);
  const [resultado, setResultado] = useState<DashboardResultado | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const desconectarRef = useRef<(() => void) | null>(null);

  const pararAcompanhamento = useCallback(() => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
    desconectarRef.current?.();
    desconectarRef.current = null;
  }, []);

  useEffect(() => () => pararAcompanhamento(), [pararAcompanhamento]);

  useEffect(() => {
    api
      .get<Projeto[]>('/projetos')
      .then((lista) => {
        setProjetos(lista);
        const salvo = typeof window !== 'undefined' ? localStorage.getItem('crudao_projeto_id') : null;
        setProjetoId(lista.find((p) => p.id === salvo)?.id ?? lista[0]?.id ?? null);
      })
      .catch(() => setErro('Não foi possível carregar os projetos.'));
  }, []);

  useEffect(() => {
    pararAcompanhamento();
    let cancelado = false;
    (async () => {
      setResultado(null);
      setEtapas([]);
      if (!projetoId) return;
      if (typeof window !== 'undefined') localStorage.setItem('crudao_projeto_id', projetoId);
      try {
        const projeto = await api.get<Projeto>(`/projetos/${projetoId}`);
        if (!projeto.workflowAtivoId || cancelado) return;
        const lista = await api.get<Etapa[]>(`/etapas?workflowId=${projeto.workflowAtivoId}`);
        if (!cancelado) setEtapas([...lista].sort((a, b) => a.ordem - b.ordem));
      } catch {
        if (!cancelado) setErro('Não foi possível carregar as etapas do projeto.');
      }
    })();
    return () => {
      cancelado = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projetoId]);

  const tratarResultado = useCallback(
    (dados: DashboardResultado) => {
      if (dados.status === 'PROCESSANDO') return;
      pararAcompanhamento();
      setCarregando(false);
      if (dados.status === 'ERRO') {
        setErro('O cálculo do dashboard falhou. Tente novamente.');
        return;
      }
      setResultado(dados);
    },
    [pararAcompanhamento],
  );

  function iniciarPolling(idProjeto: string, jobId: string) {
    pollingRef.current = setInterval(async () => {
      try {
        const dados = await api.get<DashboardResultado>(`/projetos/${idProjeto}/dashboard/jobs/${jobId}`);
        tratarResultado(dados);
      } catch {
        // Tenta novamente no próximo tick — falha pontual de rede não deve interromper o polling.
      }
    }, INTERVALO_POLLING_MS);
  }

  // Consulta avulsa ao job assim que a subscription STOMP é confirmada — cobre a corrida em que o
  // cálculo (rápido, poucos registros) já publicou o resultado via pg_notify antes do handshake
  // terminar, cenário em que a conexão não "falha" e o fallback de polling nunca seria acionado
  // (achado de code review da TASK-05.2).
  async function buscarResultadoAgora(idProjeto: string, jobId: string) {
    try {
      const dados = await api.get<DashboardResultado>(`/projetos/${idProjeto}/dashboard/jobs/${jobId}`);
      tratarResultado(dados);
    } catch {
      // Ignora — se o job realmente ainda está processando, o resultado chega pelo STOMP.
    }
  }

  async function calcular() {
    if (!projetoId) return;
    pararAcompanhamento();
    setResultado(null);
    setCarregando(true);
    setErro(null);
    try {
      const { jobId } = await api.post<{ jobId: string }>(`/projetos/${projetoId}/dashboard/calcular`, {
        dataInicio: dataIsoInicioDoDia(inicio),
        dataFim: dataIsoFimDoDia(fim),
      });
      // Resultado chega via STOMP (RF-007); se a conexão falhar, cai para polling (critério de
      // aceite desta task).
      desconectarRef.current = conectarDashboard<DashboardResultado>(
        projetoId,
        jobId,
        tratarResultado,
        () => iniciarPolling(projetoId, jobId),
        () => buscarResultadoAgora(projetoId, jobId),
      );
    } catch (e) {
      setCarregando(false);
      setErro(e instanceof ApiError ? e.message : 'Não foi possível iniciar o cálculo do dashboard.');
    }
  }

  const etapasPorId = useMemo(() => new Map(etapas.map((e) => [e.id, e])), [etapas]);

  const linhas = useMemo(() => {
    if (!resultado) return [];
    return etapas.map((etapa) => ({
      etapa,
      leadTime: resultado.leadTimeMedioPorEtapaSegundos[etapa.id] ?? 0,
      impedimento: resultado.tempoMedioImpedimentoPorEtapaSegundos[etapa.id] ?? 0,
    }));
  }, [resultado, etapas]);

  const maxLeadTime = Math.max(...linhas.map((l) => l.leadTime), 1);
  const tempoMedioImpedimentoGeral = linhas.length
    ? linhas.reduce((soma, l) => soma + l.impedimento, 0) / linhas.length
    : 0;

  if (erro && !projetos) {
    return (
      <div className={styles.pagina}>
        <p className={styles.vazio}>{erro}</p>
      </div>
    );
  }

  if (!projetos) {
    return (
      <div className={styles.pagina}>
        <Skeleton altura={280} />
      </div>
    );
  }

  return (
    <div className={styles.pagina}>
      <Link href="/" className={styles.voltar}>
        ← Voltar ao board
      </Link>

      <div className={styles.cabecalho}>
        <h1 className={styles.titulo}>Dashboard de Gestão</h1>
      </div>
      <p className={styles.subtitulo}>Lead-time médio por etapa e tempo médio em impedimento (RF-006, RF-007)</p>

      <div className={styles.seletores}>
        <div className={styles.campo}>
          <span className={styles.campoLabel}>Projeto</span>
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
        </div>
        <div className={styles.campo}>
          <span className={styles.campoLabel}>Data início</span>
          <input type="date" value={inicio} max={fim} onChange={(e) => setPeriodo((p) => ({ ...p, inicio: e.target.value }))} />
        </div>
        <div className={styles.campo}>
          <span className={styles.campoLabel}>Data fim</span>
          <input type="date" value={fim} min={inicio} onChange={(e) => setPeriodo((p) => ({ ...p, fim: e.target.value }))} />
        </div>
        <button className={styles.botaoAtualizar} onClick={calcular} disabled={!projetoId || carregando}>
          {carregando ? 'Calculando…' : 'Atualizar'}
        </button>
        <span className={styles.avisoAssincrono}>
          Cálculo processado em segundo plano — resultado chega automaticamente
        </span>
      </div>

      {carregando && (
        <>
          <div className={styles.linha}>
            <Skeleton altura={280} />
            <Skeleton altura={280} />
          </div>
          <Skeleton altura={200} />
        </>
      )}

      {!carregando && resultado && linhas.length > 0 && (
        <>
          <div className={styles.linha}>
            <div className={styles.painel}>
              <div className={styles.painelTitulo}>Lead-time médio por etapa</div>
              <div className={styles.grafico}>
                {linhas.map(({ etapa, leadTime }) => (
                  <div key={etapa.id} className={styles.barraColuna}>
                    <span className={styles.barraValor}>{formatarDuracao(leadTime)}</span>
                    <div
                      className={styles.barra}
                      style={{ height: Math.max(8, Math.round((leadTime / maxLeadTime) * 170)) }}
                    />
                    <span className={styles.barraNome}>{etapa.nome}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className={styles.painel}>
              <div className={styles.impedimentoResumo}>
                <span className={styles.impedimentoLabel}>
                  <span className={styles.bolinhaErro} />
                  Tempo médio em impedimento
                </span>
                <span className={styles.impedimentoValor}>{formatarDuracao(tempoMedioImpedimentoGeral)}</span>
                <span className={styles.campoLabel}>no período selecionado, média entre as etapas</span>
              </div>
            </div>
          </div>

          <div className={styles.painel} style={{ padding: 0, overflow: 'hidden' }}>
            <table className={styles.tabela}>
              <thead>
                <tr>
                  <th>Etapa</th>
                  <th>Lead-time médio</th>
                  <th>Tempo médio em impedimento</th>
                </tr>
              </thead>
              <tbody>
                {linhas.map(({ etapa, leadTime, impedimento }) => (
                  <tr key={etapa.id}>
                    <td>{etapasPorId.get(etapa.id)?.nome ?? etapa.nome}</td>
                    <td>{formatarDuracao(leadTime)}</td>
                    <td style={{ color: impedimento > 0 ? 'var(--color-error)' : undefined }}>
                      {formatarDuracao(impedimento)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {!carregando && resultado && linhas.length === 0 && (
        <p className={styles.vazio}>O workflow ativo deste projeto ainda não tem etapas configuradas.</p>
      )}

      {!carregando && !resultado && projetoId && (
        <p className={styles.vazio}>Selecione o período e clique em Atualizar para calcular o dashboard.</p>
      )}

      {erro && <ModalErro mensagem={erro} onFechar={() => setErro(null)} />}
    </div>
  );
}
