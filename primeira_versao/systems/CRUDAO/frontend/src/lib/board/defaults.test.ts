import { describe, expect, it } from 'vitest';
import { resolverDefaults } from './defaults';
import { Etapa, Raia } from '@/lib/api/types';

function etapa(id: string, ordem: number): Etapa {
  return { id, workflowId: 'wf-1', nome: id, ordem, etapaFinal: false };
}

function raia(id: string, ordem: number): Raia {
  return { id, projetoId: 'p-1', nome: id, ordem };
}

describe('resolverDefaults', () => {
  it('resolve a etapa e a raia de menor ordem', () => {
    const defaults = resolverDefaults(
      [etapa('e-2', 2), etapa('e-1', 1), etapa('e-3', 3)],
      [raia('r-2', 2), raia('r-1', 1)],
    );
    expect(defaults.etapaInicialId).toBe('e-1');
    expect(defaults.raiaId).toBe('r-1');
  });

  it('retorna raiaId null quando o projeto não tem raia própria', () => {
    const defaults = resolverDefaults([etapa('e-1', 1)], []);
    expect(defaults.etapaInicialId).toBe('e-1');
    expect(defaults.raiaId).toBeNull();
  });

  it('retorna etapaInicialId null quando o workflow não tem etapas', () => {
    const defaults = resolverDefaults([], [raia('r-1', 1)]);
    expect(defaults.etapaInicialId).toBeNull();
    expect(defaults.raiaId).toBe('r-1');
  });
});
