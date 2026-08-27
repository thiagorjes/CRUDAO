import { describe, expect, it } from 'vitest';
import { EventoBoard, Tarefa } from '@/lib/api/types';
import { aplicarTarefaExcluida } from './eventos';

function tarefa(id: string): Tarefa {
  return {
    id,
    projetoId: 'proj-1',
    workflowId: 'wf-1',
    etapaAtualId: 'etapa-1',
    raiaId: null,
    tipo: 'FEATURE',
    titulo: `Tarefa ${id}`,
    descricao: null,
    responsavelId: null,
    impedida: false,
    iniciada: false,
    criadoEm: '2026-08-24T00:00:00Z',
  };
}

function evento(overrides: Partial<EventoBoard> = {}): EventoBoard {
  return {
    tipo: 'TAREFA_EXCLUIDA',
    tarefaId: 't1',
    projetoId: 'proj-1',
    etapaAtualId: 'etapa-1',
    impedida: false,
    observadorIds: [],
    ...overrides,
  };
}

describe('aplicarTarefaExcluida', () => {
  it('remove a tarefa correta do array por id', () => {
    const atual = { projetoId: 'proj-1', tarefas: [tarefa('t1'), tarefa('t2')] };
    const resultado = aplicarTarefaExcluida(atual, evento({ tarefaId: 't1' }));
    expect(resultado?.tarefas.map((t) => t.id)).toEqual(['t2']);
  });

  it('não remove nada quando o evento é de outro projeto', () => {
    const atual = { projetoId: 'proj-1', tarefas: [tarefa('t1')] };
    const resultado = aplicarTarefaExcluida(atual, evento({ projetoId: 'proj-2' }));
    expect(resultado).toBe(atual);
  });

  it('é no-op seguro se a tarefa já tiver sido removida localmente antes', () => {
    const atual = { projetoId: 'proj-1', tarefas: [tarefa('t2')] };
    const resultado = aplicarTarefaExcluida(atual, evento({ tarefaId: 't1' }));
    expect(resultado?.tarefas.map((t) => t.id)).toEqual(['t2']);
  });
});
