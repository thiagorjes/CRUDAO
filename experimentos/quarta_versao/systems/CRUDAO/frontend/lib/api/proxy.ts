/**
 * Proxy autenticado do BFF para o backend, usado pelos route handlers de `/app/api/**`.
 *
 * Reexporta a implementação canônica de `lib/api.ts` (lê o cookie httpOnly cifrado `kanban_session`,
 * decifra server-side e injeta `Authorization: Bearer <accessToken>` — o token nunca chega ao JS).
 * Mantido como módulo próprio por compatibilidade com os imports `@/lib/api/proxy` já existentes.
 */
export { apiProxyFetch } from "../api";
