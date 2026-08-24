# REASONS Canvas — Criação e gerenciamento de cards no board
_Status: DRAFT | Idioma: pt_BR | Iniciado em: 2026-08-24_

> **Como usar:** cada skill do pipeline preenche suas dimensões ao concluir.
> Canvas transita de DRAFT → READY quando todas as 7 dimensões estiverem preenchidas.
> READY = canvas executável por outro agente de implementação.

---

## R — Requirements

_Atualizado por: /prd v1.0 — 2026-08-24_
> Decisões: —

Permitir que usuários com a permissão `tarefa:gerenciar` criem e excluam cards diretamente pelo board, fechando a lacuna de criação de tarefa deixada em aberto pela TASK-05.1. Usuários sem a permissão não devem ver os controles de criação/exclusão. Backend (`POST`/`DELETE /api/tarefas`) já existe e implementa RBAC + toggle `devPodeExcluirTarefa` — feature é escopo só frontend.

**RFs Must Have:**
- RF-001 — Criar card pelo board (título, descrição, tipo e demais atributos; etapa/raia padrão = "coluna 0"/primeira raia; sem responsável se não informado)
- RF-002 — Excluir card pelo board (ícone de lixeira no card + modal de confirmação)

**Escopo IN:**
- Botão "Novo card" no board, visível só a quem tem `tarefa:gerenciar` no projeto.
- Formulário/modal de criação (título, descrição, tipo e demais atributos da entidade Tarefa).
- Exclusão de card via ícone de lixeira + modal de confirmação, gated pela mesma permissão.

**Escopo OUT:**
- Importação em massa de cards.
- Templates de card.
- Duplicar card.
- Anexos/arquivos.
- Evento de broadcast em tempo real para exclusão (`TAREFA_EXCLUIDA`) — decisão adiada para `/techspec`.

---

## E — Entities

_Atualizado por: /designer v1.0 — 2026-08-24_
> Decisões: — (nenhum DDR novo; reaproveita DDR-001/002/003 de kanban-configuravel)

**Entidades de UX/UI (complementam as entidades de domínio já registradas em kanban-configuravel):**
- **Botão "Novo card":** posição fixa no header do board; visível só a quem tem `tarefa:gerenciar` no projeto (RF-001).
- **Modal "Novo card":** reaproveita padrão visual de Modal existente; campos título (obrigatório)/descrição/tipo/demais atributos; estados idle, erro de validação (título vazio), salvando (loading), sucesso (toast).
- **Ícone de lixeira no Card de Tarefa:** sempre visível (não só hover), gated pela mesma permissão + toggle `devPodeExcluirTarefa` para `dev`-tier (RF-002).
- **Modal de confirmação de exclusão:** reaproveita componente "Modal de Confirmação" já existente (variante ação destrutiva); estado loading no botão "Excluir".

**Tokens/componentes reaproveitados sem alteração:** `--color-primary`, `--color-error`, `--color-success`, tipografia Roboto, espaçamento base 8px, componentes Modal de Confirmação e Toast/Snackbar (ver `docs/design/kanban-configuravel-design-brief.md`).

---

## A — Approach

_Atualizado por: /techspec v1.0 — 2026-08-24_
> Decisões: —

**Estratégia de solução:**
Feature majoritariamente frontend: reutiliza `POST`/`DELETE /api/tarefas` (RBAC, toggle e bloqueio de projeto finalizado já implementados desde TASK-02.1/02.3/04.2) sem endpoint novo. Dois itens de backend, ambos achados pelo comitê de análise assíncrono (architect/security/database): (1) estender o broadcast já existente (ADR-004) com o tipo `TAREFA_EXCLUIDA`; (2) migrar `TarefaService.excluir` de hard-delete para **soft-delete** (`Tarefa.excluidaEm`) — o hard-delete quebraria com violação de FK em `RegistroEtapa`/`Impedimento`/`AuditoriaTarefa` (sem cascade) para qualquer tarefa com histórico, ou seja, praticamente toda tarefa. Etapa/raia padrão ("coluna 0"/primeira raia) são resolvidas no frontend a partir do estado já carregado pelo `BoardApp`.

**Trade-offs aceitos:**
- Soft-delete em vez de cascade-delete: preserva auditoria (RN-016), mas retém a linha e o histórico da tarefa excluída no banco indefinidamente — sem UI/API de restauração ou expurgo nesta feature.
- Graças ao soft-delete, `EventoBoardDTO` **não precisa** de campos nullable nem de caso especial no listener (a linha da tarefa continua existindo) — simplificação sobre uma versão anterior desta decisão.
- Se o workflow ativo não tiver etapas, não há "coluna 0" — botão "Novo card" fica desabilitado nesse estado.
- G-RT-01 (subscription STOMP sem checagem de membro do projeto) segue como débito herdado, não corrigido — só tem seu escopo ampliado por esta feature.

---

## S — Structure

_Atualizado por: /techspec v1.0 — 2026-08-24_
> Decisões: —

**Arquitetura:**
Nenhum endpoint REST novo. Componentes tocados: `Tarefa`/`TarefaRepository` (+ `excluidaEm`, soft-delete), `TarefaService.excluir` (soft-delete + publica `TAREFA_EXCLUIDA`), `TarefaService.listarPorProjeto`/`buscar` (filtram/rejeitam soft-deleted), `TipoEventoBoard` (novo valor de enum, `EventoBoardDTO` sem alteração de forma), `BoardApp`/`CardTarefa` (botão "Novo card", ícone de lixeira, gating via `GET /usuarios/me` + `GET /projetos/{id}/configuracao`), novos componentes `ModalNovoCard`/modal de confirmação de exclusão (reuso de padrão visual existente).

**Dependências externas:**
- Nenhuma nova. Reutiliza PostgreSQL LISTEN/NOTIFY (ADR-004) e o tópico STOMP `/topic/projetos/{id}/board` já existente (TASK-02.2/05.1).

---

## O — Operations

_Atualizado por: /tasks v1.0 — 2026-08-24_
> Decisões: —

**Tasks ordenadas por dependência:**
- [ ] TASK-01.1 — Backend: migrar exclusão para soft-delete e publicar evento `TAREFA_EXCLUIDA`
- [ ] TASK-02.1 [P] — Frontend: RBAC gating no BoardApp (permissão e toggle)
- [ ] TASK-02.2 [P] — Frontend: criar card pelo board (RF-001)
- [ ] TASK-02.3 [P] — Frontend: excluir card pelo board (RF-002)
- [ ] TASK-03.1 — Testes E2E de criação e exclusão de card

---

## N — Norms

_Atualizado por: /techspec v1.0 — 2026-08-24_
> Decisões: —

**Padrões relevantes para esta feature:**
- RNF-003/ADR-006 — gating de UI é só estético; toda autorização é revalidada no backend. Nenhuma exceção nesta feature.
- ADR-004 — broadcast multi-pod via PostgreSQL LISTEN/NOTIFY; evento novo (`TAREFA_EXCLUIDA`) segue o mesmo pipeline `afterCommit` → `pg_notify` → listener por pod → STOMP.
- `testing.md` — TDD 80%/BDD 100% dos cenários Gherkin do PRD; estender `RealtimeBoardIT` para o cenário de exclusão.
- `coding-standards.md` — MapStruct para DTO↔entidade (não se aplica aqui, nenhuma entidade nova); nomenclatura padrão Java/TypeScript já seguida nos arquivos reutilizados.

---

## S — Safeguards

_Atualizado por: /code-review v1.0 — {{DATE}}_
> Decisões: —

**Restrições:**
- {{RESTRICAO_1}}

**O que NÃO fazer:**
- {{NAO_FAZER_1}}
