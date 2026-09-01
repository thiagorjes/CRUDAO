/** Tipos compartilhados entre Client e Server Components */

export type MeResponse = {
  id: string;
  nome: string;
  email: string;
  adminGlobal: boolean;
  projetos: { projetoId: string; papeis: string[] }[];
};

/** Espelha ProjetoController.Response de GET /api/projetos */
export type ProjetoResumo = {
  id: string;
  nome: string;
  descricao: string | null;
  status: "ATIVO" | "FINALIZADO";
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

// Detalhe da tarefa — espelha TarefaDetalheResponse (backend) 1:1.
export type LeadTimeEtapa = {
  etapaId: string;
  etapaNome: string;
  leadTimeSegundos: number;
};

// Espelha TarefaAuditoriaResponse (backend)
export type AuditoriaEntry = {
  id: string;
  campo: string;
  valorAnterior: string | null;
  valorNovo: string | null;
  dataHora: string;
  autorId: string;
  autorNome: string;
};

export type TarefaDetalhe = {
  id: string;
  titulo: string;
  descricaoEscopo?: string;
  etapaAtualId: string;
  etapaAtualNome: string;
  raiaId: string;
  raiaNome: string;
  responsavelId?: string;
  responsavelNome?: string;
  iniciada: boolean;
  impedida: boolean;
  impedidaDesde?: string;
  criadoEm: string;
  criadoPorId: string;
  criadoPorNome: string;
  historicoEtapas: LeadTimeEtapa[];
  tempoImpedimentoTotalSegundos: number;
  observadores: { id: string; nome: string }[];
};

export type EditarTarefaRequest = {
  titulo: string;
  descricaoEscopo?: string;
  responsavelId?: string;
};

// Erro de API
export type ApiError = {
  error?: string;
  message?: string;
  status?: number;
};

// Admin types — espelham os DTOs reais do backend (workflow/dto/*, raia/dto/RaiaResponse).
export type Etapa = {
  id: string;
  nome: string;
  ordem: number;
  etapaFinal: boolean;
  transicoesSaida: string[]; // IDs de etapas destino
};

export type Workflow = {
  id: string;
  nome: string;
  etapas: Etapa[];
};

export type Raia = {
  id: string;
  nome: string;
  ordem: number;
  global: boolean;
};

export type Papel = {
  id: string;
  nome: string;
  projetoId?: string;
  protegido: boolean;
};

/** Espelha ProjetoController.UsuarioProjetoResponse (GET /api/projetos/{id}/usuarios). */
export type UsuarioProjetoPapel = {
  usuarioId: string;
  usuarioNome: string;
  papelId: string;
  papelNome: string;
};

export type EtapaLeadTime = {
  etapaId: string;
  etapaNome: string;
  leadTimeMedioSegundos: number;
  tempoImpedimentoMedioSegundos: number;
};

export type Dashboard = {
  leadTimeMedioPorEtapa: EtapaLeadTime[];
  totalTarefasConsideradas: number;
};

// Notificações (RF-005) — espelha GET /api/notificacoes (NotificacaoController.NotificacaoResponse)
export type Notificacao = {
  id: string;
  tarefaId: string;
  tarefaTitulo: string;
  tipo: string;
  lida: boolean;
  criadoEm: string;
};
