import { apiFetch, apiFetchJson } from "@/lib/api";
import { BoardResponse, MembroProjeto } from "@/lib/board";
import { TarefaAuditoriaResponse, TarefaDetalheResponse } from "@/lib/tarefa";
import { TarefaDetalheClient } from "@/components/tarefa/TarefaDetalheClient";

/** TL-04 — Detalhe da Tarefa (RF-003, RF-006, RF-017). */
export default async function TarefaDetalhePage({
  params,
}: {
  params: Promise<{ id: string; tarefaId: string }>;
}) {
  const { id: projetoId, tarefaId } = await params;

  const [tarefa, board, membros, observadores, auditoria] = await Promise.all([
    apiFetchJson<TarefaDetalheResponse>(`/api/tarefas/${tarefaId}`),
    apiFetchJson<BoardResponse>(`/api/projetos/${projetoId}/board`),
    apiFetchJson<MembroProjeto[]>(`/api/projetos/${projetoId}/usuarios`),
    apiFetchJson<string[]>(`/api/tarefas/${tarefaId}/observadores`),
    buscarAuditoria(tarefaId),
  ]);

  return (
    <TarefaDetalheClient
      projetoId={projetoId}
      tarefa={tarefa}
      etapas={board.etapas}
      membros={membros}
      observadoresIniciais={observadores}
      auditoria={auditoria}
    />
  );
}

/**
 * `GET /api/tarefas/{id}/auditoria` exige papel gestor ou admin (RF-017, `tarefa:auditoria`) — 403
 * é esperado para devs sem essa permissão, não um erro: a seção de histórico fica oculta.
 */
async function buscarAuditoria(tarefaId: string): Promise<TarefaAuditoriaResponse[] | null> {
  const res = await apiFetch(`/api/tarefas/${tarefaId}/auditoria`);
  if (res.status === 403) return null;
  if (!res.ok) {
    throw new Error(`Chamada a /api/tarefas/${tarefaId}/auditoria falhou com status ${res.status}`);
  }
  return res.json();
}
