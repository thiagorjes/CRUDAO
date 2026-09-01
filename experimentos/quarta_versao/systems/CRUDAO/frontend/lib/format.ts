/** Utilitários de formatação puros — sem dependência de servidor, seguros para Client Components. */
export function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/);
  const primeira = partes[0]?.[0] ?? "";
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : "";
  return (primeira + ultima).toUpperCase();
}

/** Formata segundos em "Xd Yh" / "Xh Ym" / "Xm Ys" / "Xs", igual ao padrão do TL-04/TL-07. */
export function formatarDuracao(segundosTotais: number): string {
  const s = Math.max(0, Math.floor(segundosTotais));
  if (s === 0) return "0s";
  const segundos = s % 60;
  const minutos = Math.floor(s / 60) % 60;
  const horas = Math.floor(s / 3600) % 24;
  const dias = Math.floor(s / 86400);

  if (dias > 0) return `${dias}d ${horas}h`;
  if (horas > 0) return `${horas}h ${minutos}m`;
  if (minutos > 0) return `${minutos}m ${segundos}s`;
  return `${segundos}s`;
}
