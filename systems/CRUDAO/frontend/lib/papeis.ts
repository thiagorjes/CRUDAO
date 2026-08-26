/** Espelha os DTOs de `com.crudao.kanban.papel` (RF-013/015/016, TASK-07.5). */

export type PermissaoToggle = {
  chave: string;
  habilitada: boolean;
};

export type PapelResponse = {
  id: string;
  chave: string;
  nome: string;
  protegido: boolean;
  permissoes: PermissaoToggle[];
};

export type UsuarioProjetoResponse = {
  usuarioId: string;
  nome: string;
  email: string;
  papeis: string[];
};

export type UsuarioResumoResponse = {
  id: string;
  nome: string;
  email: string;
};
