/** Tipos espelhando os DTOs do backend (systems/CRUDAO/backend/.../domain/*). */

export type Projeto = {
  id: string;
  nome: string;
  descricao: string | null;
  workflowAtivoId: string | null;
  dataFinalizacao: string | null;
  criadoEm: string;
};

export type ConfiguracaoProjeto = {
  devPodeExcluirTarefa: boolean;
  devPodeEditarTarefaIniciada: boolean;
  gestorVeBoard: boolean;
};

/** Papéis e permissões efetivas do usuário autenticado num projeto — parte de UsuarioMe (RF-015). */
export type ProjetoPapeis = {
  projetoId: string;
  papeis: string[];
  permissoes: string[];
};

/** Perfil do usuário autenticado (`GET /api/usuarios/me`) — gating de UI, nunca fonte de autorização (RNF-003). */
export type UsuarioMe = {
  id: string;
  nome: string;
  admin: boolean;
  projetos: ProjetoPapeis[];
};

/** Membro de um projeto (RF-015) — usuário e os papéis que acumula naquele projeto. */
export type Membro = {
  usuarioId: string;
  nome: string;
  papeis: string[];
};

/** Papel do catálogo RBAC — RF-013. */
export type Papel = {
  id: string;
  nome: string;
  protegido: boolean;
  permissoes: string[];
};

export type Workflow = {
  id: string;
  projetoId: string;
  nome: string;
  versao: number;
};

export type Etapa = {
  id: string;
  workflowId: string;
  nome: string;
  ordem: number;
  etapaFinal: boolean;
};

export type TipoTransicao = 'NORMAL' | 'REABERTURA';

export type Transicao = {
  id: string;
  etapaOrigemId: string;
  etapaDestinoId: string;
  tipo: TipoTransicao;
};

export type Raia = {
  id: string;
  projetoId: string | null;
  nome: string;
  ordem: number;
};

export type TipoTarefa = 'FEATURE' | 'BUG' | 'CHORE';

export type Tarefa = {
  id: string;
  projetoId: string;
  workflowId: string;
  etapaAtualId: string;
  raiaId: string | null;
  tipo: TipoTarefa;
  titulo: string;
  descricao: string | null;
  responsavelId: string | null;
  impedida: boolean;
  /** Marcada na primeira vez que a tarefa sai da etapa inicial do workflow — RN-009/RN-010. */
  iniciada: boolean;
};

export type CampoAuditoria = 'RESPONSAVEL' | 'TITULO' | 'DESCRICAO' | 'ETAPA';

/** Linha do histórico de auditoria da tarefa (`GET /tarefas/{id}/historico`) — RF-017. */
export type AuditoriaTarefa = {
  campo: CampoAuditoria;
  valorAnterior: string | null;
  valorNovo: string | null;
  usuarioId: string;
  usuarioNome: string;
  criadoEm: string;
};

export type Usuario = {
  id: string;
  nome: string;
};

export type RegistroEtapa = {
  id: string;
  etapaId: string;
  etapaNome: string;
  entradaEm: string;
  saidaEm: string | null;
  tempoImpedimentoSegundos: number;
};

export type StatusJobDashboard = 'PROCESSANDO' | 'CONCLUIDO' | 'ERRO';

export type DashboardResultado = {
  jobId: string;
  projetoId: string;
  status: StatusJobDashboard;
  leadTimeMedioPorEtapaSegundos: Record<string, number>;
  tempoMedioImpedimentoPorEtapaSegundos: Record<string, number>;
};

export type TipoEventoBoard = 'TAREFA_CRIADA' | 'TAREFA_MOVIDA' | 'IMPEDIMENTO_ALTERADO';

export type EventoBoard = {
  tipo: TipoEventoBoard;
  tarefaId: string;
  projetoId: string;
  etapaAtualId: string;
  impedida: boolean;
  observadorIds: string[];
};
