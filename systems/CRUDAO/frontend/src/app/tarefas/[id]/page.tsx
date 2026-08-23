'use client';

import { use, useEffect, useState } from 'react';
import Link from 'next/link';
import { api, ApiError } from '@/lib/api/client';
import { Etapa, RegistroEtapa, Tarefa, Usuario } from '@/lib/api/types';
import { duracaoEntre, formatarDuracao } from '@/lib/board/tempo';
import { ModalErro } from '@/components/ui/ModalErro';
import { Skeleton } from '@/components/ui/Skeleton';
import { mostrarToast } from '@/components/ui/toast';

export default function TarefaDetalhePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [tarefa, setTarefa] = useState<Tarefa | null>(null);
  const [etapa, setEtapa] = useState<Etapa | null>(null);
  const [registros, setRegistros] = useState<RegistroEtapa[] | null>(null);
  const [observadores, setObservadores] = useState<string[]>([]);
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    carregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function carregar() {
    try {
      const [t, regs, obs, usrs] = await Promise.all([
        api.get<Tarefa>(`/tarefas/${id}`),
        api.get<RegistroEtapa[]>(`/tarefas/${id}/registros-etapa`),
        api.get<string[]>(`/tarefas/${id}/observadores`),
        api.get<Usuario[]>('/usuarios'),
      ]);
      setTarefa(t);
      setRegistros(regs);
      setObservadores(obs);
      setUsuarios(usrs);
      // Sem GET /etapas/{id} no backend — lista por workflow e filtra localmente.
      const etapasDoWorkflow = await api
        .get<Etapa[]>(`/etapas?workflowId=${t.workflowId}`)
        .catch(() => []);
      setEtapa(etapasDoWorkflow.find((e) => e.id === t.etapaAtualId) ?? null);
    } catch {
      setErro('Não foi possível carregar a tarefa.');
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
      carregar();
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
      </div>

      <h1 style={{ font: 'var(--font-heading-1)', marginBottom: 'var(--spacing-sm)' }}>{tarefa.titulo}</h1>
      {tarefa.descricao && (
        <p style={{ font: 'var(--font-body)', color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-md)' }}>
          {tarefa.descricao}
        </p>
      )}

      <dl style={{ font: 'var(--font-body)', marginBottom: 'var(--spacing-lg)' }}>
        <div style={{ display: 'flex', gap: 'var(--spacing-sm)' }}>
          <dt style={{ color: 'var(--color-text-secondary)' }}>Etapa atual:</dt>
          <dd>{etapa?.nome ?? '—'}</dd>
        </div>
        <div style={{ display: 'flex', gap: 'var(--spacing-sm)' }}>
          <dt style={{ color: 'var(--color-text-secondary)' }}>Responsável:</dt>
          <dd>{responsavel?.nome ?? 'Sem responsável'}</dd>
        </div>
      </dl>

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
      <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 'var(--spacing-xs)' }}>
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

      {erro && <ModalErro mensagem={erro} onFechar={() => setErro(null)} />}
    </div>
  );
}
