import { describe, expect, it } from 'vitest';
import { Raia, Tarefa } from '@/lib/api/types';
import { agruparPorRaiaEEtapa, RAIA_SEM_RAIA_ID } from './agrupar';

const raias: Raia[] = [{ id: 'r1', projetoId: 'p1', nome: 'Ana', ordem: 1 }];

function tarefa(parcial: Partial<Tarefa>): Tarefa {
  return {
    id: 't1',
    projetoId: 'p1',
    workflowId: 'w1',
    etapaAtualId: 'e1',
    raiaId: null,
    tipo: 'FEATURE',
    titulo: 'Título',
    descricao: null,
    responsavelId: null,
    impedida: false,
    ...parcial,
  };
}

describe('agruparPorRaiaEEtapa', () => {
  it('agrupa tarefas pela raia e etapa corretas', () => {
    const grade = agruparPorRaiaEEtapa(
      [tarefa({ id: 't1', raiaId: 'r1', etapaAtualId: 'e1' })],
      raias,
    );
    expect(grade.get('r1')?.get('e1')).toEqual([tarefa({ id: 't1', raiaId: 'r1', etapaAtualId: 'e1' })]);
  });

  it('agrupa tarefa sem raiaId no grupo "sem raia"', () => {
    const grade = agruparPorRaiaEEtapa([tarefa({ id: 't1', raiaId: null })], raias);
    expect(grade.get(RAIA_SEM_RAIA_ID)?.get('e1')).toHaveLength(1);
  });

  it('agrupa tarefa com raiaId que não existe mais na lista de raias no grupo "sem raia"', () => {
    const grade = agruparPorRaiaEEtapa([tarefa({ id: 't1', raiaId: 'raia-excluida' })], raias);
    expect(grade.get(RAIA_SEM_RAIA_ID)?.get('e1')).toHaveLength(1);
    expect(grade.get('raia-excluida')).toBeUndefined();
  });

  it('mantém múltiplas tarefas na mesma célula', () => {
    const grade = agruparPorRaiaEEtapa(
      [
        tarefa({ id: 't1', raiaId: 'r1', etapaAtualId: 'e1' }),
        tarefa({ id: 't2', raiaId: 'r1', etapaAtualId: 'e1' }),
      ],
      raias,
    );
    expect(grade.get('r1')?.get('e1')).toHaveLength(2);
  });
});
