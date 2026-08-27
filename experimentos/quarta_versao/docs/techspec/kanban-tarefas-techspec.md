# TechSpec — Kanban de Tarefas

_Versão: 1.1 | Status: Draft | Data: 2026-08-26 | Autor: Thiago Goncalves Cavalcante_

> Referências: [PRD v1.0](prd/kanban-tarefas-prd.md) · [Design Brief v1.0](../design/kanban-tarefas-design-brief.md)

---

## 1. Visão Geral Técnica

Aplicação web com backend **Spring Boot 3.5.16 / Java 25** (API REST + WebSocket/STOMP) e frontend **Next.js**, autenticação via **Keycloak (OIDC)**, persistência em **PostgreSQL**, sem fallback de auth local ([ADR-006](../decisions/ADR-006-sem-fallback-auth-keycloak.md)). Broadcast de eventos em tempo real entre pods via **PostgreSQL LISTEN/NOTIFY** ([ADR-004](../decisions/ADR-004-broadcast-listen-notify.md)), sem broker dedicado (ADR-002). Schema versionado via **Flyway** ([ADR-005](../decisions/ADR-005-flyway-migrations.md)). Deploy containerizado (Docker/OpenShift/Kubernetes, RNF-004).

Escopo: 19 RFs Must/Should Have cobrindo board configurável, workflows/transições, CRUD de tarefas com congelamento pós-início, impedimentos, notificações internas, lead-time (por etapa e agregado em dashboard), RBAC configurável por projeto com toggles, SSO e auditoria.

---

## 2. Decisões Arquiteturais

| Decisão | ADR |
|---|---|
| Backend Java 25 / Spring Boot 3.5.16 | [ADR-001](../decisions/ADR-001-stack-backend-java-spring.md) |
| PostgreSQL sem cache/broker dedicado nesta fase | [ADR-002](../decisions/ADR-002-postgresql-sem-cache-tempo-real.md) |
| RBAC híbrido (Keycloak autentica, app modela papéis) | [ADR-003](../decisions/ADR-003-rbac-hibrido-keycloak.md) |
| Broadcast multi-pod via PostgreSQL LISTEN/NOTIFY | [ADR-004](../decisions/ADR-004-broadcast-listen-notify.md) |
| Flyway para versionamento de schema | [ADR-005](../decisions/ADR-005-flyway-migrations.md) |
| Sem fallback de autenticação local | [ADR-006](../decisions/ADR-006-sem-fallback-auth-keycloak.md) |
| Dockerização de backend e frontend (RNF-004) | [ADR-008](../decisions/ADR-008-dockerizacao-backend-frontend.md) |

> **Nota:** ADR-001/002/003 são referenciados por `stack.md`/`architecture.md`/`security.md` mas os arquivos correspondentes não foram encontrados em `docs/decisions/` (apenas `.gitkeep`). Pré-existente ao escopo deste `/techspec` — não recriados aqui; revisar antes do `/tasks` se a ausência bloquear rastreabilidade.

**Trade-offs aceitos:**
- LISTEN/NOTIFY tem payload limitado (8KB) e não garante replay de eventos perdidos — aceitável para o volume esperado (dezenas a centenas de usuários, RNF-002); reavaliar broker dedicado se escalar além disso.
- Sem fallback de autenticação local: disponibilidade do sistema acoplada à do Keycloak — risco operacional aceito (ADR-006).

---

## 3. Modelo de Dados

Fonte de verdade: [data-model.md](kanban-tarefas/data-model.md).

**Entidades principais:** Usuario, Projeto, Papel, Permissao, PapelPermissao, UsuarioProjetoPapel, Workflow, Etapa, Transicao, Raia, Tarefa, TarefaObservador, TarefaEtapaHistorico, TarefaImpedimentoHistorico, TarefaAuditoria, Notificacao.

**Pontos-chave:**
- RBAC modelado como papéis por projeto (exceto `admin`, global e protegido — RN-006) com permissões granulares via toggle (`PapelPermissao`).
- Lead-time (RN-001, RN-002) calculado a partir de `TarefaEtapaHistorico` + `TarefaImpedimentoHistorico`, sem pré-agregação nesta fase.
- Campos estruturais da tarefa (`titulo`, `descricaoEscopo`) congelados por flag `iniciada`, validado em nível de serviço (RF-003).
- Migrations Flyway V1–V7 detalhadas no data model.

---

## 4. Contratos de API

Índice de endpoints (fonte de verdade em `kanban-tarefas/contracts/`):

| Recurso | Arquivo | RFs |
|---|---|---|
| Autenticação | [auth.md](kanban-tarefas/contracts/auth.md) | RF-014 |
| Projetos | [projetos.md](kanban-tarefas/contracts/projetos.md) | RF-008 |
| Workflows/Etapas/Transições | [workflows.md](kanban-tarefas/contracts/workflows.md) | RF-002, RF-009, RF-010 |
| Raias | [raias.md](kanban-tarefas/contracts/raias.md) | RF-011 |
| Tarefas (board, CRUD, mover, impedimento, auditoria) | [tarefas.md](kanban-tarefas/contracts/tarefas.md) | RF-001, RF-003, RF-004, RF-005, RF-006, RF-012, RF-017, RF-018, RF-019 |
| Papéis/Permissões/Usuários do projeto | [papeis-permissoes.md](kanban-tarefas/contracts/papeis-permissoes.md) | RF-013, RF-015, RF-016 |
| Dashboard/Notificações (incl. WebSocket) | [dashboard-notificacoes.md](kanban-tarefas/contracts/dashboard-notificacoes.md) | RF-005, RF-006, RF-007 |

Todos os endpoints de escrita retornam `403` se o usuário não possuir a permissão exigida no backend, independentemente do estado da UI (RNF-003).

---

## 5. Arquitetura e Fluxo

**Camadas (backend, conforme `architecture.md`):** Controller (REST + STOMP handlers) → Service (regras de negócio: transições, permissões, lead-time) → Repository (Spring Data JPA) → DTO/Mapper (MapStruct).

**Fluxo de movimentação de card (RF-002, RNF-001):**
1. Frontend envia `POST /api/tarefas/{id}/mover` com `etapaDestinoId`.
2. `TarefaService` valida transição, permissão (`tarefa:finalizar` se aplicável) e estado do projeto (não finalizado).
3. Em transação: fecha `TarefaEtapaHistorico` atual, abre novo, atualiza `Tarefa.etapaAtualId`/`iniciada`, grava `TarefaAuditoria`.
4. Após o commit da transação (`TransactionSynchronization.afterCommit`), o service invoca a porta `EventoBoardPublisher.publicar(...)`, implementada por um adapter LISTEN/NOTIFY que executa `NOTIFY board_events, '<payload_json + seq>'` — a Service layer não conhece o mecanismo de broadcast (ADR-004), apenas a interface de domínio.
5. Pod(s) em `LISTEN` recebem o evento e retransmitem via STOMP a `/topic/board/{projetoId}`.
6. Frontend dos clientes conectados atualiza o board sem refresh manual, dentro de 2s (RNF-001). Se o cliente detectar gap no `seq` do evento (ou reconectar o WebSocket), refaz `GET /api/projetos/{projetoId}/board` para resincronizar — mitiga perda de evento durante reconexão do listener (ADR-004).

**Fluxo de notificação (RF-005):** ao alterar etapa/impedimento, o service resolve a lista de observadores (responsável + criador + `TarefaObservador`), cria uma `Notificacao` por observador e publica em `/topic/notificacoes/{usuarioId}` (paralelo ao evento de board, mesmo `afterCommit`). **Decisão revisada em TASK-05.2** (substitui a opção "broadcast amplo + filtro client-side" cogitada nesta seção): replica a arquitetura do board (ADR-004) — canal dedicado `NOTIFY notificacao_events` + `NotificacaoEventListener` por pod, que retransmite localmente via STOMP para `/topic/notificacoes/{usuarioId}`. Como o `NOTIFY` alcança todos os pods e cada um retransmite para o mesmo destino STOMP determinístico, o pod que detém a sessão WebSocket do usuário entrega a mensagem — sem tabela de mapeamento `usuarioId → pod` e sem depender de filtro client-side. Motivo da mudança: o contrato `dashboard-notificacoes.md` já exige autorização de `SUBSCRIBE` por `usuarioId` no próprio tópico per-user, o que torna o canal amplo desnecessário e incompatível com a autorização documentada.

**Autorização em WebSocket/STOMP:** subscrição (`SUBSCRIBE`) em `/topic/board/{projetoId}` e `/topic/notificacoes/{usuarioId}` é validada em um `ChannelInterceptor` no handshake — o usuário autenticado (principal do JWT) deve ter vínculo `UsuarioProjetoPapel` ativo com o `projetoId` (para board) ou ser o próprio `usuarioId` (para notificações); subscrição sem autorização é rejeitada com `ERROR` STOMP. Cobre RNF-003 também para o canal WebSocket, não apenas endpoints REST.

**Multi-instância (RNF-002):** nenhum estado de sessão/negócio em memória não compartilhada — toda leitura de estado do board é servida do PostgreSQL; o listener LISTEN/NOTIFY é o único componente com estado de conexão por pod, e é stateless em relação ao dado (apenas retransmite). Resincronização client-side por `seq` (ADR-004) é a rede de segurança contra divergência residual sob falha de reconexão.

**Empacotamento e execução (RNF-004, ADR-008):** backend e frontend rodam como imagens Docker multi-stage (`backend/Dockerfile`, `frontend/Dockerfile`), orquestradas junto com `postgres`/`keycloak` no mesmo `docker-compose.yml`. `docker compose up -d` sobe a stack completa (infra + app) — caminho padrão para homologação. Setup local sem Docker (`mvnw spring-boot:run` / `npm run dev`) continua válido como alternativa de desenvolvimento ativo com hot reload, apontando para a mesma infra (`docker compose up -d postgres keycloak`).

---

## 6. Dependências Inter-Sistemas

| Dependência | Tipo | Status |
|---|---|---|
| Keycloak (OIDC) | Autenticação | Externo, assumido disponível (premissa do PRD). Sem mock — integração via Spring Security OAuth2/OIDC padrão. Sem fallback se indisponível (ADR-006). |
| PostgreSQL | Persistência + broadcast (LISTEN/NOTIFY) | Interno ao deploy, sem mock necessário. |

Nenhum mock contract foi necessário — não há integração com sistema de terceiros indisponível para consulta.

---

## 7. Estratégia de Testes

Conforme `testing.md`: **JUnit 5 + Testcontainers** (backend, com PostgreSQL real — crítico para validar `LISTEN/NOTIFY` e Flyway), **Jest/Vitest + Testing Library** (frontend). Cobertura: 80% TDD geral, 100% dos cenários Gherkin do PRD (BDD).

**Cobertura por RF — todos os 19 RFs Must/Should Have têm cenário mapeado (100% exigido por `testing.md`; detalhamento Dado/Quando/Então em [quickstart.md](kanban-tarefas/quickstart.md)):**
- RF-001: board retorna etapas na ordem configurada, cada uma com as tarefas correspondentes — teste de integração de serialização do board.
- RF-002/RN-003: transição bloqueada quando não configurada — teste de integração no `TarefaService.mover`.
- RF-003: congelamento de campos estruturais pós-início — teste unitário de validação no service.
- RF-004/RN-002/RN-013: acumulação correta de tempo de impedimento (múltiplos ciclos marca/desmarca); usuário sem `tarefa:impedimento` → `403`.
- RF-005: alteração de etapa/impedimento gera `Notificacao` para responsável + criador + observadores explícitos — teste de integração no fluxo de notificação.
- RF-006: cálculo de lead-time por etapa com etapa em andamento (`saidaEm=null`).
- RF-007: dashboard agrega lead-time médio corretamente com histórico de múltiplas tarefas/etapas — teste de integração com dataset controlado.
- RF-008/RN-015: projeto finalizado bloqueia toda escrita (inclusive para admin/project_admin) e mantém leitura/dashboard acessíveis.
- RF-009/RN-005: exclusão de workflow bloqueada com tarefas ativas vinculadas — teste de integração por entidade afetada (workflow).
- RF-010/RN-003: etapa não-final sem transição de saída configurada → erro de validação (`422`) ao tentar operacionalizar a etapa.
- RF-011/RN-005: agrupamento visual por raia no board; exclusão de raia bloqueada com tarefas ativas vinculadas.
- RF-012/RN-004/RN-011: "desfinalizar" exige `tarefa:finalizar`, retorna tarefa à etapa selecionada.
- RF-013/RNF-003: toda ação sensível testada com usuário sem a permissão exigida → espera `403` (teste por endpoint, não só por regra) — inclui subscrição STOMP sem vínculo ao projeto.
- RF-014: fluxo de redirect OIDC e criação just-in-time de `Usuario` no primeiro login (teste de integração com Keycloak via Testcontainers ou mock OIDC).
- RF-015: associação usuário↔projeto↔papel reflete corretamente nas permissões efetivas do usuário.
- RF-016: toggle de permissão desabilitado bloqueia a ação mesmo com papel normalmente permitido; tentativa de alterar permissão do próprio papel → `403` (RN-017, novo — comitê de análise).
- RF-017: alteração de responsável/título/etapa/impedimento gera linha em `TarefaAuditoria` com autor, valor anterior/novo e data/hora.
- RF-018/RN-CB-001/004/005: criação de card sem responsável/raia usa defaults corretos; sem `tarefa:gerenciar` → `403`.
- RF-019/RN-CB-002/003: exclusão bloqueada para dev com toggle desabilitado, permitida com toggle habilitado; bloqueada em projeto finalizado.
- RNF-001: teste de integração com 2 conexões WebSocket simuladas validando propagação do evento em <2s (`Awaitility`).
- RNF-002 (multi-pod): 2 instâncias Spring Boot compartilhando o mesmo PostgreSQL Testcontainer — evento publicado via pod A deve chegar ao cliente STOMP conectado ao pod B.
- ADR-004 (reconexão): kill da conexão JDBC do listener em execução + assert de reconexão com backoff e de que o próximo `NOTIFY` ainda propaga; teste complementar de resincronização client-side por gap de `seq`.

TDD obrigatório (via `/tdd`) para lógica de maior risco: engine de transições, cálculo de lead-time, resolução de permissões (`skill-conventions.md`).

---

## 8. Segurança e Observabilidade

**Segurança (conforme `security.md`):**
- Toda escrita valida permissão no backend via `@PreAuthorize`/checagem explícita no service — nunca apenas na UI (RNF-003), incluindo a subscrição de canais STOMP (ver Seção 5).
- Papel `admin` protegido contra edição/exclusão em nível de serviço, não só de UI (RN-006).
- **RN-017 (nova, comitê de análise):** um usuário não pode alterar `PapelPermissao` do(s) papel(is) que ele próprio possui no projeto corrente — previne autoconcessão de privilégio via toggle (RF-016). Alteração de permissão de papel próprio exige outro usuário com `papel:administrar` no projeto.
- Alterações em `PapelPermissao` geram registro de auditoria (mesma estrutura de `TarefaAuditoria`, campo `entidade` generalizado — detalhar em `data-model.md`/task de implementação).
- Toda checagem de permissão no backend valida `Usuario.ativo=true`, não apenas a presença de `UsuarioProjetoPapel` — cobre remoção/desativação de usuário sem revogação explícita de sessão.
- Fluxo de logout via RP-Initiated Logout do Keycloak (`POST /api/auth/logout` → back-channel logout), documentado em `auth.md`.
- Validação de entrada via Bean Validation (Jakarta) em todos os DTOs de request.
- Sessão via JWT/OIDC padrão do Spring Security — sem gestão de senha própria (ADR-006).
- Baseline OWASP Top 10 (proteção contra injeção via JPA parametrizado/MapStruct, sem SQL manual concatenado).

**Observabilidade (conforme `observability.md`):**
- Log em arquivo local, rotação a cada 5MB, retenção de 10 arquivos — configuração padrão Spring Boot/Logback.
- Sem APM/tracing nesta fase. Reavaliar se RNF-002 exigir troubleshooting distribuído em produção.
- Eventos de `NOTIFY`/reconexão de listener devem ser logados (nível `WARN`→`ERROR` progressivo em falha de reconexão) e refletidos no readiness probe do pod (Actuator) — um pod com listener desconectado não deve ser considerado saudável sob RNF-002.
- Health-check dedicado para dependência Keycloak (`/actuator/health/keycloak` ou equivalente) — dado que não há fallback (ADR-006), a operação precisa de sinal explícito de indisponibilidade do IdP, não apenas falha de login reportada por usuários.
- Métricas mínimas via Micrometer/Actuator (sem stack de tracing completa): contador de reconexões do listener por pod, latência entre `NOTIFY` e broadcast STOMP — necessárias para validar RNF-001/RNF-002 em produção, não só em Testcontainers.
- Runbook operacional de indisponibilidade do Keycloak (quem é acionado, tempo esperado de restabelecimento) a produzir antes do go-live — fora do escopo desta feature, mas referenciado aqui como pré-requisito operacional.

---

## 9. Matriz de Rastreabilidade

| RF | Contrato | Entidade(s) | Regras |
|---|---|---|---|
| RF-001 | tarefas.md (GET board) | Etapa, Raia, Tarefa | — |
| RF-002 | workflows.md, tarefas.md (mover) | Etapa, Transicao | RN-003 |
| RF-003 | tarefas.md (PUT) | Tarefa | campo `iniciada` |
| RF-004 | tarefas.md (impedimento) | TarefaImpedimentoHistorico | RN-002, RN-013 |
| RF-005 | tarefas.md, dashboard-notificacoes.md | Notificacao, TarefaObservador | WebSocket `/topic/notificacoes/{usuarioId}` |
| RF-006 | tarefas.md (GET detalhe) | TarefaEtapaHistorico, TarefaImpedimentoHistorico | RN-001, RN-002 |
| RF-007 | dashboard-notificacoes.md | TarefaEtapaHistorico (agregado) | — |
| RF-008 | projetos.md | Projeto | RN-015 |
| RF-009 | workflows.md | Workflow | RN-005 |
| RF-010 | workflows.md | Etapa | RN-003 |
| RF-011 | raias.md | Raia | RN-005 |
| RF-012 | tarefas.md (mover) | Tarefa | RN-004, RN-011 |
| RF-013 | papeis-permissoes.md | Papel, PapelPermissao | RNF-003 |
| RF-014 | auth.md | Usuario | ADR-006 |
| RF-015 | papeis-permissoes.md | UsuarioProjetoPapel | — |
| RF-016 | papeis-permissoes.md | PapelPermissao | RN-017 (novo) |
| RF-017 | tarefas.md (GET auditoria) | TarefaAuditoria | RN-016 |
| RF-018 | tarefas.md (POST) | Tarefa | RN-CB-001, RN-CB-004, RN-CB-005 |
| RF-019 | tarefas.md (DELETE) | Tarefa | RN-CB-001, RN-CB-002, RN-CB-003 |

Verificação automatizada pendente de execução (`check_rf_coverage.py`) — ver Fase 2.4 do workflow da skill.

---

## 10. Questões em Aberto

> **Nota:** as questões abaixo relativas a broadcast multi-pod, autorização STOMP e observabilidade mínima foram endereçadas na Seção 5/8 após o Comitê de Análise Assíncrono (Architect, Security, Database, DevOps) — 2026-08-25. Permanecem como "questão em aberto" apenas o detalhamento fino de implementação (não a decisão de abordagem, já tomada).

| Questão | Impacto | Bloqueante? |
|---|---|---|
| Modelagem final do toggle `devPodeExcluirTarefa` (permissão dedicada vs. flag de contexto sobre `tarefa:gerenciar`) | Afeta migration V2 e `PapelPermissao` | Não — decidir na task de implementação de papéis/permissões |
| Estrutura exata de auditoria de `PapelPermissao` (reuso de `TarefaAuditoria` generalizada vs. tabela dedicada) | Afeta migration V2 | Não — decidir na task de implementação de papéis/permissões (RN-017) |
| Mecanismo definitivo de roteamento STOMP por usuário em multi-pod (broadcast + filtro client-side vs. `UserDestinationMessageHandler` compartilhado) | Afeta confiabilidade de RF-005 sob RNF-002 | Não — abordagem inicial (broadcast + filtro) documentada na Seção 5; revisar se volume de usuários crescer |
| Disponibilidade do sistema acoplada à do Keycloak (sem fallback, ADR-006) | Risco operacional, não funcional | Não — decisão aceita; health-check dedicado adicionado (Seção 8), runbook operacional pendente fora do escopo desta feature |
| ADR-001/002/003 referenciados mas ausentes em `docs/decisions/` | Rastreabilidade | Não — não impede `/tasks`, mas recomenda-se recriar os arquivos antes de auditoria formal |

---

## Histórico de Revisões

| Versão | Data | Autor | Alteração |
|---|---|---|---|
| 1.0 | 2026-08-25 | Thiago Goncalves Cavalcante | Versão inicial |
| 1.1 | 2026-08-26 | Thiago Goncalves Cavalcante | ADR-008 — backend e frontend passam a rodar via Docker (fecha gap de RNF-004), Seções 2 e 5 atualizadas |
