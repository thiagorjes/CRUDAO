# Tasks — Kanban Configurável
_Versão: 1.0 | Data: 2026-08-22 | Autor: Thiago Goncalves Cavalcante_
_PRD: docs/prd/kanban-configuravel-prd.md v1.1_
_TechSpec: docs/techspec/kanban-configuravel-techspec.md v1.0_

> Planejamento sequencial, sem paralelismo (decisão do usuário) — cada task depende da conclusão da anterior, na ordem do Backlog Priorizado.
> Este documento é o **índice**: sumário, grafo de dependências e backlog priorizado. Cada task tem um arquivo próprio, auto-contido, em `docs/tasks/kanban-configuravel/`.

---

## Sumário de Epics

| ID | Epic | Tasks | Estimativa total | Pode iniciar |
|----|------|-------|-----------------|-------------|
| EPIC-00 | Infraestrutura Base | 2 | ~1.5 dia | Imediatamente |
| EPIC-01 | Domínio: Projeto, Workflow, Etapas e Raias | 2 | ~2 dias | Após EPIC-00 |
| EPIC-02 | Tarefas, Board e Tempo Real | 2 | ~2.5 dias | Após EPIC-01 |
| EPIC-03 | Lead-time e Dashboard | 1 | ~1.5 dia | Após EPIC-02 |
| EPIC-04 | RBAC e Autenticação (SSO Should Have) | 1 | ~1.5 dia | Após EPIC-00 (paralelo funcional, mas executado em sequência) |
| EPIC-05 | Frontend Next.js | 4 | ~4 dias | Após cada backend correspondente |
| EPIC-06 | Testes E2E e Fechamento | 1 | ~1 dia | Após EPIC-05 |

**Legenda:** P ≤ 4h | M 4–8h | G 1–2 dias

---

## Grafo de Dependências

```
EPIC-00
  └── TASK-00.1 (Keycloak via Docker) → TASK-00.2 (Setup backend/frontend/Postgres)
        └── TASK-01.1 (Domínio: Projeto/Workflow/Etapa/Transição) → TASK-01.2 (Raias)
              └── TASK-02.1 (Tarefa CRUD + engine de movimentação) → TASK-02.2 (Tempo real + notificações)
                    └── TASK-03.1 (Lead-time + Dashboard assíncrono)
        └── TASK-04.1 (RBAC + integração Keycloak) [após TASK-00.2, antes ou junto de EPIC-01+]
  └── TASK-05.0 (Frontend: Login Keycloak) [lacuna, criada em 2026-08-22] → depende de TASK-00.2, TASK-04.1
        └── TASK-05.1 (Frontend: Board) → depende de TASK-02.2 e TASK-05.0
  └── TASK-05.2 (Frontend: Dashboard) → depende de TASK-03.1
  └── TASK-05.3 (Frontend: Painel de Administração) → depende de TASK-04.1 e TASK-01.2
        └── TASK-06.1 (Testes E2E e fechamento)
```

---

## EPIC-00 — Infraestrutura Base

### US-00.1 — Ambiente de desenvolvimento pronto

#### TASK-00.1 — Provisionar Keycloak via Docker [M]
**Sistema:** CRUDAO | **RF:** RF-014 (dependência de RNF-003) | **Dependências:** nenhuma

**Contexto:**
Diretriz definida na techspec (seção 6): esta task deve ser uma das primeiras, antes de qualquer task de backend que dependa de autenticação/autorização, para não bloquear o fluxo de implementação.

**O que deve ser feito:**
- [ ] Subir instância local de Keycloak via `docker-compose` (`quay.io/keycloak/keycloak`)
- [ ] Criar realm `crudao` e client para a aplicação (Authorization Code Flow)
- [ ] Criar usuários e papéis de teste (admin, user)
- [ ] Validar claims retornadas (`sub`, `email`, `preferred_username`, `realm_access.roles`) contra o mock em `docs/contracts/CRUDAO-keycloak-mock-contract.md`
- [ ] Substituir o mock contract pelo contrato real (renomear e atualizar status para `ok`, conforme checklist do próprio arquivo)

**Guia técnico:**
- Arquivo: `docker-compose.yml` (novo serviço `keycloak`)
- Referência: `docs/contracts/CRUDAO-keycloak-mock-contract.md`, ADR-003

**Critérios de aceite:**
- Keycloak acessível localmente via Docker, com realm/client configurados
- Contrato de autenticação validado e documento renomeado para `CRUDAO-keycloak-contract.md` com status `ok`

---

#### TASK-00.2 — Setup do projeto base (backend, frontend, PostgreSQL) [G]
**Sistema:** CRUDAO | **RF:** — (infraestrutura) | **Dependências:** TASK-00.1

**Contexto:**
Estabelecer o esqueleto do backend Spring Boot e do frontend Next.js, com PostgreSQL via Docker, seguindo `systems/CRUDAO/guidelines/stack.md` e `architecture.md`.

**O que deve ser feito:**
- [ ] Criar projeto Spring Boot (Java 25) com Spring Data JPA/Hibernate, Lombok, MapStruct, Bean Validation, WebSocket/STOMP, client OIDC
- [ ] Configurar PostgreSQL via `docker-compose`
- [ ] Configurar Spring Security com client OIDC apontando para o Keycloak da TASK-00.1
- [ ] Criar projeto Next.js (Node LTS) com estrutura inicial de páginas
- [ ] Configurar linters: Spotless+Checkstyle (backend), ESLint+Prettier (frontend)
- [ ] Configurar logging em arquivo com rotação a cada 5MB, retendo os 10 últimos (guidelines/observability.md)
- [ ] Configurar estrutura de testes: JUnit 5 + Testcontainers (backend), Jest/Vitest + Testing Library (frontend)

**Guia técnico:**
- Arquivos: `backend/pom.xml` (ou `build.gradle`), `frontend/package.json`, `docker-compose.yml`
- Referência: `systems/CRUDAO/guidelines/stack.md`, `architecture.md`, `git-workflow.md`

**Critérios de aceite:**
- Backend sobe localmente via Docker e conecta ao PostgreSQL e ao Keycloak
- Frontend sobe localmente e faz uma chamada de exemplo à API do backend
- Lint e testes rodam localmente sem erro (mesmo com suíte vazia/mínima)

---

## EPIC-01 — Domínio: Projeto, Workflow, Etapas e Raias

### US-01.1 — Estrutura configurável de workflow por projeto

#### TASK-01.1 — Modelo de domínio: Projeto, Workflow, Etapa e Transição [G]
**Sistema:** CRUDAO | **RF:** RF-008, RF-009, RF-010, RF-002 | **Dependências:** TASK-00.2

**Contexto:**
Base do sistema: projetos configuráveis com workflows próprios, etapas ordenadas e transições que definem o que é permitido mover (RF-002, engine central do produto).

**O que deve ser feito:**
- [ ] Implementar entidades Projeto, Workflow, Etapa, Transição (ver `docs/techspec/kanban-configuravel/data-model.md`)
- [ ] CRUD de Projeto (RF-008), com bloqueio de exclusão se houver tarefas ativas (RN-005)
- [ ] CRUD de Workflow por projeto (RF-009), editável, afetando todas as tarefas do projeto
- [ ] CRUD de Etapa (RF-010), com flag `e_final`, bloqueio de exclusão se houver tarefas na etapa (RN-005), exigência de ao menos uma transição de saída para etapas não-finais (RN-003)
- [ ] CRUD de Transição, incluindo tipo `REABERTURA` para suportar "desfinalizar" (RF-012)
- [ ] Engine de validação de transição: dado etapa atual + etapa destino, retornar se é permitida

**Guia técnico:**
- Pacote: `domain/projeto`, `domain/workflow`
- Referência: `docs/techspec/kanban-configuravel-techspec.md` seções 3 e 4, `data-model.md`

**Critérios de aceite:**
- Endpoints REST de CRUD funcionando com validação de regras de negócio (RN-003, RN-005)
- Testes unitários da engine de transição cobrindo casos permitido/proibido/reabertura (TDD, 80%+)

---

#### TASK-01.2 — Raias (swimlanes) por projeto e default globais [M]
**Sistema:** CRUDAO | **RF:** RF-011 | **Dependências:** TASK-01.1

**Contexto:**
Suportar múltiplos desenvolvedores no mesmo board via raias horizontais, específicas do projeto ou default globais.

**O que deve ser feito:**
- [ ] Implementar entidade Raia (projeto_id nullable = raia default global)
- [ ] CRUD de Raia, com bloqueio de exclusão se houver tarefas (RN-005)
- [ ] Regra: projeto sem raias próprias usa raias default globais, editáveis/removíveis pelo admin do projeto (clarificado no PRD)

**Guia técnico:**
- Pacote: `domain/raia`
- Referência: PRD RF-011 (v1.1, clarificação)

**Critérios de aceite:**
- Board de um projeto sem raias próprias retorna as raias default globais
- Admin do projeto consegue mantê-las, editá-las ou removê-las

---

## EPIC-02 — Tarefas, Board e Tempo Real

### US-02.1 — Gestão de tarefas e movimentação no board

#### TASK-02.1 — CRUD de Tarefa e movimentação entre etapas [G]
**Sistema:** CRUDAO | **RF:** RF-003, RF-002, RF-004, RF-012 | **Dependências:** TASK-01.2

**Contexto:**
Núcleo funcional do board: criar/editar/excluir tarefas, movê-las respeitando o workflow, sinalizar impedimento e suportar reabertura de tarefas finalizadas.

**O que deve ser feito:**
- [ ] Implementar entidade Tarefa (com tipo, responsável, etapa/raia atual — ver enum TipoTarefa a definir, Q-004 da techspec)
- [ ] CRUD de Tarefa (RF-003)
- [ ] Endpoint `PATCH /api/tarefas/{id}/mover`, validando a transição contra o workflow (RF-002); impedimento não bloqueia nem libera movimentação — regra é só o workflow (RF-004 clarificado)
- [ ] Suporte à transição `REABERTURA` para "desfinalizar" (RF-012)
- [ ] Endpoint de marcar/desmarcar impedimento (`POST`/`DELETE /api/tarefas/{id}/impedimento`)
- [ ] Suporte a mover tarefa entre projetos por admin com permissão em ambos (confirmado na entrevista de techspec)

**Guia técnico:**
- Pacote: `domain/tarefa`
- Referência: `docs/techspec/kanban-configuravel-techspec.md` seção 4 (contratos REST)

**Critérios de aceite:**
- Movimentação de tarefa só ocorre se a transição for permitida pelo workflow (teste unitário)
- Marcar/desmarcar impedimento funciona independentemente da posição no workflow
- Cenários Gherkin do PRD (RF-002, RF-004, RF-012) cobertos a 100% (BDD)

---

#### TASK-02.2 — Tempo real (WebSocket/STOMP), broadcast multi-pod e observadores [G]
**Sistema:** CRUDAO | **RF:** RF-005, RNF-001, RNF-002 | **Dependências:** TASK-02.1

**Contexto:**
Entregar atualizações em tempo real (<2s) a todos os usuários conectados, incluindo observadores da tarefa, com consistência entre múltiplos pods via PostgreSQL LISTEN/NOTIFY (ADR-004).

**O que deve ser feito:**
- [ ] Configurar endpoint STOMP (`/topic/projetos/{id}/board`)
- [ ] Implementar listener `LISTEN/NOTIFY` do PostgreSQL por pod, retransmitindo eventos recebidos via STOMP local
- [ ] Publicar `NOTIFY` a cada mudança relevante (movimentação, impedimento, criação de tarefa)
- [ ] Implementar entidade Observador (usuários cadastrados vinculados à tarefa, RN-007) e notificação a eles nas transições (RF-005)
- [ ] Teste de integração simulando 2+ pods (ou 2 conexões WebSocket) validando entrega consistente

**Guia técnico:**
- Pacote: `realtime/`
- Referência: ADR-004, techspec seção 5 (Arquitetura e Fluxo)

**Critérios de aceite:**
- Evento originado em um pod chega a clientes conectados a outro pod em até 2s (RNF-001)
- Observadores da tarefa recebem notificação em toda transição de etapa

---

## EPIC-03 — Lead-time e Dashboard

### US-03.1 — Métricas de andamento para gestão

#### TASK-03.1 — Cálculo de lead-time, impedimento e Dashboard assíncrono [G]
**Sistema:** CRUDAO | **RF:** RF-006, RF-007 | **Dependências:** TASK-02.2

**Contexto:**
Dar visibilidade de lead-time por etapa e tempo em impedimento, com dashboard agregado por período configurável, calculado de forma assíncrona para não travar a UI (ADR-005).

**O que deve ser feito:**
- [ ] Implementar entidades RegistroEtapa e Impedimento (histórico de permanência e tempo impedido por etapa, RN-001, RN-002)
- [ ] Ao mover uma tarefa, fechar o RegistroEtapa da etapa anterior e abrir um novo na etapa destino
- [ ] Exibir na tarefa: tempo por etapa + observação de tempo em impedimento durante aquela etapa (RF-006)
- [ ] Endpoint `POST /api/projetos/{id}/dashboard/calcular` disparando cálculo `@Async`, respondendo com `jobId` (202)
- [ ] Entrega do resultado via STOMP (`/topic/projetos/{id}/dashboard/{jobId}`) com fallback de polling (`GET .../jobs/{jobId}`)
- [ ] Cálculo de lead-time médio por etapa e tempo médio em impedimento, filtrado pelo período (data início/fim) selecionado

**Guia técnico:**
- Pacote: `domain/leadtime`, `dashboard/`
- Referência: ADR-005, techspec seção 4 (Dashboard assíncrono)

**Critérios de aceite:**
- Lead-time e tempo de impedimento calculados corretamente mesmo com múltiplos períodos de impedimento intercalados (teste unitário)
- Dashboard não bloqueia a requisição HTTP inicial — resposta imediata com `jobId`, resultado entregue posteriormente

---

## EPIC-04 — RBAC e Autenticação

### US-04.1 — Controle de acesso configurável

#### TASK-04.1 — RBAC híbrido: papéis, permissões e integração Keycloak [G]
**Sistema:** CRUDAO | **RF:** RF-013, RF-014, RNF-003 | **Dependências:** TASK-00.2

**Contexto:**
Keycloak autentica; a aplicação modela papéis/permissões configuráveis pelo admin em runtime (ADR-003), com o papel `admin` protegido (RN-006).

**O que deve ser feito:**
- [ ] Implementar entidades Usuário, Papel, Permissão, PapelPermissao
- [ ] Seed dos papéis padrão `admin` (protegido) e `user`
- [ ] CRUD de Papel/Permissão pelo admin (ou papel delegado, exceto sobre o próprio papel `admin`, RN-006)
- [ ] Definir granularidade final das chaves de permissão (Q-003 da techspec: ex. `projeto:gerenciar`, `workflow:gerenciar`, `tarefa:gerenciar`, `impedimento:marcar`, `papel:gerenciar`, `dashboard:visualizar`)
- [ ] Middleware/aspecto de validação de permissão em todo endpoint de escrita (RNF-003)
- [ ] Integração OIDC completa com o Keycloak da TASK-00.1, mapeando claim/sub do usuário autenticado ao Usuário interno (RF-014)
- [ ] Implementar fallback de autenticação própria caso Keycloak esteja indisponível

**Guia técnico:**
- Pacote: `security/`, `domain/rbac`
- Referência: ADR-003, `docs/contracts/CRUDAO-keycloak-contract.md` (validado na TASK-00.1)

**Critérios de aceite:**
- Usuário autenticado via Keycloak tem seu papel/permissões aplicados corretamente
- Tentativa de alterar o papel `admin` por um papel delegado é bloqueada (teste unitário, RN-006)
- Todo endpoint de escrita rejeita requisição sem a permissão necessária

---

## EPIC-05 — Frontend Next.js

### US-05.1 — Interfaces do sistema

#### TASK-05.1 — Frontend: Board principal (drag-and-drop, cards, raias) [G]
**Sistema:** CRUDAO | **RF:** RF-001, RF-002, RF-004, RF-005, RF-012 | **Dependências:** TASK-02.2

**Contexto:**
Implementar a tela principal do sistema conforme o design brief e o protótipo aprovado (Artifact: board com drag-and-drop e destaque de colunas válidas).

**O que deve ser feito:**
- [ ] Implementar layout de board (colunas × raias) consumindo a API REST
- [ ] Conectar ao WebSocket/STOMP para atualização em tempo real (<2s)
- [ ] Implementar drag-and-drop real com destaque visual das colunas válidas (DDR-002)
- [ ] Implementar menu do card (avançar/retroceder/desfinalizar)
- [ ] Implementar indicador de impedimento (semáforo vermelho) e página de detalhe da tarefa
- [ ] Aplicar tokens do design brief (cores, tipografia Roboto, espaçamento base 8px, desktop-only ≥1024px)

**Guia técnico:**
- Referência: `docs/design/kanban-configuravel-design-brief.md`, protótipo aprovado (Artifact: https://claude.ai/code/artifact/a7612319-88d0-434d-9729-64d3d1604c6c)

**Critérios de aceite:**
- Board reflete o design aprovado no protótipo
- Drag-and-drop só permite drop em colunas com transição válida
- Atualizações de outros usuários aparecem em até 2s

---

#### TASK-05.2 — Frontend: Dashboard de gestão [M]
**Sistema:** CRUDAO | **RF:** RF-007 | **Dependências:** TASK-03.1

**Contexto:**
Implementar a tela de dashboard conforme o design brief e o protótipo aprovado (seletor de período, gráfico de barras, tabela).

**O que deve ser feito:**
- [ ] Implementar seletor de período (data início/fim) fixo no topo
- [ ] Disparar job assíncrono e tratar loading com skeleton screen (DDR-003)
- [ ] Renderizar gráfico de barras + tabela de lead-time médio por etapa e tempo médio em impedimento
- [ ] Aplicar fallback de polling caso WebSocket não esteja disponível

**Guia técnico:**
- Referência: `docs/design/kanban-configuravel-design-brief.md`, protótipo aprovado

**Critérios de aceite:**
- Dashboard reflete o design aprovado no protótipo
- Skeleton exibido durante o processamento assíncrono, substituído pelo resultado ao concluir

---

#### TASK-05.3 — Frontend: Painel de Administração [G]
**Sistema:** CRUDAO | **RF:** RF-008, RF-009, RF-010, RF-011, RF-013 | **Dependências:** TASK-04.1, TASK-01.2

**Contexto:**
Painel único separado do board, com seletor de projeto, para gerenciar Projetos, Workflows, Colunas, Raias e Papéis/Permissões; também acessível em modo restrito a partir do board ("Configurações do projeto").

**O que deve ser feito:**
- [ ] Implementar painel administrativo com seletor de projeto (troca de contexto)
- [ ] Telas de CRUD de Projeto, Workflow, Etapa, Transição, Raia
- [ ] Tela de CRUD de Papéis/Permissões (visível apenas a quem tem a permissão correspondente)
- [ ] Acesso restrito ao projeto corrente via "Configurações do projeto" a partir do board
- [ ] Feedback de erro (modal de confirmação) e sucesso (toast) conforme DDR-003

**Guia técnico:**
- Referência: `docs/design/kanban-configuravel-design-brief.md`

**Critérios de aceite:**
- Admin global alterna entre projetos no painel; usuário com permissão restrita edita apenas o projeto de origem
- Exclusões bloqueadas (RN-005) exibem modal de erro claro, orientando migração

---

## EPIC-06 — Testes E2E e Fechamento

### US-06.1 — Validação final

#### TASK-06.1 — Testes E2E dos fluxos principais e revisão de cobertura [M]
**Sistema:** CRUDAO | **RF:** todos (validação cruzada) | **Dependências:** TASK-05.1, TASK-05.2, TASK-05.3

**Contexto:**
Fechar a primeira entrega com testes de ponta a ponta dos fluxos críticos e confirmar a cobertura exigida pelas guidelines (80% TDD / 100% BDD).

**O que deve ser feito:**
- [ ] Escolher e configurar ferramenta de E2E (Q-005 da techspec, ex. Playwright)
- [ ] Cobrir fluxos: mover tarefa (drag e menu), marcar/desmarcar impedimento, desfinalizar, dashboard assíncrono, RBAC (bloqueio de ação sem permissão)
- [ ] Revisar cobertura de testes unitários/integração contra a meta de guidelines/testing.md
- [ ] Revisar `docs/spdd/kanban-configuravel-canvas.md` — confirmar se todas as dimensões estão preenchidas para transição a `READY`

**Guia técnico:**
- Referência: `systems/CRUDAO/guidelines/testing.md`

**Critérios de aceite:**
- Fluxos críticos cobertos por E2E, passando localmente
- Cobertura de testes atinge as metas definidas em guidelines

---

## Backlog Priorizado

_Ordem de início — sequencial, sem paralelismo._

| Prioridade | Task | Arquivo | Motivo |
|-----------|------|---------|--------|
| 1 | ~~TASK-00.1~~ ✅ | [kanban-configuravel/TASK-00.1-provisionar-keycloak-docker.md](kanban-configuravel/TASK-00.1-provisionar-keycloak-docker.md) | Concluída 2026-08-22 — Diretriz explícita: Keycloak deve ser uma das primeiras tasks, antes de qualquer dependência de autenticação |
| 2 | ~~TASK-00.2~~ ✅ | [kanban-configuravel/TASK-00.2-setup-projeto-base.md](kanban-configuravel/TASK-00.2-setup-projeto-base.md) | Concluída 2026-08-22 — Base de infraestrutura para todo o restante |
| 3 | ~~TASK-01.1~~ ✅ | [kanban-configuravel/TASK-01.1-dominio-projeto-workflow-etapa-transicao.md](kanban-configuravel/TASK-01.1-dominio-projeto-workflow-etapa-transicao.md) | Concluída 2026-08-22 — Núcleo de domínio (projeto/workflow/etapas/transições) do qual tudo depende |
| 4 | TASK-01.2 | [kanban-configuravel/TASK-01.2-raias-swimlanes.md](kanban-configuravel/TASK-01.2-raias-swimlanes.md) | Completa o domínio de board (raias) |
| 5 | TASK-02.1 | [kanban-configuravel/TASK-02.1-crud-tarefa-movimentacao.md](kanban-configuravel/TASK-02.1-crud-tarefa-movimentacao.md) | CRUD de tarefa e movimentação — funcionalidade central |
| 6 | TASK-02.2 | [kanban-configuravel/TASK-02.2-tempo-real-broadcast-observadores.md](kanban-configuravel/TASK-02.2-tempo-real-broadcast-observadores.md) | Tempo real e observadores, sobre a movimentação já implementada |
| 7 | TASK-04.1 | [kanban-configuravel/TASK-04.1-rbac-keycloak.md](kanban-configuravel/TASK-04.1-rbac-keycloak.md) | RBAC completo, necessário antes do painel de administração e antes de expor o sistema |
| 8 | TASK-03.1 | [kanban-configuravel/TASK-03.1-leadtime-dashboard-assincrono.md](kanban-configuravel/TASK-03.1-leadtime-dashboard-assincrono.md) | Lead-time e dashboard, sobre o histórico de movimentação já existente |
| 9a | TASK-05.0 | [kanban-configuravel/TASK-05.0-frontend-login-keycloak.md](kanban-configuravel/TASK-05.0-frontend-login-keycloak.md) | Lacuna identificada em 2026-08-22: login/OIDC no frontend, pré-requisito para qualquer chamada autenticada à API |
| 9 | TASK-05.1 | [kanban-configuravel/TASK-05.1-frontend-board.md](kanban-configuravel/TASK-05.1-frontend-board.md) | Frontend do Board, após o backend de tempo real estar pronto e o login (TASK-05.0) |
| 10 | TASK-05.2 | [kanban-configuravel/TASK-05.2-frontend-dashboard.md](kanban-configuravel/TASK-05.2-frontend-dashboard.md) | Frontend do Dashboard, após o backend de lead-time estar pronto |
| 11 | TASK-05.3 | [kanban-configuravel/TASK-05.3-frontend-painel-administracao.md](kanban-configuravel/TASK-05.3-frontend-painel-administracao.md) | Frontend do Painel Admin, após RBAC e domínio completos |
| 12 | TASK-06.1 | [kanban-configuravel/TASK-06.1-testes-e2e-fechamento.md](kanban-configuravel/TASK-06.1-testes-e2e-fechamento.md) | Fechamento com testes E2E, ao final de tudo |

---

## Fora do Escopo (Backlog Futuro)

- Integração com sistemas externos de notificação (email, Slack etc.)
- Controle de horas/timesheet do desenvolvedor
- Suporte a múltiplas organizações/clientes
- Dependência entre projetos (bloqueio cruzado)
- Migração de LISTEN/NOTIFY para Redis Pub/Sub, caso necessário sob carga real (Q-001 da techspec)
- Acessibilidade formal WCAG AA (fora de escopo nesta versão, DDR-003)
