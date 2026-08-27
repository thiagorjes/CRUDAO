import { describe, expect, it } from 'vitest';
import { montarUrlBackend } from './proxy-url';

describe('montarUrlBackend', () => {
  it('monta a URL com o path e sem query string quando não há search', () => {
    expect(montarUrlBackend('http://backend:8080', ['tarefas'], '')).toBe(
      'http://backend:8080/api/tarefas',
    );
  });

  it('preserva múltiplos segmentos de path', () => {
    expect(
      montarUrlBackend('http://backend:8080', ['tarefas', 'abc-123', 'mover'], ''),
    ).toBe('http://backend:8080/api/tarefas/abc-123/mover');
  });

  it('inclui a query string quando presente', () => {
    expect(montarUrlBackend('http://backend:8080', ['tarefas'], 'projetoId=xyz')).toBe(
      'http://backend:8080/api/tarefas?projetoId=xyz',
    );
  });

  it('remove barra final duplicada do backendUrl', () => {
    expect(montarUrlBackend('http://backend:8080/', ['tarefas'], '')).toBe(
      'http://backend:8080/api/tarefas',
    );
  });
});
