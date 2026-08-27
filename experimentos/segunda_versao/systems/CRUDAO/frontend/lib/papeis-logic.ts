import { PapelResponse } from "./papeis";

/**
 * Lógica pura do admin de papéis/permissões/usuários (RF-013/015/016, TASK-07.5) — extraída para
 * ser testável sem DOM, mesmo padrão de `admin-logic.ts`.
 */

/** Mensagens de erro claras por status HTTP, específicas desta tela (critério de aceite da task). */
export function mensagemErroPapeis(status: number, padrao: string): string {
  if (status === 403) {
    return `${padrao} Você não pode alterar a permissão de um papel que você mesmo possui neste projeto (RN-017) — peça para outro administrador executar a ação, ou você não tem "papel:administrar" aqui.`;
  }
  if (status === 409) {
    return `${padrao} Há usuários vinculados a este papel, ou a chave já está em uso no projeto.`;
  }
  if (status === 422) {
    return `${padrao} Verifique os dados informados (ex.: papel protegido não pode ser associado).`;
  }
  return padrao;
}

/** Papéis não protegidos e escopados ao projeto — únicos elegíveis para associação (RN-006). */
export function papeisAssociaveis(papeis: PapelResponse[]): PapelResponse[] {
  return papeis.filter((p) => !p.protegido);
}

/**
 * `true` se o usuário autenticado possui o papel — usado para desabilitar toggles com a mensagem
 * de RN-017 antes mesmo do backend responder 403 (a validação real é sempre do backend).
 */
export function autorPossuiPapel(usuarios: UsuarioProjetoLike[], usuarioIdAutor: string | null, papelChave: string): boolean {
  if (!usuarioIdAutor) return false;
  const autor = usuarios.find((u) => u.usuarioId === usuarioIdAutor);
  return autor ? autor.papeis.includes(papelChave) : false;
}

type UsuarioProjetoLike = { usuarioId: string; papeis: string[] };

export function ordenarPapeisPorNome(papeis: PapelResponse[]): PapelResponse[] {
  return [...papeis].sort((a, b) => a.nome.localeCompare(b.nome));
}

export function ordenarUsuariosPorNome<T extends { nome: string }>(usuarios: T[]): T[] {
  return [...usuarios].sort((a, b) => a.nome.localeCompare(b.nome));
}
