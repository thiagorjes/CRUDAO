/**
 * Valida que `returnTo` é um path relativo interno — nunca uma URL absoluta — antes de usá-lo
 * como destino de redirect pós-login. Sem essa validação, um `returnTo` como
 * `https://evil.example` sobreviveria a `new URL(returnTo, base)` (finding 🔴 do code review da
 * TASK-05.0): o usuário seria redirecionado para fora do domínio logo após autenticar de verdade
 * no Keycloak — vetor clássico de phishing, já que a vítima acabou de confiar no fluxo de login.
 */
export function caminhoRelativoSeguro(returnTo: string | null | undefined): string {
  if (!returnTo) {
    return '/';
  }
  // Deve começar com uma única "/" — "//evil.com" e "/\evil.com" são tratados por alguns
  // browsers como protocol-relative URL (equivalente a um domínio externo).
  const ehPathRelativoSeguro = /^\/(?!\/|\\)/.test(returnTo);
  return ehPathRelativoSeguro ? returnTo : '/';
}
