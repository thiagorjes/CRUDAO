# Tasks — Kanban de Tarefas

_Versão: 1.1 | Status: Draft | Data: 2026-08-26 | Autor: Thiago Goncalves Cavalcante_

> Referências: [PRD v1.0](../prd/kanban-tarefas-prd.md) · [TechSpec v1.0](../techspec/kanban-tarefas-techspec.md) · [Data Model](../techspec/kanban-tarefas/data-model.md)
> Sistema: CRUDAO (único sistema no workspace — sem Plano Git Multi-Sistema)
> Granularidade: maior (tasks G, poucas P/M) — decisão do usuário na Fase 1, prioriza menos overhead de coordenação sobre paralelismo máximo.
> Nota de escopo (Fase 1): setup do Keycloak (realm/client) para ambiente **local de desenvolvimento** está em escopo desta feature (TASK-01.1, conforme `quickstart.md`) — Keycloak em si (servidor/IdP) é premissa externa já disponível (PRD Seção 7, ADR-006), mas o container/realm de dev precisa ser provisionado pela equipe. Confirmado sem menção de time externo responsável na TechSpec.
>
> **Revisão pós-geração (Comitê de Análise Assíncrono — Architect + QA, 2026-08-25):** aplicadas correções de 3 achados críticos e 1 alto — ver seção "Revisão do Comitê" ao final. TASK-02.2 foi desmembrada em TASK-02.2 (motor RBAC) + TASK-02.3 (CRUD papéis/permissões, dona da migration V8) para não bloquear paralelismo de 03.x; RN-005 (exclusão com tarefas ativas) e RN-012 (autoatribuição de responsável) passaram a ter task dona explícita; TDD obrigatório adicionado a TASK-02.2. **Total revisado: 25 tasks em 8 epics.**

---

## Grafo de Dependências

```
TASK-01.1 (Setup projeto + docker-compose + Keycloak dev)
  └── TASK-01.2 (Migrations V1-V2: Usuario/Projeto/Papel/Permissao)
        ├── TASK-02.1 (OIDC + provisioning JIT + /api/me + logout)
        │     └── TASK-02.2 (RBAC: motor de permissões efetivas + guard — TDD obrigatório)
        │           ├── TASK-02.3 (CRUD papéis/permissões/usuários — dona da migration V8)  — não bloqueia 03.x/04.x
        │           ├── TASK-03.1 [P] (CRUD Projeto)         ⚡ paralelo com 03.2/03.3
        │           ├── TASK-03.2 [P] (CRUD Workflow/Etapa/Transicao — stub RN-005)  ⚡
        │           └── TASK-03.3 [P] (CRUD Raia — stub RN-005)      ⚡
        │                 └── TASK-04.1 (Migrations V5-V6 + criar card + fecha RN-005 real)
        │                       ├── TASK-04.2 (Mover tarefa: transição + congelamento + lead-time + RN-012)
        │                       ├── TASK-04.3 [P] (Impedimento)        ⚡ paralelo com 04.2/04.4
        │                       └── TASK-04.4 [P] (Excluir tarefa + leitura de auditoria — depende também de 02.3)  ⚡
        │                             └── TASK-04.5 (GET board + GET detalhe, projeção DTO)
        │                                   ├── TASK-05.1 (EventoBoardPublisher + LISTEN/NOTIFY + STOMP — depende também de 04.1/04.2/04.4)
        │                                   │     ├── TASK-05.2 (Notificações internas — depende também de 04.3)
        │                                   │     └── TASK-05.3 (Resiliência: reconexão, resync, health-check)
        │                                   │           ├── TASK-08.1 (Testes multi-pod / WebSocket — depende também de 05.2)
        │                                   │           └── TASK-08.2 [P] (Observabilidade final)  ⚡
        │                                   └── TASK-06.1 (Migration V7 + dashboard lead-time — sem V8)
        │
        └── (frontend, depende de 02.x/03.x/04.x/05.x/06.1 conforme abaixo)
              TASK-07.1 (Shell Next.js + auth + RNF-005)
                ├── TASK-07.2 (Board: colunas, raias, cards, mover)      — depende também de 04.5, 05.1
                │     └── TASK-07.3 (Detalhe da tarefa)                  — depende também de 04.5
                ├── TASK-07.4 [P] (Admin projeto/workflow/raia)          ⚡ — depende também de 03.1-03.3
                ├── TASK-07.5 [P] (Admin papéis/permissões)              ⚡ — depende também de 02.3
                ├── TASK-07.6 [P] (Dashboard UI)                         ⚡ — depende também de 06.1
                └── TASK-07.7 [P] (Notificações UI)                      ⚡ — depende também de 05.2
```

---

## Sumário de Epics

| Epic | Nome | Tasks | Foco |
|---|---|---|---|
| 01 | Infra Base | 01.1, 01.2 | Setup de projeto, docker-compose, Keycloak dev, migrations iniciais |
| 02 | Autenticação & RBAC | 02.1, 02.2, 02.3 | OIDC, provisioning JIT, resolução de permissões, CRUD papéis |
| 03 | Projetos/Workflows/Raias | 03.1, 03.2, 03.3 | CRUD de configuração do board |
| 04 | Tarefas (core) | 04.1–04.5 | CRUD de card, transições, impedimento, auditoria, board |
| 05 | Tempo real & Notificações | 05.1–05.3 | LISTEN/NOTIFY, STOMP, notificações, resiliência |
| 06 | Dashboard | 06.1 | Agregação de lead-time |
| 07 | Frontend | 07.1–07.7 | Todas as telas |
| 08 | Hardening | 08.1, 08.2 | Testes multi-pod, observabilidade |

**Total:** 25 tasks em 8 epics (revisado pelo Comitê de Análise — TASK-02.2 original desmembrada em 02.2 + 02.3).

---

## EPIC 01 — Infra Base

### TASK-01.1 — Setup de projeto backend/frontend + docker-compose + Keycloak dev [G]

- **Sistema:** CRUDAO
- **RF de origem:** RNF-004 (pré-requisito de todas as demais)
- **Dependências:** nenhuma
- **Contexto:** Base de execução local para todo o desenvolvimento subsequente. Sem isso, nenhuma outra task pode ser validada localmente (Postgres + Keycloak são dependências diretas de quase todo RF).
- **O que deve ser feito:**
  - [ ] Criar esqueleto Spring Boot 3.5.16 / Java 25 (`backend/`) com dependências: Web, Data JPA, OAuth2 Client/Resource Server, WebSocket, Validation, Flyway, Actuator, MapStruct.
  - [ ] Criar esqueleto Next.js (`frontend/`).
  - [ ] Criar `docker-compose.yml` na raiz do sistema com serviços `postgres` e `keycloak` (imagem oficial, modo dev).
  - [ ] Criar realm export do Keycloak (`keycloak/realm-export.json`) com client OIDC configurado, redirect URI `http://localhost:3000/login/oauth2/code/keycloak`, e ao menos 2 usuários de teste (um por papel dev/admin) — importado automaticamente no boot do container.
  - [ ] Configurar `application.yml` (dev profile) apontando para Postgres/Keycloak locais.
  - [ ] Configurar Dockerfile do backend e do frontend (RNF-004).
- **Guia técnico:** `backend/pom.xml`, `backend/src/main/resources/application.yml`, `docker-compose.yml`, `keycloak/realm-export.json`, `frontend/package.json`. Seguir `stack.md`/`architecture.md` para versões e estrutura de pacotes.
- **Critérios de aceite:**
  - `docker compose up postgres keycloak` sobe os dois serviços saudáveis.
  - Backend inicia (`./mvnw spring-boot:run`) e conecta ao Postgres sem erro.
  - Frontend inicia (`npm run dev`) servindo página padrão.
  - Realm importado automaticamente contém client e usuários de teste — validado por login manual no Keycloak admin console.

---

### TASK-01.2 — Migrations V1-V2: Usuario, Projeto, Papel, Permissao, PapelPermissao, UsuarioProjetoPapel [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-008, RF-013, RF-014, RF-015, RF-016
- **Dependências:** TASK-01.1
- **Contexto:** Base de dados fundacional para autenticação e RBAC — todas as demais entidades referenciam `Usuario` e `Projeto`.
- **O que deve ser feito:**
  - [ ] Criar migration V1 (Usuario, Projeto) conforme `data-model.md`.
  - [ ] Criar migration V2 (Papel, Permissao, PapelPermissao, UsuarioProjetoPapel) com seed de papéis (`admin`, `product_owner`, `project_admin`, `dev`, `gestor`), catálogo de permissões (`tarefa:gerenciar`, `tarefa:finalizar`, `tarefa:impedimento`, `projeto:administrar`, `workflow:administrar`, `papel:administrar`, `usuario:associar`) e defaults de `PapelPermissao` refletindo RN-011, RN-012, RN-013, RN-CB-001, RN-CB-002.
  - [ ] Criar entidades JPA correspondentes + repositórios Spring Data.
  - [ ] Validar `admin` como papel global (`projetoId=null`) e protegido (RN-006).
- **Guia técnico:** `backend/src/main/resources/db/migration/V1__...sql`, `V2__...sql`; `backend/src/main/java/.../domain/usuario/`, `.../domain/papel/`.
- **Critérios de aceite:**
  - Flyway aplica V1/V2 sem erro no boot (Testcontainers).
  - Seed de papéis/permissões presente e defaults batem com RN-011/012/013/RN-CB-001/002 (teste de integração lendo `PapelPermissao`).
  - Tentativa de criar/editar papel `admin` via repositório com `protegido=true` é rejeitada em nível de serviço (placeholder de teste, serviço completo vem em 02.2).

---

## EPIC 02 — Autenticação & RBAC

### TASK-02.1 — Integração OIDC Keycloak + provisioning JIT + /api/me + logout [G]

- **Sistema:** CRUDAO
- **RF de origem:** RF-014
- **Dependências:** TASK-01.2
- **Contexto:** Habilita login via SSO — pré-requisito de qualquer ação autenticada no sistema.
- **O que deve ser feito:**
  - [ ] Configurar Spring Security OAuth2 Client para Authorization Code Flow com Keycloak.
  - [ ] Implementar provisioning just-in-time de `Usuario` a partir do `sub`/claims do token no primeiro login.
  - [ ] Implementar `GET /api/me` retornando usuário + vínculos projeto/papel (contrato `auth.md`).
  - [ ] Implementar `POST /api/auth/logout` com RP-Initiated Logout (back-channel) no Keycloak.
  - [ ] Retornar `401` se `Usuario.ativo=false` mesmo com token válido (achado do Comitê — Security).
  - [ ] Health-check dedicado de dependência Keycloak (`/actuator/health/keycloak` ou equivalente).
- **Guia técnico:** `backend/src/main/java/.../security/`, `.../auth/`. Contrato: `docs/techspec/kanban-tarefas/contracts/auth.md`. ADR-006 (sem fallback).
- **Critérios de aceite:**
  - Login via Keycloak (ambiente dev de 01.1) redireciona corretamente e cria `Usuario` local no primeiro acesso.
  - `GET /api/me` retorna estrutura do contrato.
  - `POST /api/auth/logout` invalida sessão local e token no Keycloak (verificável por tentativa de reuso do token).
  - Usuário com `ativo=false` recebe `401` em qualquer endpoint autenticado.
  - Health-check reflete indisponibilidade do Keycloak quando o container é parado.

---

### TASK-02.2 — RBAC: motor de permissões efetivas + guard reutilizável [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-013 (suporte)
- **Dependências:** TASK-02.1
- **[P] com:** TASK-03.1, TASK-03.2, TASK-03.3 (após concluída, essas podem rodar em paralelo)
- **Contexto:** Motor central de autorização — todo endpoint de escrita das demais epics depende deste serviço para revalidação backend (RNF-003). Desmembrada de uma task única (achado do Comitê de Análise — Architect) para não bloquear o paralelismo de 03.x com escopo de CRUD que elas não precisam. **TDD obrigatório** (achado do Comitê — QA: TechSpec exige TDD para resolução de permissões, omitido na primeira versão desta task).
- **O que deve ser feito:**
  - [ ] Implementar serviço de resolução de permissões efetivas do usuário por projeto (papel(is) + toggles `PapelPermissao` habilitados + `Usuario.ativo`).
  - [ ] Implementar checagem reutilizável (`@PreAuthorize` custom ou service guard) usada por todos os controllers de escrita subsequentes.
- **Guia técnico:** `backend/src/main/java/.../rbac/`. Usar `/tdd`.
- **Critérios de aceite:**
  - Endpoint sensível sem permissão exigida retorna `403` (teste por endpoint, não só por regra — RNF-003).
  - Toggle desabilitado bloqueia ação mesmo com papel normalmente permitido.
  - Usuário com `ativo=false` nunca passa na checagem.

---

### TASK-02.3 — CRUD de papéis/permissões/usuários (RN-006, RN-017) [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-013, RF-015, RF-016
- **Dependências:** TASK-02.2
- **[P] com:** TASK-03.1, TASK-03.2, TASK-03.3, TASK-04.x (não bloqueia mais o board — só bloqueia TASK-07.5)
- **Contexto:** Destacada de TASK-02.2 pelo Comitê de Análise. **Dona única da migration V8** (`PapelPermissaoAuditoria`) — TASK-04.4 e TASK-06.1 apenas referenciam essa migration já existente.
- **O que deve ser feito:**
  - [ ] Implementar CRUD de papéis por projeto (exceto `admin`, protegido — RN-006), `PapelPermissao` (toggles), associação usuário↔projeto↔papel (RF-015).
  - [ ] Implementar RN-017: bloquear alteração de `PapelPermissao` do(s) papel(is) que o próprio usuário possui no projeto — exige outro usuário com `papel:administrar`.
  - [ ] Criar migration V8 (`PapelPermissaoAuditoria`).
  - [ ] Registrar `PapelPermissaoAuditoria` em toda alteração de toggle.
- **Guia técnico:** `backend/src/main/java/.../papel/`. Contrato: `papeis-permissoes.md`. Migration V8.
- **Critérios de aceite:**
  - Tentativa de alterar `PapelPermissao` do próprio papel retorna `403` (RN-017); outro admin consegue.
  - Alteração de toggle gera linha em `PapelPermissaoAuditoria`.
  - Tentativa de editar/excluir papel `admin` bloqueada (RN-006).

---

## EPIC 03 — Projetos/Workflows/Raias

### TASK-03.1 — CRUD de Projeto incl. finalizar/reabrir [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-008
- **Dependências:** TASK-02.2
- **[P] com:** TASK-03.2, TASK-03.3
- **Contexto:** Ciclo de vida do projeto — finalização bloqueia toda escrita subsequente (RN-015), afeta todas as demais epics de domínio.
- **O que deve ser feito:**
  - [ ] Implementar `POST/PUT/GET /api/projetos` (contrato `projetos.md`).
  - [ ] Implementar `finalizar`/`reabrir` com checagem `projeto:administrar`.
  - [ ] Implementar guard reutilizável "projeto finalizado → somente leitura" a ser usado pelas demais epics de escrita (04.x).
- **Guia técnico:** `backend/src/main/java/.../projeto/`. Contrato: `projetos.md`.
- **Critérios de aceite:**
  - Projeto finalizado bloqueia toda escrita, inclusive para `admin`/`project_admin` (RN-015).
  - Reabertura restaura capacidade de edição.
  - Dashboard/leitura permanecem acessíveis com projeto finalizado.

---

### TASK-03.2 — CRUD Workflow/Etapa/Transicao [G]

- **Sistema:** CRUDAO
- **RF de origem:** RF-002, RF-009, RF-010
- **Dependências:** TASK-02.2
- **[P] com:** TASK-03.1, TASK-03.3
- **Contexto:** Motor de workflow configurável — base para toda movimentação de tarefas (Epic 04).
- **O que deve ser feito:**
  - [ ] Criar migration V3 (Workflow, Etapa, Transicao).
  - [ ] Implementar CRUD de Workflow, Etapa (reordenação incluída), Transicao (contrato `workflows.md`).
  - [ ] Validar RN-003 (etapa não-final exige ≥1 transição de saída) em nível de serviço na criação/edição de Etapa.
  - [ ] Preparar RN-005 (bloquear exclusão de workflow/etapa com tarefas ativas vinculadas): stub que sempre retorna "sem tarefas ativas" (decisão fechada pelo Comitê). **Checagem real obrigatória em TASK-04.1**, não é alternativa em aberto.
- **Guia técnico:** `backend/src/main/resources/db/migration/V3__...sql`; `backend/src/main/java/.../workflow/`. Contrato: `workflows.md`.
- **Critérios de aceite:**
  - Transição bloqueada quando não configurada (`TarefaService.mover` só existe em 04.2, mas a validação de configuração da Transicao já é testável aqui isoladamente).
  - Etapa não-final sem transição de saída configurada → erro `422` ao tentar salvar/operacionalizar.
  - Stub de RN-005 responde "sem tarefas ativas" (implementação real fica em TASK-04.1, fora do escopo de aceite desta task).

---

### TASK-03.3 — CRUD Raia (swimlanes) [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-011
- **Dependências:** TASK-02.2
- **[P] com:** TASK-03.1, TASK-03.2
- **Contexto:** Agrupamento visual de tarefas no board — inclui raia default global usada quando o card é criado sem raia (RN-CB-005).
- **O que deve ser feito:**
  - [ ] Criar migration V4 (Raia, incl. seed de raia default global com `projetoId=null`).
  - [ ] Implementar CRUD de Raia (contrato `raias.md`).
  - [ ] Preparar RN-005 (bloquear exclusão com tarefas ativas vinculadas): stub "sem tarefas ativas" (mesma decisão de 03.2). **Checagem real obrigatória em TASK-04.1.**
- **Guia técnico:** `backend/src/main/resources/db/migration/V4__...sql`; `backend/src/main/java/.../raia/`. Contrato: `raias.md`.
- **Critérios de aceite:**
  - Raia default global existe após seed e é usada quando projeto não tem raia própria.
  - Stub de RN-005 responde "sem tarefas ativas" (implementação real fica em TASK-04.1, fora do escopo de aceite desta task).

---

## EPIC 04 — Tarefas (core domain)

### TASK-04.1 — Migrations V5-V6 + entidade Tarefa + criação de card [G]

- **Sistema:** CRUDAO
- **RF de origem:** RF-018
- **Dependências:** TASK-02.2, TASK-03.2, TASK-03.3
- **Contexto:** Núcleo do domínio — todas as demais tasks de Epic 04/05/06 dependem da entidade Tarefa existir.
- **O que deve ser feito:**
  - [ ] Criar migration V5 (Tarefa, TarefaObservador) e V6 (TarefaEtapaHistorico, TarefaImpedimentoHistorico, TarefaAuditoria) conforme `data-model.md`, incluindo os índices de suporte a agregação (achado do Comitê — Database).
  - [ ] Implementar `POST /api/tarefas` (criar card pelo board — contrato `tarefas.md`): sem responsável se não informado (RN-CB-004), etapa de menor ordem + primeira raia do projeto ou raia default global (RN-CB-005), exige `tarefa:gerenciar` (RN-CB-001), bloqueado se projeto finalizado (RN-CB-003).
  - [ ] Ao criar, abrir o primeiro `TarefaEtapaHistorico` (entradaEm=now, saidaEm=null).
  - [ ] **Obrigatório:** implementar a checagem real de RN-005 em `WorkflowService`/`EtapaService`/`RaiaService`, substituindo o stub de TASK-03.2/TASK-03.3 (achado do Comitê de Análise).
- **Guia técnico:** `backend/src/main/resources/db/migration/V5__...sql`, `V6__...sql`; `backend/src/main/java/.../tarefa/`. Contrato: `tarefas.md` (seção POST).
- **Critérios de aceite:**
  - Criação sem responsável/raia usa defaults corretos (RN-CB-004, RN-CB-005).
  - Sem `tarefa:gerenciar` → `403`.
  - Projeto finalizado → bloqueado.
  - `TarefaEtapaHistorico` inicial criado corretamente.

---

### TASK-04.2 — Mover tarefa: engine de transição + congelamento + lead-time [G]

- **Sistema:** CRUDAO
- **RF de origem:** RF-002, RF-003, RF-006, RF-012
- **Dependências:** TASK-04.1
- **Contexto:** Lógica de maior risco do domínio (TDD obrigatório conforme `skill-conventions.md`) — movimentação de card, congelamento pós-início e cálculo de lead-time por etapa.
- **O que deve ser feito:**
  - [ ] Implementar `POST /api/tarefas/{id}/mover` (contrato `tarefas.md`): valida transição configurada (RN-003 via Transicao), valida `tarefa:finalizar` se destino/origem for etapa final (RN-011), valida projeto não finalizado.
  - [ ] Em transação: fechar `TarefaEtapaHistorico` atual (`saidaEm=now`), abrir novo, atualizar `etapaAtualId`, setar `iniciada=true` ao sair da primeira etapa, gravar `TarefaAuditoria` (campo `etapa`).
  - [ ] Implementar "desfinalizar" (RN-004, RN-011): retorna tarefa a etapa selecionada, exige `tarefa:finalizar`.
  - [ ] Implementar congelamento de campos estruturais (`titulo`, `descricaoEscopo`) via `PUT /api/tarefas/{id}` quando `iniciada=true` — permite apenas campos editáveis pós-início (RF-003).
  - [ ] Implementar `GET /api/tarefas/{id}` com cálculo de lead-time por etapa (RN-001) a partir de `TarefaEtapaHistorico` (etapa em andamento: `now() - entradaEm`).
  - [ ] Implementar RN-012 (autoatribuição de responsável, achado do Comitê — QA): dev só se autoatribui (nunca a terceiros, mesmo já atribuída); product_owner/project_admin/admin atribuem/reatribuem livremente; toda troca gera `TarefaAuditoria` (campo `responsavel`).
- **Guia técnico:** `backend/src/main/java/.../tarefa/TarefaService.java` (mover, editar, detalhe). TDD obrigatório — usar `/tdd` para esta task.
- **Critérios de aceite:**
  - Transição bloqueada quando não configurada.
  - Mover para/reabrir etapa final sem `tarefa:finalizar` → `403`.
  - Edição de campo estrutural após início bloqueada; campos editáveis pós-início (responsável, etapa via transição, impedimento) permanecem editáveis.
  - Lead-time por etapa calculado corretamente, incluindo etapa em andamento (`saidaEm=null`).
  - Toda movimentação gera linha em `TarefaAuditoria`.

---

### TASK-04.3 — Impedimento: marcar/desmarcar + histórico [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-004
- **Dependências:** TASK-04.1
- **[P] com:** TASK-04.2, TASK-04.4
- **Contexto:** Sinalização de bloqueio — base do KPI de redução de tempo parado por impedimento não visto (motivação central do PRD).
- **O que deve ser feito:**
  - [ ] Implementar `POST/DELETE /api/tarefas/{id}/impedimento` (contrato `tarefas.md`): exige `tarefa:impedimento` (RN-013 — dev e product_owner por padrão, gestor não).
  - [ ] Ao marcar: abrir `TarefaImpedimentoHistorico` (`marcadoEm=now`), setar `Tarefa.impedida=true`/`impedidaDesde`.
  - [ ] Ao desmarcar: fechar `TarefaImpedimentoHistorico` (`desmarcadoEm=now`), setar `impedida=false`.
  - [ ] Suportar múltiplos ciclos marca/desmarca acumulando corretamente (RN-002 — validado no cálculo de lead-time em 04.5/06.1).
  - [ ] Gravar `TarefaAuditoria` (campo `impedimento`).
- **Guia técnico:** `backend/src/main/java/.../tarefa/ImpedimentoService.java` (ou método dedicado em `TarefaService`).
- **Critérios de aceite:**
  - Usuário sem `tarefa:impedimento` → `403`.
  - Marcar/desmarcar reflete corretamente em `impedida`/`impedidaDesde` e no histórico.
  - Múltiplos ciclos acumulam tempo de impedimento corretamente.
  - Auditoria registrada em cada marca/desmarca.

---

### TASK-04.4 — Excluir tarefa + leitura de auditoria [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-019, RF-017
- **Dependências:** TASK-04.1, TASK-02.3
- **[P] com:** TASK-04.2, TASK-04.3
- **Contexto:** Fecha o RF-019 (exclusão de card pelo board) e consolida a leitura da auditoria (`TarefaAuditoria` já gravada em 04.2/04.3). Modelagem de `devPodeExcluirTarefa` decidida pelo Comitê: permissão dedicada `tarefa:excluir` no catálogo (não flag de contexto).
- **O que deve ser feito:**
  - [ ] Implementar `DELETE /api/tarefas/{id}` (contrato `tarefas.md`): exige `tarefa:gerenciar` (RN-CB-001); se usuário é `dev`, exige adicionalmente `tarefa:excluir` habilitada (RN-CB-002); bloqueado se projeto finalizado (RN-CB-003).
  - [ ] Implementar `GET /api/tarefas/{id}/auditoria` (RF-017) retornando histórico completo (autor, valor anterior/novo, data/hora) agregando os registros gravados em 04.2/04.3 e nesta task.
  - [ ] Adicionar/confirmar permissão `tarefa:excluir` no catálogo e defaults por papel (via seed de V2/TASK-01.2 — **não criar migration nova**; V8 é de TASK-02.3).
- **Guia técnico:** `backend/src/main/java/.../tarefa/TarefaService.java` (excluir); `.../tarefa/TarefaAuditoriaService.java`.
- **Critérios de aceite:**
  - Exclusão bloqueada para dev com `tarefa:excluir` desabilitada; permitida com toggle habilitada.
  - Exclusão bloqueada em projeto finalizado.
  - `GET /auditoria` retorna todas as alterações relevantes (responsável, título, etapa, impedimento) com autor/valores/data.

---

### TASK-04.5 — GET board + GET detalhe com projeção DTO (sem N+1) [G]

- **Sistema:** CRUDAO
- **RF de origem:** RF-001, RF-006
- **Dependências:** TASK-04.2, TASK-04.3
- **Contexto:** Endpoint mais consultado do sistema — exige projeção DTO obrigatória (achado do Comitê — Database, `data-model.md` seção final) para evitar N+1 sob volume de tarefas.
- **O que deve ser feito:**
  - [ ] Implementar `GET /api/projetos/{projetoId}/board` retornando etapas na ordem configurada, cada uma com as tarefas correspondentes, agrupadas por raia — via JPQL `SELECT NEW` ou `@EntityGraph` cobrindo `Etapa`, `Raia`, `Tarefa`, indicadores de impedimento.
  - [ ] Confirmar `GET /api/tarefas/{id}` (iniciado em 04.2) usa a mesma estratégia de projeção para as associações de histórico.
  - [ ] Validar via Hibernate Statistics/Testcontainers que a contagem de queries não escala com o número de tarefas retornadas (critério de aceite explícito da TechSpec).
- **Guia técnico:** `backend/src/main/java/.../tarefa/BoardService.java`, `.../dto/BoardDTO.java` (MapStruct/JPQL). Contrato: `tarefas.md` (GET board).
- **Critérios de aceite:**
  - Board retorna etapas na ordem configurada, cada uma com as tarefas correspondentes.
  - Teste de integração comprova ausência de N+1 (contagem de queries fixa independentemente do volume de tarefas).

---

## EPIC 05 — Tempo real & Notificações

### TASK-05.1 — EventoBoardPublisher + adapter LISTEN/NOTIFY + STOMP + autorização de subscrição [G]

- **Sistema:** CRUDAO
- **RF de origem:** RNF-001, RNF-002 (suporte a RF-002/RF-005/RF-019)
- **Dependências:** TASK-04.1, TASK-04.2, TASK-04.4, TASK-04.5
- **Contexto:** Mecanismo de broadcast multi-pod (ADR-004) — viabiliza atualização em tempo real do board sem depender de refresh manual. Grafo de dependência corrigido pelo Comitê (esta task altera código já implementado em 04.1/04.2/04.4, não só 04.5).
- **O que deve ser feito:**
  - [ ] Definir porta de domínio `EventoBoardPublisher` (interface) desacoplada do mecanismo de transporte.
  - [ ] Implementar adapter LISTEN/NOTIFY: `NOTIFY board_events, '<payload_json + seq>'`, invocado via `TransactionSynchronization.afterCommit` nos services de 04.x (mover, criar, excluir).
  - [ ] Implementar listener por pod (`LISTEN board_events`) retransmitindo via STOMP a `/topic/board/{projetoId}`.
  - [ ] Implementar `ChannelInterceptor` validando autorização na subscrição STOMP: vínculo `UsuarioProjetoPapel` ativo com o `projetoId` (board) — rejeita com `ERROR` STOMP caso contrário.
  - [ ] Conectar os pontos de publicação já criados em 04.2 (mover), 04.1 (criar) e 04.4 (excluir) ao publisher.
- **Guia técnico:** `backend/src/main/java/.../evento/EventoBoardPublisher.java` (porta), `.../evento/adapter/ListenNotifyPublisher.java`, `.../websocket/StompConfig.java`, `.../websocket/BoardChannelInterceptor.java`. ADR-004.
- **Critérios de aceite:**
  - Movimentação/criação/exclusão de card propaga evento STOMP a `/topic/board/{projetoId}` em <2s (RNF-001, validado com `Awaitility`).
  - Subscrição sem vínculo ao projeto rejeitada com `ERROR` STOMP.
  - Evento publicado por um pod é recebido por cliente conectado a outro pod (RNF-002 — teste completo fica em TASK-08.1, aqui validar com 1 pod).

---

### TASK-05.2 — Notificações internas [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-005
- **Dependências:** TASK-05.1, TASK-04.3
- **Contexto:** Endereça a motivação central do PRD — impedimento não visto a tempo.
- **O que deve ser feito:**
  - [ ] Ao mudar etapa (04.2) ou marcar/desmarcar impedimento (04.3), resolver lista de observadores (responsável + criador + `TarefaObservador`).
  - [ ] Criar uma `Notificacao` por observador (tipos `TRANSICAO_ETAPA`, `IMPEDIMENTO_MARCADO`, `IMPEDIMENTO_DESMARCADO`).
  - [ ] Publicar via `EventoBoardPublisher` em `/topic/notificacoes/{usuarioId}` (broadcast amplo + filtro client-side por `usuarioId`, conforme decisão da TechSpec Seção 5).
  - [ ] Implementar `GET /api/notificacoes` (lista de não lidas) e endpoint de marcar como lida (contrato `dashboard-notificacoes.md`).
  - [ ] Implementar CRUD de `TarefaObservador` (adicionar/remover observador explícito).
- **Guia técnico:** `backend/src/main/java/.../notificacao/`. Contrato: `dashboard-notificacoes.md`.
- **Critérios de aceite:**
  - Alteração de etapa/impedimento gera `Notificacao` para responsável + criador + observadores explícitos.
  - Notificação chega ao cliente correto independentemente do pod que processou o evento (validação completa em 08.1).

---

### TASK-05.3 — Resiliência: reconexão do listener, resincronização client, health-check, métricas [M]

- **Sistema:** CRUDAO
- **RF de origem:** RNF-001, RNF-002
- **Dependências:** TASK-05.1
- **Contexto:** Mitiga o trade-off aceito de LISTEN/NOTIFY (sem replay de eventos perdidos, payload limitado a 8KB) — rede de segurança operacional.
- **O que deve ser feito:**
  - [ ] Implementar reconexão automática com backoff da conexão JDBC do listener em caso de queda.
  - [ ] Incluir `seq` incremental no payload do evento; frontend detecta gap ou reconexão de WebSocket e refaz `GET /board` para resincronizar (contrato client-side documentado aqui, implementação de frontend em 07.2).
  - [ ] Readiness probe (Actuator) reflete listener desconectado como não saudável.
  - [ ] Métricas Micrometer: contador de reconexões do listener por pod, latência entre `NOTIFY` e broadcast STOMP.
  - [ ] Logs progressivos (`WARN`→`ERROR`) em falha de reconexão.
- **Guia técnico:** `backend/src/main/java/.../evento/adapter/ListenNotifyPublisher.java` (extensão), `.../health/ListenerHealthIndicator.java`, `.../metrics/`.
- **Critérios de aceite:**
  - Kill da conexão JDBC do listener em execução → reconecta com backoff; próximo `NOTIFY` ainda propaga.
  - Readiness probe reporta não saudável durante desconexão.
  - Métricas de reconexão e latência disponíveis via Actuator.

---

## EPIC 06 — Dashboard

### TASK-06.1 — Migration V7 + agregação de lead-time médio [G]

- **Sistema:** CRUDAO
- **RF de origem:** RF-007
- **Dependências:** TASK-04.5
- **Contexto:** Visibilidade para gestores sem necessidade de acompanhar a execução diretamente — segundo objetivo central do PRD.
- **O que deve ser feito:**
  - [ ] Criar migration V7 (Notificacao). **Não criar V8** — pertence exclusivamente a TASK-02.3.
  - [ ] Implementar `GET /api/projetos/{projetoId}/dashboard` (contrato `dashboard-notificacoes.md`): lead-time médio por etapa + tempo médio de impedimento agregado (RN-001, RN-002), a partir de `TarefaEtapaHistorico`/`TarefaImpedimentoHistorico`, usando os índices já criados em `data-model.md`.
  - [ ] Garantir acessibilidade do dashboard mesmo com projeto finalizado (RN-015 — leitura permitida).
- **Guia técnico:** `backend/src/main/java/.../dashboard/DashboardService.java`. Contrato: `dashboard-notificacoes.md`.
- **Critérios de aceite:**
  - Dashboard agrega lead-time médio corretamente com histórico de múltiplas tarefas/etapas (dataset controlado de teste).
  - Tempo médio de impedimento agregado calculado corretamente.
  - Acessível com projeto finalizado.

---

## EPIC 07 — Frontend

### TASK-07.1 — Shell Next.js + autenticação [G]

- **Sistema:** CRUDAO
- **RF de origem:** RF-014
- **Dependências:** TASK-01.1, TASK-02.1
- **Contexto:** Base de navegação e sessão para todas as demais telas.
- **O que deve ser feito:**
  - [ ] Implementar fluxo de login (redirect a `/oauth2/authorization/keycloak`), consumo de `GET /api/me`, logout.
  - [ ] Implementar guarda de rota (redireciona não autenticado ao login).
  - [ ] Implementar shell de navegação (lista de projetos do usuário, acesso a board/dashboard/admin conforme permissões retornadas por `/api/me`).
  - [ ] Aplicar tokens visuais do Design Brief (`docs/design/kanban-tarefas-design-brief.md`).
  - [ ] Garantir responsividade desktop do shell (RNF-005, achado do Comitê — QA, sem task dona na primeira versão).
- **Guia técnico:** `frontend/app/`, `frontend/lib/auth.ts`. Referência visual: protótipo `docs/design/kanban-tarefas/prototypes/tl-01-login.html`.
- **Critérios de aceite:**
  - Login/logout funcionam de ponta a ponta contra o ambiente dev (01.1).
  - Usuário não autenticado é redirecionado ao login em qualquer rota protegida.
  - Menu reflete apenas ações permitidas ao usuário (validação real permanece no backend — RNF-003).

---

### TASK-07.2 — Board: colunas, raias, cards, criar/excluir, mover [G]

- **Sistema:** CRUDAO
- **RF de origem:** RF-001, RF-002, RF-004, RF-011, RF-018, RF-019
- **Dependências:** TASK-07.1, TASK-04.5, TASK-05.1
- **Contexto:** Tela central do sistema.
- **O que deve ser feito:**
  - [ ] Renderizar board a partir de `GET /board` (colunas na ordem configurada, tarefas agrupadas por raia).
  - [ ] Implementar criação de card ("Novo card" na etapa de menor ordem) e exclusão de card.
  - [ ] Implementar movimentação (drag-and-drop ou ação equivalente) chamando `POST /mover`, com feedback de erro quando transição bloqueada.
  - [ ] Implementar indicador visual de impedimento e ação marcar/desmarcar.
  - [ ] Conectar ao STOMP `/topic/board/{projetoId}` para atualização em tempo real; implementar resincronização por gap de `seq` (conforme 05.3).
- **Guia técnico:** `frontend/app/projetos/[id]/board/`. Referência visual: `docs/design/kanban-tarefas/screen-map.md`.
- **Critérios de aceite:**
  - Board reflete estado do backend e se atualiza em tempo real sem refresh manual (<2s, RNF-001) quando outro usuário move/cria/exclui um card.
  - Transição bloqueada exibe mensagem de erro clara.
  - Reconexão de WebSocket dispara resincronização via `GET /board`.

---

### TASK-07.3 — Detalhe da tarefa [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-003, RF-006, RF-017
- **Dependências:** TASK-07.2, TASK-04.5
- **Contexto:** Visão detalhada por card — lead-time por etapa, histórico de auditoria, edição de campos.
- **O que deve ser feito:**
  - [ ] Exibir lead-time por etapa e tempo total de impedimento acumulado (RF-006).
  - [ ] Exibir histórico de auditoria (RF-017).
  - [ ] Formulário de edição respeitando congelamento pós-início (campos estruturais desabilitados quando `iniciada=true`).
  - [ ] Gerenciar observadores explícitos (adicionar/remover).
- **Guia técnico:** `frontend/app/projetos/[id]/tarefas/[tarefaId]/`.
- **Critérios de aceite:**
  - Lead-time exibido bate com o retornado pelo backend, incluindo etapa em andamento.
  - Campos estruturais aparecem desabilitados/bloqueados quando tarefa iniciada.
  - Histórico de auditoria exibido em ordem cronológica.

---

### TASK-07.4 — Admin: projeto/workflow/etapa/transição/raia [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-008, RF-009, RF-010, RF-011
- **Dependências:** TASK-07.1, TASK-03.1, TASK-03.2, TASK-03.3
- **[P] com:** TASK-07.5, TASK-07.6, TASK-07.7
- **Contexto:** Configuração do board por projeto.
- **O que deve ser feito:**
  - [ ] Telas de CRUD de projeto (incl. finalizar/reabrir), workflow, etapa (com reordenação), transição, raia.
  - [ ] Validação de UX espelhando RN-003/RN-005 (com revalidação sempre no backend).
- **Guia técnico:** `frontend/app/projetos/[id]/admin/`.
- **Critérios de aceite:**
  - Todas as operações de CRUD refletem corretamente as respostas de erro do backend (403, 422, bloqueio por tarefas ativas).

---

### TASK-07.5 — Admin: papéis/permissões/usuários [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-013, RF-015, RF-016
- **Dependências:** TASK-07.1, TASK-02.3
- **[P] com:** TASK-07.4, TASK-07.6, TASK-07.7
- **Contexto:** Tela administrativa de RBAC configurável.
- **O que deve ser feito:**
  - [ ] Tela de gestão de papéis por projeto (exceto `admin`, somente leitura/protegido).
  - [ ] Tela de toggles de `PapelPermissao`.
  - [ ] Tela de associação usuário↔projeto↔papel.
  - [ ] Feedback claro quando bloqueado por RN-017 (autoconcessão).
- **Guia técnico:** `frontend/app/projetos/[id]/admin/papeis/`.
- **Critérios de aceite:**
  - Toggle desabilitado bloqueia ação correspondente na UI e reflete erro real do backend.
  - Tentativa de alterar permissão do próprio papel exibe mensagem clara (RN-017).

---

### TASK-07.6 — Dashboard UI [M]

- **Sistema:** CRUDAO
- **RF de origem:** RF-007
- **Dependências:** TASK-07.1, TASK-06.1
- **[P] com:** TASK-07.4, TASK-07.5, TASK-07.7
- **Contexto:** Visão de gestão sem necessidade de acesso de execução.
- **O que deve ser feito:**
  - [ ] Renderizar lead-time médio por etapa e tempo médio de impedimento agregado a partir de `GET /dashboard`.
  - [ ] Garantir acesso mesmo para papel `gestor` (sem `tarefa:gerenciar`/execução).
- **Guia técnico:** `frontend/app/projetos/[id]/dashboard/`.
- **Critérios de aceite:**
  - Dashboard acessível a gestor sem permissões de execução.
  - Dados batem com o retornado pelo backend em dataset de teste.

---

### TASK-07.7 — Notificações UI [P]

- **Sistema:** CRUDAO
- **RF de origem:** RF-005
- **Dependências:** TASK-07.1, TASK-05.2
- **[P] com:** TASK-07.4, TASK-07.5, TASK-07.6
- **Contexto:** Fecha o ciclo de visibilidade de impedimento — objetivo central do PRD.
- **O que deve ser feito:**
  - [ ] Lista de notificações não lidas, conectada a `/topic/notificacoes/{usuarioId}` (com filtro client-side por `usuarioId`, conforme decisão da TechSpec).
  - [ ] Ação de marcar como lida.
- **Guia técnico:** `frontend/components/notificacoes/`.
- **Critérios de aceite:**
  - Notificação aparece em tempo real quando o usuário é observador de uma tarefa alterada.
  - Marcar como lida reflete no backend e na UI.

---

## EPIC 08 — Hardening

### TASK-08.1 — Testes multi-pod e WebSocket (RNF-001/RNF-002) [M]

- **Sistema:** CRUDAO
- **RF de origem:** RNF-001, RNF-002
- **Dependências:** TASK-05.3, TASK-05.2
- **Contexto:** Validação formal do requisito de escalabilidade horizontal sem inconsistência.
- **O que deve ser feito:**
  - [ ] Teste de integração com 2 instâncias Spring Boot compartilhando o mesmo PostgreSQL Testcontainer — evento publicado via pod A deve chegar ao cliente STOMP conectado ao pod B.
  - [ ] Teste de integração com 2 conexões WebSocket simuladas validando propagação do evento em <2s (`Awaitility`).
  - [ ] Teste de resincronização client-side por gap de `seq`.
  - [ ] Teste de notificação multi-pod (RF-005 sob RNF-002, achado do Comitê — Architect: dependência de 05.2 estava implícita).
- **Guia técnico:** `backend/src/test/java/.../multipod/`.
- **Critérios de aceite:** 0 falhas em 10 execuções consecutivas locais (amostra fixa, achado do Comitê — QA substitui percentual vago de flakiness).

---

### TASK-08.2 — Observabilidade final [P]

- **Sistema:** CRUDAO
- **RF de origem:** RNF-002, RNF-004
- **Dependências:** TASK-05.3
- **[P] com:** TASK-08.1
- **Contexto:** Fecha os requisitos de `observability.md` não cobertos incrementalmente pelas tasks anteriores.
- **O que deve ser feito:**
  - [ ] Confirmar logging em arquivo local (rotação 5MB, retenção 10 arquivos).
  - [ ] Confirmar métricas mínimas via Actuator/Micrometer completas (reconexões, latência NOTIFY→STOMP).
  - [ ] Produzir stub de runbook operacional de indisponibilidade do Keycloak (referenciado na TechSpec como pré-requisito de go-live, fora do escopo funcional).
- **Guia técnico:** `backend/src/main/resources/logback-spring.xml`; `docs/runbooks/keycloak-indisponivel.md` (novo).
- **Critérios de aceite:**
  - Logs rotacionam conforme especificado.
  - Métricas visíveis via `/actuator/metrics`.
  - Runbook stub existe e cobre: sintoma, verificação, escalonamento.

---

### TASK-08.3 — Dockerização de backend e frontend (RNF-004, ADR-008) [M]

- **Sistema:** CRUDAO
- **RF de origem:** RNF-004
- **Dependências:** nenhuma (não depende de código funcional — só empacota o que já existe)
- **[P] com:** TASK-08.1, TASK-08.2
- **Contexto:** Débito técnico registrado desde a TechSpec inicial (RNF-004/`stack.md` já previam containerização, mas `docker-compose.yml` só subia infra `postgres`/`keycloak` — backend/frontend continuavam rodando local via `mvnw spring-boot:run`/`npm run dev`). Formalizado como requisito explícito pelo usuário em 2026-08-26 (ADR-008): homologação deve ser possível com `docker compose up -d` único, sem instalar JDK 25/Node na máquina de quem valida.
- **O que deve ser feito:**
  - [ ] Criar `backend/Dockerfile` multi-stage: stage de build `maven:3.9-eclipse-temurin-25` (`mvn dependency:go-offline` antes de copiar o código-fonte, para cache de camada), stage de runtime `eclipse-temurin:25-jre` copiando só o jar final, rodando como usuário não-root.
  - [ ] Criar `frontend/Dockerfile` multi-stage: stage de build `node:20-alpine` (`npm ci && npm run build`), stage de runtime enxuto servindo o build (avaliar `output: standalone` do Next.js para reduzir a imagem final).
  - [ ] Ajustar `docker-compose.yml` (`systems/CRUDAO/`) — os serviços `backend`/`frontend` já foram esboçados na TechSpec v1.1 (build context, portas 8081/3000, `depends_on` com healthcheck do Postgres/Keycloak); conferir e corrigir os nomes reais de variável de ambiente (`SPRING_DATASOURCE_*`, `SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_ISSUER-URI` — validar contra `application.yml`/`application-dev.yml`) e as variáveis do frontend (`NEXT_PUBLIC_BACKEND_URL`, `SESSION_SECRET`, URL interna do Keycloak) contra `.env.local.example`.
  - [ ] Validar que o backend, dentro do container, resolve `postgres`/`keycloak` pelo nome do serviço na rede do compose (não `localhost`) — e que o frontend, no browser, ainda usa `localhost:8081`/`localhost:8080` (URLs client-side não resolvem nome de serviço Docker).
  - [ ] `docker compose up -d` de ponta a ponta: subir a stack completa do zero (sem volumes/imagens pré-existentes) e confirmar login OIDC + board funcionando via `http://localhost:3000`.
  - [ ] Atualizar `README.md`/`quickstart.md` se o comportamento real divergir do que já foi documentado na TechSpec v1.1.
- **Guia técnico:** `backend/Dockerfile` (novo), `frontend/Dockerfile` (novo), `docker-compose.yml` (ajustar serviços já esboçados). Seguir a convenção de multi-stage já documentada em `guidelines/stack.md`. Referência de decisão: [ADR-008](../decisions/ADR-008-dockerizacao-backend-frontend.md).
- **Critérios de aceite:**
  - `docker compose up -d` (sem nenhum outro comando) sobe `postgres`, `keycloak`, `backend`, `frontend` e a aplicação fica utilizável em `http://localhost:3000` — login, board, mover card.
  - Nenhuma credencial/URL sensível hardcoded na imagem — tudo via `environment:`/`.env` do compose.
  - Backend continua aplicando migrations Flyway automaticamente no boot do container (sem mudança de comportamento, ADR-005).
  - Setup local sem Docker (`mvnw spring-boot:run`/`npm run dev` apontando para `docker compose up -d postgres keycloak`) continua funcionando, sem regressão para quem desenvolve ativamente.

---

## Backlog Priorizado (ordem de início recomendada)

1. TASK-01.1 → TASK-01.2 (infra base — bloqueante de tudo)
2. TASK-02.1 → TASK-02.2 (motor RBAC — bloqueante de todo endpoint de escrita); TASK-02.3 (CRUD papéis) pode seguir em paralelo às demais frentes, só bloqueia TASK-07.5
3. TASK-03.1 / TASK-03.2 / TASK-03.3 em paralelo (configuração de board)
4. TASK-04.1 (fecha RN-005 real) → TASK-04.2 (inclui RN-012) / TASK-04.3 / TASK-04.4 (depende também de 02.3) em paralelo → TASK-04.5 (core de tarefas)
5. TASK-05.1 (depende de 04.1/04.2/04.4/04.5) → TASK-05.2 (depende também de 04.3) / TASK-05.3 (tempo real e notificações)
6. TASK-06.1 (dashboard, sem V8; pode iniciar em paralelo a Epic 05 assim que 04.5 concluída)
7. TASK-07.1 (inclui RNF-005) → TASK-07.2 → TASK-07.3; TASK-07.4 / TASK-07.5 (depende de 02.3) / TASK-07.6 / TASK-07.7 em paralelo assim que suas dependências de backend estiverem prontas
8. TASK-08.1 (depende também de 05.2) / TASK-08.2 / TASK-08.3 em paralelo (hardening final, antes de release)

---

## Revisão do Comitê de Análise Assíncrono (Architect + QA, 2026-08-25)

Executada conforme Fase 3.5 da skill, sobre os 24 arquivos originalmente gerados. Achados aplicados nesta versão:

**Críticos (Architect/QA):**
- Migration V8 (`PapelPermissaoAuditoria`) disputada por 3 tasks sem dono único → fixada em TASK-02.3 (nova, destacada de TASK-02.2).
- RN-005 (bloqueio de exclusão com tarefas ativas) virava stub permanente em 03.2/03.3 sem task de fechamento rastreável → TASK-04.1 agora implementa a checagem real de forma obrigatória (não opcional).
- RN-012 (autoatribuição/reatribuição de responsável) sem task dona nem critério de aceite → adicionada a TASK-04.2.

**Alto:**
- TDD obrigatório da TechSpec para "resolução de permissões" ausente na task correspondente → adicionado a TASK-02.2.

**Médios (aplicados):**
- TASK-02.2 misturava 4 subsistemas e bloqueava paralelismo de 03.x além do necessário → desmembrada em TASK-02.2 (motor+guard) e TASK-02.3 (CRUD).
- TASK-04.4 misturava exclusão + auditoria + decisão de design em aberto → decisão de modelagem de `devPodeExcluirTarefa` fechada (permissão dedicada `tarefa:excluir`).
- RNF-005 (responsividade desktop) sem task dona → adicionada a TASK-07.1.
- Critérios de aceite com dois desfechos válidos (03.2/03.3) e "flakiness > 1%" sem amostra definida (08.1) → ambos fixados com decisão única/amostra fixa.
- Arestas do grafo omitidas (05.1 depende também de 04.1/04.2/04.4; 05.2 de 04.3; 08.1 de 05.2) → grafo e dependências corrigidos.

**Não aplicado nesta rodada (registrado, não bloqueante):** rastreabilidade explícita a cenários Gherkin do PRD por task (achado QA #6) — cobertura fica a cargo de `/tests`, que já lê a TechSpec Seção 7 diretamente.

---

## Histórico de Revisões

| Versão | Data | Autor | Alteração |
|---|---|---|---|
| 1.0 | 2026-08-25 | Thiago Goncalves Cavalcante | Versão inicial — 24 tasks em 8 epics; revisada pelo Comitê de Análise (Architect+QA) no mesmo dia — TASK-02.2 desmembrada em 02.2+02.3, correções de RN-005/RN-012/TDD/RNF-005/grafo — total final 25 tasks |
| 1.1 | 2026-08-26 | Thiago Goncalves Cavalcante | TASK-08.3 adicionada (dockerização de backend/frontend, RNF-004/ADR-008) — débito técnico registrado a pedido do usuário; total 26 tasks |
