/** Tipos espelhando os DTOs do backend (systems/CRUDAO/backend/.../domain/*). */

export type Projeto = {
  id: string;
  nome: string;
  descricao: string | null;
  workflowAtivoId: string | null;
  criadoEm: string;
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
};

export type Usuario = {
  id: string;
  nome: string;
  email: string;
};

export type RegistroEtapa = {
  id: string;
  etapaId: string;
  etapaNome: string;
  entradaEm: string;
  saidaEm: string | null;
  tempoImpedimentoSegundos: number;
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
