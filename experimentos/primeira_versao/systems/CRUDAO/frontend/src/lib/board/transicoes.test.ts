import { describe, expect, it } from 'vitest';
import { Etapa, Transicao } from '@/lib/api/types';
import { acoesDoMenu, etapasAlvoValidas, transicaoPermitida } from './transicoes';

const etapas: Etapa[] = [
  { id: 'e1', workflowId: 'w1', nome: 'Backlog', ordem: 1, etapaFinal: false },
  { id: 'e2', workflowId: 'w1', nome: 'Em Andamento', ordem: 2, etapaFinal: false },
  { id: 'e3', workflowId: 'w1', nome: 'Em Revisão', ordem: 3, etapaFinal: false },
  { id: 'e4', workflowId: 'w1', nome: 'Concluído', ordem: 4, etapaFinal: true },
];

const transicoes: Transicao[] = [
  { id: 't1', etapaOrigemId: 'e1', etapaDestinoId: 'e2', tipo: 'NORMAL' },
  { id: 't2', etapaOrigemId: 'e2', etapaDestinoId: 'e3', tipo: 'NORMAL' },
  { id: 't3', etapaOrigemId: 'e2', etapaDestinoId: 'e1', tipo: 'NORMAL' },
  { id: 't4', etapaOrigemId: 'e3', etapaDestinoId: 'e4', tipo: 'NORMAL' },
  { id: 't5', etapaOrigemId: 'e4', etapaDestinoId: 'e3', tipo: 'REABERTURA' },
];

describe('etapasAlvoValidas', () => {
  it('retorna somente os destinos alcançáveis pela etapa atual', () => {
    expect(etapasAlvoValidas('e2', transicoes)).toEqual(new Set(['e3', 'e1']));
  });

  it('retorna vazio quando a etapa não tem transição de saída', () => {
    expect(etapasAlvoValidas('inexistente', transicoes)).toEqual(new Set());
  });
});

describe('transicaoPermitida', () => {
  it('permite quando existe transição', () => {
    expect(transicaoPermitida('e1', 'e2', transicoes)).toBe(true);
  });

  it('nega quando não existe transição', () => {
    expect(transicaoPermitida('e1', 'e4', transicoes)).toBe(false);
  });
});

describe('acoesDoMenu', () => {
  it('rotula como avancar quando o destino tem ordem maior', () => {
    const acoes = acoesDoMenu('e1', transicoes, etapas);
    expect(acoes).toEqual([{ transicaoId: 't1', etapaDestinoId: 'e2', etapaDestinoNome: 'Em Andamento', rotulo: 'avancar' }]);
  });

  it('rotula como retroceder quando o destino tem ordem menor', () => {
    const acoes = acoesDoMenu('e2', transicoes, etapas);
    expect(acoes).toContainEqual({
      transicaoId: 't3',
      etapaDestinoId: 'e1',
      etapaDestinoNome: 'Backlog',
      rotulo: 'retroceder',
    });
    expect(acoes).toContainEqual({
      transicaoId: 't2',
      etapaDestinoId: 'e3',
      etapaDestinoNome: 'Em Revisão',
      rotulo: 'avancar',
    });
  });

  it('rotula transição tipo REABERTURA como reabertura, mesmo que o destino tenha ordem menor', () => {
    const acoes = acoesDoMenu('e4', transicoes, etapas);
    expect(acoes).toEqual([
      { transicaoId: 't5', etapaDestinoId: 'e3', etapaDestinoNome: 'Em Revisão', rotulo: 'reabertura' },
    ]);
  });

  it('retorna lista vazia para etapa sem transição de saída', () => {
    expect(acoesDoMenu('inexistente', transicoes, etapas)).toEqual([]);
  });
});
