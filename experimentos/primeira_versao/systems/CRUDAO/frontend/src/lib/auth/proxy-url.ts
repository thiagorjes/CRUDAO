/** Monta a URL de destino no backend a partir do path capturado por `/api/proxy/[...path]`. */
export function montarUrlBackend(backendUrl: string, path: string[], search: string): string {
  const caminho = path.map(encodeURIComponent).join('/');
  const query = search ? `?${search}` : '';
  return `${backendUrl.replace(/\/$/, '')}/api/${caminho}${query}`;
}
