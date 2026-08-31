/** Tipos compartilhados entre Client e Server Components */

export type MeResponse = {
  id: string;
  nome: string;
  email: string;
  adminGlobal: boolean;
  projetos: { projetoId: string; papeis: string[] }[];
};

// Board types (espelha contrato de GET /api/projetos/{projetoId}/board)
export type BoardEtapa = {
  id: string;
  nome: string;
  ordem: number;
  transicoesSaida: string[]; // IDs das etapas possíveis
};

export type BoardRaia = {
  id: string;
  nome: string;
  ordem: number;
  global: boolean;
};

export type BoardTarefa = {
  id: string;
  titulo: string;
  etapaAtualId: string;
  raiaId: string;
  responsavelId?: string;
  impedida: boolean;
  impedidaDesde?: string;
  iniciada: boolean;
};

export type BoardResponse = {
  etapas: BoardEtapa[];
  raias: BoardRaia[];
  tarefas: BoardTarefa[];
};

// Eventos STOMP
export type EventoBoardMessage = {
  seq: number;
  tipo: "TAREFA_CRIADA" | "TAREFA_MOVIDA" | "TAREFA_EXCLUIDA";
  tarefa?: BoardTarefa;
  tarefaId?: string;
  timestamp: string;
};

// Erro de API
export type ApiError = {
  error?: string;
  message?: string;
  status?: number;
};
