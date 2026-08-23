import { describe, expect, it, vi, afterEach } from 'vitest';
import { getBackendHealth } from './page';

describe('getBackendHealth', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('retorna os dados de saúde quando o backend responde 200', async () => {
    const payload = { status: 'ok', service: 'kanban-backend', timestamp: '2026-08-22T00:00:00Z' };
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(payload),
      }),
    );

    const result = await getBackendHealth();

    expect(result).toEqual(payload);
  });

  it('retorna um erro quando o backend está indisponível', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('connection refused')));

    const result = await getBackendHealth();

    expect(result).toEqual({ error: 'Backend indisponível' });
  });

  it('retorna um erro quando o backend responde com status de erro', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 500 }));

    const result = await getBackendHealth();

    expect(result).toEqual({ error: 'Backend respondeu com status 500' });
  });
});
