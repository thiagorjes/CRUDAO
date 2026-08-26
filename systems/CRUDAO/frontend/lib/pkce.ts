/**
 * Utilitários de CSRF/PKCE do fluxo OIDC Authorization Code, via Web Crypto — compatíveis tanto
 * com o runtime Node.js (Route Handlers) quanto com o runtime Edge (middleware).
 */

function base64UrlEncode(bytes: Uint8Array): string {
  let binario = "";
  for (const b of bytes) binario += String.fromCharCode(b);
  return btoa(binario).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export function gerarValorAleatorio(tamanhoBytes = 32): string {
  const bytes = new Uint8Array(tamanhoBytes);
  crypto.getRandomValues(bytes);
  return base64UrlEncode(bytes);
}

export async function desafioPkce(codeVerifier: string): Promise<string> {
  const dados = new TextEncoder().encode(codeVerifier);
  const digest = await crypto.subtle.digest("SHA-256", dados);
  return base64UrlEncode(new Uint8Array(digest));
}
