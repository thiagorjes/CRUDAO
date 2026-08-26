/** Variáveis de ambiente do BFF OIDC (frontend como client OIDC próprio — ver TASK-07.1). */
function obrigatoria(nome: string, valor: string | undefined): string {
  if (!valor) {
    throw new Error(`Variável de ambiente ${nome} não configurada.`);
  }
  return valor;
}

export const env = {
  keycloakIssuer: () => obrigatoria("KEYCLOAK_ISSUER", process.env.KEYCLOAK_ISSUER),
  /**
   * Issuer do Keycloak alcançável pelo BROWSER (redirect da tela de login) — em Docker, o
   * container do frontend fala com o Keycloak via nome de serviço interno (`KEYCLOAK_ISSUER`),
   * mas o browser do usuário só alcança a porta publicada no host (TASK-08.3, ADR-008). Fora de
   * Docker (dev local), os dois valores coincidem — cai no fallback.
   */
  keycloakIssuerPublic: () => process.env.KEYCLOAK_ISSUER_PUBLIC || env.keycloakIssuer(),
  keycloakClientId: () => obrigatoria("KEYCLOAK_CLIENT_ID", process.env.KEYCLOAK_CLIENT_ID),
  keycloakClientSecret: () =>
    obrigatoria("KEYCLOAK_CLIENT_SECRET", process.env.KEYCLOAK_CLIENT_SECRET),
  backendUrl: () => obrigatoria("BACKEND_URL", process.env.BACKEND_URL),
  /**
   * URL do backend alcançável diretamente pelo browser — usada só para a conexão STOMP/SockJS do
   * board (TASK-07.2), que precisa ser direta (browser→Spring) por causa do ticket de curta
   * duração (ver `websocket/WsTicketAuthenticationFilter` no backend). Pode divergir de
   * `BACKEND_URL` em produção (URL interna de cluster vs. URL pública).
   */
  publicBackendUrl: () =>
    obrigatoria("NEXT_PUBLIC_BACKEND_URL", process.env.NEXT_PUBLIC_BACKEND_URL),
  appUrl: () => process.env.APP_URL || "http://localhost:3000",
  sessionSecret: () => obrigatoria("SESSION_SECRET", process.env.SESSION_SECRET),
};
