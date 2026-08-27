# Data Model — Kanban de Tarefas

_Versão: 1.0 | Data: 2026-08-25_

> Fonte de verdade do modelo de dados. `kanban-tarefas-techspec.md` Seção 3 apenas resume e referencia este arquivo.
> Migrations: Flyway ([ADR-005](../../decisions/ADR-005-flyway-migrations.md)), `backend/src/main/resources/db/migration/`.

---

## Diagrama ER (lógico)

```
Usuario ──< UsuarioProjetoPapel >── Papel ──< PapelPermissao >── Permissao
   │                                   │(projetoId FK)
   │                                   │
   └──< TarefaObservador               │
   └──< Notificacao                    │
                                        │
Projeto ──< Raia                       │
   │       Projeto ──< Papel (escopo por projeto; "admin" é global/protegido — RN-006)
   ├──< Workflow ──< Etapa ──< Transicao (etapaOrigemId → etapaDestinoId)
   │
   └──< Tarefa (projetoId, workflowId, etapaAtualId, raiaId, responsavelId, criadoPorId)
              ├──< TarefaEtapaHistorico
              ├──< TarefaImpedimentoHistorico
              ├──< TarefaAuditoria
              ├──< TarefaObservador
              └──< Notificacao (tarefaId)
```

---

## Entidades

### Usuario

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID PK | |
| keycloakSub | varchar(255) UNIQUE | `sub` do token OIDC (RF-014) |
| nome | varchar(255) | |
| email | varchar(255) UNIQUE | |
| ativo | boolean | default true |
| adminGlobal | boolean | default false — bootstrap do primeiro admin (ADR-007), bypassa RBAC escopado |
| criadoEm | timestamptz | |

Índices: `UNIQUE(keycloakSub)`, `UNIQUE(email)`.

### Projeto

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID PK | |
| nome | varchar(255) | |
| descricao | text | nullable |
| status | varchar(20) | `ATIVO` \| `FINALIZADO` (RF-008, RN-015) |
| criadoPorId | UUID FK → Usuario | |
| criadoEm | timestamptz | |
| finalizadoEm | timestamptz | nullable |

Índice: `(status)` — listagens filtram por projetos ativos com frequência.

### Papel

Papéis são configuráveis **por projeto** (RF-013), exceto `admin`, que é global e protegido (RN-006).

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID PK | |
| projetoId | UUID FK → Projeto | nullable — `null` = papel global (`admin`) |
| chave | varchar(50) | `admin`, `product_owner`, `project_admin`, `dev`, `gestor` (seed inicial) ou papel custom |
| nome | varchar(255) | label de exibição |
| protegido | boolean | `true` somente para `admin` — bloqueia edição/exclusão (RN-006) |

Índice: `UNIQUE(projetoId, chave)`.

### Permissao

Catálogo fixo de permissões (seed via migration, não editável em runtime):

`tarefa:gerenciar` (criar/excluir card), `tarefa:finalizar` (mover para/reabrir etapa final), `tarefa:impedimento` (marcar/desmarcar impedimento), `projeto:administrar`, `workflow:administrar`, `papel:administrar`, `usuario:associar`.

| Campo | Tipo |
|---|---|
| id | UUID PK |
| chave | varchar(50) UNIQUE |
| descricao | varchar(255) |

### PapelPermissao

Toggle de permissão por papel dentro de um projeto (RF-016).

| Campo | Tipo | Regras |
|---|---|---|
| papelId | UUID FK → Papel | PK composta |
| permissaoId | UUID FK → Permissao | PK composta |
| habilitada | boolean | default conforme seed (ex.: `dev` + `tarefa:impedimento` = true; `dev` + `tarefa:finalizar` = false — RN-011, RN-013) |

Seed de defaults por papel documentado na migration `V{n}__seed_papeis_permissoes.sql`, refletindo RN-011, RN-012, RN-013, RN-CB-001, RN-CB-002.

### UsuarioProjetoPapel

Associação usuário↔projeto↔papel (RF-015).

| Campo | Tipo |
|---|---|
| usuarioId | UUID FK → Usuario (PK composta) |
| projetoId | UUID FK → Projeto (PK composta) |
| papelId | UUID FK → Papel (PK composta) |
| associadoEm | timestamptz |

### Workflow

| Campo | Tipo |
|---|---|
| id | UUID PK |
| projetoId | UUID FK → Projeto |
| nome | varchar(255) |

### Etapa

Coluna do board (RF-010).

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID PK | |
| workflowId | UUID FK → Workflow | |
| nome | varchar(255) | |
| ordem | int | define posição no board |
| etapaFinal | boolean | apenas uma etapa final por workflow (constraint de aplicação); nome escolhido para evitar o problema de boolean com duas maiúsculas descrito em `coding-standards.md` |

Índice: `(workflowId, ordem)`.

### Transicao

Transição permitida entre etapas (RF-002, RN-003).

| Campo | Tipo |
|---|---|
| id | UUID PK |
| etapaOrigemId | UUID FK → Etapa |
| etapaDestinoId | UUID FK → Etapa |

Índice: `UNIQUE(etapaOrigemId, etapaDestinoId)`. Regra RN-003 (toda etapa não-final tem ≥1 transição de saída) validada em nível de serviço na criação/edição de `Etapa`, não em constraint de banco.

### Raia

Swimlane (RF-011).

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID PK | |
| projetoId | UUID FK → Projeto | nullable — `null` = raia default global (RN-CB-005) |
| nome | varchar(255) | |
| ordem | int | |

### Tarefa

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID PK | |
| projetoId | UUID FK → Projeto | |
| workflowId | UUID FK → Workflow | |
| etapaAtualId | UUID FK → Etapa | |
| raiaId | UUID FK → Raia | pode apontar para raia default global (RN-CB-005) |
| titulo | varchar(255) | |
| descricaoEscopo | text | campo estrutural — congelado após início (RF-003) |
| responsavelId | UUID FK → Usuario | nullable |
| criadoPorId | UUID FK → Usuario | |
| iniciada | boolean | `true` quando sai da primeira etapa do workflow (RF-003) — controla congelamento de campos |
| impedida | boolean | default false (RF-004) |
| impedidaDesde | timestamptz | nullable |
| criadoEm | timestamptz | |
| atualizadoEm | timestamptz | |

Índices: `(projetoId, etapaAtualId)` (query do board), `(responsavelId)`.

Campos editáveis pós-início (RF-003): `responsavelId`, `etapaAtualId` (via transição), `impedida`/`impedidaDesde`. `titulo` e `descricaoEscopo` ficam bloqueados via validação de serviço, não via constraint de banco.

### TarefaObservador

Observadores explícitos (RF-005) — responsável e criador são observadores implícitos, não precisam de linha aqui.

| Campo | Tipo |
|---|---|
| tarefaId | UUID FK → Tarefa (PK composta) |
| usuarioId | UUID FK → Usuario (PK composta) |

### TarefaEtapaHistorico

Base para lead-time por etapa (RF-006, RN-001).

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID PK | |
| tarefaId | UUID FK → Tarefa | |
| etapaId | UUID FK → Etapa | |
| entradaEm | timestamptz | |
| saidaEm | timestamptz | nullable — `null` = etapa atual em andamento |

Índices: `(tarefaId, entradaEm)`; `(etapaId, saidaEm)` — suporta a agregação de lead-time médio do dashboard (RF-007) sem seq scan por etapa (achado do Comitê de Análise — Database). Lead-time da etapa = `saidaEm - entradaEm` (ou `now() - entradaEm` se em andamento).

### TarefaImpedimentoHistorico

Base para tempo de impedimento acumulado (RF-006, RN-002).

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID PK | |
| tarefaId | UUID FK → Tarefa | |
| marcadoEm | timestamptz | |
| desmarcadoEm | timestamptz | nullable |

Índice: `(tarefaId, marcadoEm)` — suporta soma de tempo de impedimento por tarefa (RF-006), alinhando com o padrão de `TarefaEtapaHistorico` (achado do Comitê de Análise — Database).

### TarefaAuditoria

Histórico de auditoria (RF-017, RN-016).

| Campo | Tipo |
|---|---|
| id | UUID PK |
| tarefaId | UUID FK → Tarefa |
| autorId | UUID FK → Usuario |
| campo | varchar(50) — `responsavel`, `titulo`, `etapa`, `impedimento` |
| valorAnterior | text | nullable |
| valorNovo | text | nullable |
| dataHora | timestamptz |

Índice: `(tarefaId, dataHora)`.

### Notificacao

Notificação interna (RF-005).

| Campo | Tipo |
|---|---|
| id | UUID PK |
| usuarioId | UUID FK → Usuario |
| tarefaId | UUID FK → Tarefa |
| tipo | varchar(30) — `TRANSICAO_ETAPA`, `IMPEDIMENTO_MARCADO`, `IMPEDIMENTO_DESMARCADO` |
| mensagem | varchar(500) |
| lida | boolean | default false |
| criadoEm | timestamptz |

Índice: `(usuarioId, lida, criadoEm)` — query da lista de notificações não lidas.

### PapelPermissaoAuditoria

Auditoria de alterações em toggles de permissão (RF-016, RN-017 — comitê de análise), estrutura análoga a `TarefaAuditoria`.

| Campo | Tipo |
|---|---|
| id | UUID PK |
| papelId | UUID FK → Papel |
| permissaoId | UUID FK → Permissao |
| autorId | UUID FK → Usuario |
| valorAnterior | boolean |
| valorNovo | boolean |
| dataHora | timestamptz |

Índice: `(papelId, dataHora)`.

---

## Ciclo de vida — Tarefa

```
[criada] --(sai da 1ª etapa)--> [iniciada=true, campos estruturais congelados]
[iniciada] --(marca impedimento)--> [impedida=true, TarefaImpedimentoHistorico aberto]
[impedida] --(desmarca)--> [impedida=false, TarefaImpedimentoHistorico fechado]
[iniciada] --(move p/ etapa final, requer tarefa:finalizar)--> [finalizada]
[finalizada] --("desfinalizar", requer tarefa:finalizar)--> [volta a etapa selecionada, RN-004/RN-011]
```

## Ciclo de vida — Projeto

```
[ATIVO] --(admin finaliza)--> [FINALIZADO, somente leitura p/ todos — RN-015]
[FINALIZADO] --(admin reabre)--> [ATIVO]
```

## Estratégia de migrations (Flyway)

| Migration | Conteúdo |
|---|---|
| V1 | Usuario, Projeto |
| V2 | Papel, Permissao, PapelPermissao, UsuarioProjetoPapel + seed de papéis/permissões default |
| V3 | Workflow, Etapa, Transicao |
| V4 | Raia (incl. seed de raia default global) |
| V5 | Tarefa, TarefaObservador |
| V6 | TarefaEtapaHistorico, TarefaImpedimentoHistorico, TarefaAuditoria |
| V7 | Notificacao |
| V8 | PapelPermissaoAuditoria (RN-017, achado do Comitê de Análise — Security) |
| V9 | Usuario.adminGlobal (bootstrap do primeiro admin, ADR-007, achado da TASK-03.1) |

Toda alteração futura de schema gera nova migration `V{n+1}` — nunca editar migration já aplicada (ver `git-workflow.md`).

## Nota de performance — projeção DTO obrigatória (Comitê de Análise — Database)

`GET /api/projetos/{projetoId}/board` e `GET /api/tarefas/{id}` agregam múltiplas entidades relacionadas (`Etapa`, `Raia`, `Tarefa`, `TarefaEtapaHistorico`, `TarefaImpedimentoHistorico`, `TarefaObservador`). Implementação **não pode** usar relações `lazy` percorridas em loop (risco real de N+1). Ambos os endpoints devem usar projeção DTO via JPQL `SELECT NEW` ou `@EntityGraph` cobrindo as associações necessárias em uma única query. Critério de aceite testável na task correspondente: contagem de queries via Hibernate Statistics/Testcontainers não deve escalar com o número de tarefas retornadas.
