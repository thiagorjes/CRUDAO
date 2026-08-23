import { describe, expect, it } from 'vitest';
import { caminhoRelativoSeguro } from './return-to';

describe('caminhoRelativoSeguro', () => {
  it('aceita um path relativo simples', () => {
    expect(caminhoRelativoSeguro('/board')).toBe('/board');
  });

  it('aceita um path relativo com query string', () => {
    expect(caminhoRelativoSeguro('/board?projeto=abc')).toBe('/board?projeto=abc');
  });

  it('retorna "/" quando returnTo é nulo ou vazio', () => {
    expect(caminhoRelativoSeguro(null)).toBe('/');
    expect(caminhoRelativoSeguro(undefined)).toBe('/');
    expect(caminhoRelativoSeguro('')).toBe('/');
  });

  it('rejeita URL absoluta com esquema (open redirect)', () => {
    expect(caminhoRelativoSeguro('https://evil.example')).toBe('/');
    expect(caminhoRelativoSeguro('http://evil.example/phish')).toBe('/');
  });

  it('rejeita protocol-relative URL ("//host")', () => {
    expect(caminhoRelativoSeguro('//evil.example')).toBe('/');
  });

  it('rejeita path com barra invertida (interpretada como protocol-relative por alguns browsers)', () => {
    expect(caminhoRelativoSeguro('/\\evil.example')).toBe('/');
  });

  it('rejeita path sem barra inicial', () => {
    expect(caminhoRelativoSeguro('evil.example')).toBe('/');
  });
});
