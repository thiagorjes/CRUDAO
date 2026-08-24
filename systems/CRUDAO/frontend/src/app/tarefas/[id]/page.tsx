'use client';

import { use, useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { api, ApiError } from '@/lib/api/client';
import {
  AuditoriaTarefa,
  ConfiguracaoProjeto,
  Etapa,
  RegistroEtapa,
  Tarefa,
  Usuario,
  UsuarioMe,
} from '@/lib/api/types';
import { duracaoEntre, formatarDuracao } from '@/lib/board/tempo';
import { ModalErro } from '@/components/ui/ModalErro';
import { Skeleton } from '@/components/ui/Skeleton';
import { mostrarToast } from '@/components/ui/toast';

const ROTULO_CAMPO: Record<AuditoriaTarefa['campo'], string> = {
  RESPONSAVEL: 'Responsável',
  TITULO: 'Título',
  DESCRICAO: 'Descrição',
  ETAPA: 'Etapa',
};

export default function TarefaDetalhePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [tarefa, setTarefa] = useState<Tarefa | null>(null);
  const [etapa, setEtapa] = useState<Etapa | null>(null);
  const [registros, setRegistros] = useState<RegistroEtapa[] | null>(null);
  const [observadores, setObservadores] = useState<string[]>([]);
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [usuarioMe, setUsuarioMe] = useState<UsuarioMe | null>(null);
  const [configuracao, setConfiguracao] = useState<ConfiguracaoProjeto | null>(null);
  const [historico, setHistorico] = useState<AuditoriaTarefa[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  const [editando, setEditando] = useState(false);
  const [tituloEdicao, setTituloEdicao] = useState('');
  const [descricaoEdicao, setDescricaoEdicao] = useState('');

  const carregar = useCallback(async () => {
    try {
      const [t, regs, obs, usrs, me, hist] = await Promise.all([
        api.get<Tarefa>(`/tarefas/${id}`),
        api.get<RegistroEtapa[]>(`/tarefas/${id}/registros-etapa`),
        api.get<string[]>(`/tarefas/${id}/observadores`),
        api.get<Usuario[]>('/usuarios'),
        api.get<UsuarioMe>('/usuarios/me'),
        api.get<AuditoriaTarefa[]>(`/tarefas/${id}/historico`),
      ]);
      setTarefa(t);
      setRegistros(regs);
      setObservadores(obs);
      setUsuarios(usrs);
      setUsuarioMe(me);
      setHistorico(hist);
      // Sem GET /etapas/{id} no backend — lista por workflow e filtra localmente.
      const etapasDoWorkflow = await api
        .get<Etapa[]>(`/etapas?workflowId=${t.workflowId}`)
        .catch(() => []);
      setEtapa(etapasDoWorkflow.find((e) => e.id === t.etapaAtualId) ?? null);
      const config = await api
        .get<ConfiguracaoProjeto>(`/projetos/${t.projetoId}/configuracao`)
        .catch(() => null);
      setConfiguracao(config);
    } catch {
      setErro('Não foi possível carregar a tarefa.');
    }
  }, [id]);

  useEffect(() => {
    (async () => {
      await carregar();
    })();
  }, [carregar]);

  // Gating de UX (backend é a fonte real de autorização — RNF-003) — mesmo padrão do AdminApp.
  const permissoesProjeto = useMemo(() => {
    if (!usuarioMe || !tarefa) return new Set<string>();
    if (usuarioMe.admin) return new Set(['tarefa:gerenciar', 'tarefa:atribuir', 'tarefa:finalizar']);
    const vinculo = usuarioMe.projetos.find((p) => p.projetoId === tarefa.projetoId);
    return new Set(vinculo?.permissoes ?? []);
  }, [usuarioMe, tarefa]);

  const podeAtribuirOutro = permissoesProjeto.has('tarefa:atribuir');
  // "dev-tier": tem tarefa:gerenciar mas não tarefa:atribuir (mesma heurística do backend —
  // único papel seedado com essa combinação, ver nota técnica da TASK-02.3).
  const ehDevTier = permissoesProjeto.has('tarefa:gerenciar') && !podeAtribuirOutro;
  const edicaoTravada =
    Boolean(tarefa?.iniciada) && ehDevTier && !(configuracao?.devPodeEditarTarefaIniciada ?? false);

  function iniciarEdicao() {
    if (!tarefa) return;
    setTituloEdicao(tarefa.titulo);
    setDescricaoEdicao(tarefa.descricao ?? '');
    setEditando(true);
  }

  async function salvarEdicao() {
    if (!tarefa) return;
    try {
      const atualizada = await api.put<Tarefa>(`/tarefas/${id}`, {
        projetoId: tarefa.projetoId,
        raiaId: tarefa.raiaId,
        tipo: tarefa.tipo,
        titulo: tituloEdicao,
        descricao: descricaoEdicao || null,
        responsavelId: tarefa.responsavelId,
      });
      setTarefa(atualizada);
      setEditando(false);
      mostrarToast('Tarefa atualizada.');
      api
        .get<AuditoriaTarefa[]>(`/tarefas/${id}/historico`)
        .then(setHistorico)
        .catch(() => undefined);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Não foi possível salvar as alterações.');
    }
  }

  async function atribuir(usuarioId: string) {
    try {
      const atualizada = await api.patch<Tarefa>(`/tarefas/${id}/responsavel`, { usuarioId });
      setTarefa(atualizada);
      mostrarToast('Responsável atualizado.');
      api
        .get<AuditoriaTarefa[]>(`/tarefas/${id}/historico`)
        .then(setHistorico)
        .catch(() => undefined);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Não foi possível atribuir a tarefa.');
    }
  }

  async function alternarImpedimento() {
    if (!tarefa) return;
    try {
      const atualizada = tarefa.impedida
        ? await api.delete<Tarefa>(`/tarefas/${id}/impedimento`)
        : await api.post<Tarefa>(`/tarefas/${id}/impedimento`, {});
      setTarefa(atualizada);
      mostrarToast(atualizada.impedida ? 'Tarefa marcada como impedida.' : 'Impedimento removido.');
      // Só o histórico de tempo por etapa muda ao (des)marcar impedimento — evita re-buscar
      // usuários/observadores, que não mudaram (achado de code review da TASK-05.1).
      api
        .get<RegistroEtapa[]>(`/tarefas/${id}/registros-etapa`)
        .then(setRegistros)
        .catch(() => undefined);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Não foi possível alterar o impedimento.');
    }
  }

  async function alternarObservador(usuarioId: string, ehObservador: boolean) {
    try {
      if (ehObservador) {
        await api.delete(`/tarefas/${id}/observadores/${usuarioId}`);
        setObservadores((atual) => atual.filter((o) => o !== usuarioId));
      } else {
        await api.post(`/tarefas/${id}/observadores/${usuarioId}`);
        setObservadores((atual) => [...atual, usuarioId]);
      }
    } catch {
      setErro('Não foi possível atualizar os observadores.');
    }
  }

  if (erro && !tarefa) {
    return (
      <div style={{ padding: 'var(--spacing-xl)' }}>
        <p>{erro}</p>
        <Link href="/">Voltar ao board</Link>
      </div>
    );
  }

  if (!tarefa) {
    return (
      <div style={{ padding: 'var(--spacing-xl)', maxWidth: 640 }}>
        <Skeleton altura={200} />
      </div>
    );
  }

  const responsavel = usuarios.find((u) => u.id === tarefa.responsavelId);
  const meuId = usuarioMe?.id;

  return (
    <div style={{ padding: 'var(--spacing-xl)', maxWidth: 640, margin: '0 auto' }}>
      <Link href="/" style={{ font: 'var(--font-caption)' }}>
        ← Voltar ao board
      </Link>

      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)', margin: 'var(--spacing-md) 0' }}>
        <span
          style={{
            fontSize: 11,
            fontWeight: 700,
            textTransform: 'uppercase',
            color: 'var(--color-primary)',
            background: '#e7f1ff',
            padding: '2px 8px',
            borderRadius: 4,
          }}
        >
          {tarefa.tipo}
        </span>
        {tarefa.impedida && (
          <span style={{ font: 'var(--font-caption)', color: 'var(--color-error)', fontWeight: 700 }}>
            ● Impedida
          </span>
        )}
        {tarefa.iniciada && (
          <span style={{ font: 'var(--font-caption)', color: 'var(--color-text-secondary)' }}>Iniciada</span>
        )}
      </div>

      {editando ? (
        <div style={{ marginBottom: 'var(--spacing-md)' }}>
          <input
            value={tituloEdicao}
            onChange={(e) => setTituloEdicao(e.target.value)}
            readOnly={edicaoTravada}
            style={{ font: 'var(--font-heading-1)', width: '100%', marginBottom: 'var(--spacing-sm)' }}
          />
          <textarea
            value={descricaoEdicao}
            onChange={(e) => setDescricaoEdicao(e.target.value)}
            readOnly={edicaoTravada}
            rows={3}
            style={{ font: 'var(--font-body)', width: '100%', marginBottom: 'var(--spacing-sm)' }}
          />
          {edicaoTravada && (
            <p style={{ font: 'var(--font-caption)', color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-sm)' }}>
              Tarefa já iniciada — edição travada para o seu papel neste projeto.
            </p>
          )}
          <div style={{ display: 'flex', gap: 'var(--spacing-sm)' }}>
            {!edicaoTravada && <button onClick={salvarEdicao}>Salvar</button>}
            <button onClick={() => setEditando(false)}>Cancelar</button>
          </div>
        </div>
      ) : (
        <>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 'var(--spacing-sm)' }}>
            <h1 style={{ font: 'var(--font-heading-1)', marginBottom: 'var(--spacing-sm)' }}>{tarefa.titulo}</h1>
            <button
              onClick={iniciarEdicao}
              title={edicaoTravada ? 'Tarefa já iniciada — edição travada para este papel' : 'Editar'}
              style={{ font: 'var(--font-caption)', color: 'var(--color-text-secondary)' }}
            >
              {edicaoTravada ? '🔒 Editar' : '✎ Editar'}
            </button>
          </div>
          {tarefa.descricao && (
            <p style={{ font: 'var(--font-body)', color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-md)' }}>
              {tarefa.descricao}
            </p>
          )}
        </>
      )}

      <dl style={{ font: 'var(--font-body)', marginBottom: 'var(--spacing-md)' }}>
        <div style={{ display: 'flex', gap: 'var(--spacing-sm)' }}>
          <dt style={{ color: 'var(--color-text-secondary)' }}>Etapa atual:</dt>
          <dd>{etapa?.nome ?? '—'}</dd>
        </div>
        <div style={{ display: 'flex', gap: 'var(--spacing-sm)' }}>
          <dt style={{ color: 'var(--color-text-secondary)' }}>Responsável:</dt>
          <dd>{responsavel?.nome ?? 'Sem responsável'}</dd>
        </div>
      </dl>

      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)', marginBottom: 'var(--spacing-lg)' }}>
        {meuId && meuId !== tarefa.responsavelId && (
          <button onClick={() => atribuir(meuId)} style={{ font: 'var(--font-body)' }}>
            Atribuir a mim
          </button>
        )}
        {podeAtribuirOutro && (
          <select
            value={tarefa.responsavelId ?? ''}
            onChange={(e) => e.target.value && atribuir(e.target.value)}
            style={{ font: 'var(--font-body)' }}
          >
            <option value="">Reatribuir a…</option>
            {usuarios.map((u) => (
              <option key={u.id} value={u.id}>
                {u.nome}
              </option>
            ))}
          </select>
        )}
      </div>

      <button
        onClick={alternarImpedimento}
        style={{
          padding: '8px 16px',
          borderRadius: 6,
          border: '1px solid var(--color-error)',
          background: tarefa.impedida ? 'var(--color-error)' : 'var(--color-surface)',
          color: tarefa.impedida ? '#fff' : 'var(--color-error)',
          font: 'var(--font-body)',
          marginBottom: 'var(--spacing-xl)',
        }}
      >
        {tarefa.impedida ? 'Remover impedimento' : 'Marcar como impedida'}
      </button>

      <h2 style={{ font: 'var(--font-heading-2)', marginBottom: 'var(--spacing-sm)' }}>
        Tempo por etapa (RF-006)
      </h2>
      {registros === null ? (
        <Skeleton altura={80} />
      ) : registros.length === 0 ? (
        <p style={{ color: 'var(--color-text-secondary)', font: 'var(--font-body)' }}>Sem histórico ainda.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', font: 'var(--font-body)', marginBottom: 'var(--spacing-xl)' }}>
          <thead>
            <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--color-border)' }}>
              <th style={{ padding: '6px 4px' }}>Etapa</th>
              <th style={{ padding: '6px 4px' }}>Tempo</th>
              <th style={{ padding: '6px 4px' }}>Tempo impedido</th>
            </tr>
          </thead>
          <tbody>
            {registros.map((r) => (
              <tr key={r.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                <td style={{ padding: '6px 4px' }}>
                  {r.etapaNome} {r.saidaEm === null && '(atual)'}
                </td>
                <td style={{ padding: '6px 4px' }}>{formatarDuracao(duracaoEntre(r.entradaEm, r.saidaEm))}</td>
                <td style={{ padding: '6px 4px', color: r.tempoImpedimentoSegundos > 0 ? 'var(--color-error)' : undefined }}>
                  {formatarDuracao(r.tempoImpedimentoSegundos)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2 style={{ font: 'var(--font-heading-2)', marginBottom: 'var(--spacing-sm)' }}>
        Observadores (RF-005)
      </h2>
      <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 'var(--spacing-xs)', marginBottom: 'var(--spacing-xl)' }}>
        {usuarios.map((u) => {
          const ehObservador = observadores.includes(u.id);
          return (
            <li key={u.id} style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)' }}>
              <input
                type="checkbox"
                checked={ehObservador}
                onChange={() => alternarObservador(u.id, ehObservador)}
                id={`obs-${u.id}`}
              />
              <label htmlFor={`obs-${u.id}`} style={{ font: 'var(--font-body)' }}>
                {u.nome}
              </label>
            </li>
          );
        })}
      </ul>

      <h2 style={{ font: 'var(--font-heading-2)', marginBottom: 'var(--spacing-sm)' }}>
        Histórico (RF-017)
      </h2>
      {historico === null ? (
        <Skeleton altura={80} />
      ) : historico.length === 0 ? (
        <p style={{ color: 'var(--color-text-secondary)', font: 'var(--font-body)' }}>Sem alterações registradas.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', font: 'var(--font-body)' }}>
          <thead>
            <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--color-border)' }}>
              <th style={{ padding: '6px 4px' }}>Quando</th>
              <th style={{ padding: '6px 4px' }}>Quem</th>
              <th style={{ padding: '6px 4px' }}>Campo</th>
              <th style={{ padding: '6px 4px' }}>De</th>
              <th style={{ padding: '6px 4px' }}>Para</th>
            </tr>
          </thead>
          <tbody>
            {historico.map((h, i) => (
              <tr key={i} style={{ borderBottom: '1px solid var(--color-border)' }}>
                <td style={{ padding: '6px 4px' }}>{new Date(h.criadoEm).toLocaleString('pt-BR')}</td>
                <td style={{ padding: '6px 4px' }}>{h.usuarioNome}</td>
                <td style={{ padding: '6px 4px' }}>{ROTULO_CAMPO[h.campo]}</td>
                <td style={{ padding: '6px 4px', color: 'var(--color-text-secondary)' }}>{h.valorAnterior ?? '—'}</td>
                <td style={{ padding: '6px 4px' }}>{h.valorNovo ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {erro && <ModalErro mensagem={erro} onFechar={() => setErro(null)} />}
    </div>
  );
}
