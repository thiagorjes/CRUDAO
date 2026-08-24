import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { duracaoEntre, formatarDuracao } from './tempo';

describe('formatarDuracao', () => {
  it('formata menos de um minuto como 0m', () => {
    expect(formatarDuracao(30)).toBe('0m');
  });

  it('formata minutos', () => {
    expect(formatarDuracao(150)).toBe('2m');
  });

  it('formata horas e minutos', () => {
    expect(formatarDuracao(3 * 3600 + 20 * 60)).toBe('3h 20m');
  });

  it('formata dias e horas', () => {
    expect(formatarDuracao(2 * 86400 + 4 * 3600)).toBe('2d 4h');
  });
});

describe('duracaoEntre', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-22T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('calcula a diferença entre início e fim informados', () => {
    expect(duracaoEntre('2026-08-22T10:00:00Z', '2026-08-22T11:00:00Z')).toBe(3600);
  });

  it('usa "agora" quando fim é nulo (permanência em andamento)', () => {
    expect(duracaoEntre('2026-08-22T11:00:00Z', null)).toBe(3600);
  });
});
