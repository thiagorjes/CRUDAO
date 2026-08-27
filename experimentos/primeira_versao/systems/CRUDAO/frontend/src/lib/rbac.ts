/** Gating de UI a partir de `GET /api/usuarios/me` (RNF-003/ADR-006 — backend é a fonte real de autorização). */

import { UsuarioMe } from '@/lib/api/types';

/** Permissões efetivas do usuário autenticado no projeto informado (Set vazio se sem vínculo). */
export function permissoesDoProjeto(usuarioMe: UsuarioMe | null, projetoId: string | null): Set<string> {
  if (!usuarioMe || !projetoId) return new Set<string>();
  if (usuarioMe.admin) return new Set(['tarefa:gerenciar', 'tarefa:atribuir', 'tarefa:finalizar']);
  const vinculo = usuarioMe.projetos.find((p) => p.projetoId === projetoId);
  return new Set(vinculo?.permissoes ?? []);
}

/**
 * "dev-tier": tem `tarefa:gerenciar` mas não `tarefa:atribuir` — único papel seedado com essa
 * combinação, mesma heurística do backend (`TarefaService.ehDevTier`, ver nota técnica da TASK-02.3).
 */
export function ehDevTier(permissoesProjeto: Set<string>): boolean {
  return permissoesProjeto.has('tarefa:gerenciar') && !permissoesProjeto.has('tarefa:atribuir');
}
