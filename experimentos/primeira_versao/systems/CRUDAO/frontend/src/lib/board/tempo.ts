/** Formata uma duração em segundos como "Xd Yh" / "Xh Ym" / "Xm" (RF-006, cards e detalhe da tarefa). */
export function formatarDuracao(segundos: number): string {
  if (segundos < 60) {
    return '0m';
  }
  const dias = Math.floor(segundos / 86400);
  const horas = Math.floor((segundos % 86400) / 3600);
  const minutos = Math.floor((segundos % 3600) / 60);

  if (dias > 0) {
    return `${dias}d ${horas}h`;
  }
  if (horas > 0) {
    return `${horas}h ${minutos}m`;
  }
  return `${minutos}m`;
}

/** Tempo decorrido entre duas datas ISO (ou até agora, se `fim` for nulo — permanência em andamento). */
export function duracaoEntre(inicioIso: string, fimIso: string | null): number {
  const inicio = new Date(inicioIso).getTime();
  const fim = fimIso ? new Date(fimIso).getTime() : Date.now();
  return Math.max(0, Math.round((fim - inicio) / 1000));
}
