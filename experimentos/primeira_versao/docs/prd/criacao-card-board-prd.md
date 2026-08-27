# PRD — Criação e gerenciamento de cards no board
_Versão: 1.0 | Status: Draft | Data: 2026-08-24 | Autor: Thiago Goncalves Cavalcante_

---

## 1. Visão Geral

**Problema:** o board do CRUDAO permite visualizar e movimentar cards (tarefas), mas não existe forma de criar um novo card pela interface — a criação só é possível via chamada direta à API. A criação de tarefa pela UI ficou explicitamente fora do escopo da TASK-05.1 (board principal). Também não existe ação de exclusão de card pela UI.

**Solução proposta:** adicionar um botão "Novo card" no board (visível só a quem tem `tarefa:gerenciar` no projeto), abrindo um formulário com os campos da entidade (título, descrição, tipo e demais atributos), criando por padrão na raia e etapa de referência ("coluna 0"/backlog). Adicionar um ícone de lixeira no card, visível ao mesmo público, que abre um modal de confirmação e executa a exclusão.

**Público-alvo:** usuários com a permissão `tarefa:gerenciar` no projeto (`project_admin`, `dev`-tier — modelo RBAC por projeto do BDR-001).

---

## 2. Stakeholders

| Papel | Nome | Responsabilidade |
|-------|------|-----------------|
| Product Owner | Thiago Goncalves Cavalcante | Aprovação de requisitos |
| Tech Lead | Thiago Goncalves Cavalcante | Decisões técnicas |
| Usuário final | Membros de projeto com `tarefa:gerenciar` | Validação de UX |

---

## 3. Requisitos Funcionais

### RF-001 — Criar card pelo board

**Como** usuário com `tarefa:gerenciar` no projeto, **quero** criar um novo card diretamente pelo board, **para** popular e manter o board sem depender de chamada direta à API.

**Critérios de aceite:**

**Dado que** estou no board de um projeto e tenho a permissão `tarefa:gerenciar` nele
**Quando** clico em "Novo card", preencho título (obrigatório), descrição, tipo (`FEATURE`/`BUG`/`CHORE`) e demais atributos disponíveis, e salvo
**Então** o card é criado via `POST /api/tarefas`, aparece imediatamente no board (sem reload) na raia e etapa padrão (etapa de menor `ordem` do workflow do projeto — "coluna 0"/backlog; primeira raia do projeto, ou raia default global se o projeto não tiver raia própria), sem responsável, e um feedback de sucesso é exibido

**Dado que** não tenho a permissão `tarefa:gerenciar` no projeto
**Quando** visualizo o board
**Então** o botão "Novo card" não é exibido

**Dado que** o projeto está finalizado (RN-015)
**Quando** tento criar um card
**Então** a ação é bloqueada pelo backend (`AutorizacaoProjetoService`), consistente com o restante do board

**Prioridade:** Must Have

---

### RF-002 — Excluir card pelo board

**Como** usuário com `tarefa:gerenciar` no projeto, **quero** excluir um card diretamente pelo board, **para** gerenciar completamente o ciclo de vida do card sem sair da ferramenta.

**Critérios de aceite:**

**Dado que** tenho a permissão `tarefa:gerenciar` no projeto (e, se for `dev`-tier, o toggle `devPodeExcluirTarefa` está habilitado)
**Quando** clico no ícone de lixeira do card
**Então** um modal de confirmação é exibido

**Dado que** o modal de confirmação está aberto
**Quando** confirmo a exclusão
**Então** o card é removido via `DELETE /api/tarefas/{id}` e desaparece do board

**Dado que** não tenho a permissão (ou sou `dev`-tier com o toggle desabilitado)
**Quando** visualizo o card
**Então** o ícone de lixeira não é exibido

**Prioridade:** Must Have

---

## 4. Requisitos Não-Funcionais

Nenhum requisito não-funcional adicional identificado nesta entrevista — a feature herda RNF-003 (RBAC revalidado sempre no backend, gating de UI é só estético) já vigente no sistema. Ver nota técnica na Seção 7 sobre tempo real na exclusão.

---

## 5. Regras de Negócio

| ID | Regra | Origem |
|----|-------|--------|
| RN-001 | Criação e exclusão de card exigem a permissão `tarefa:gerenciar` no projeto, avaliada por `AutorizacaoProjetoService` (já implementado) | Discovery / código existente |
| RN-002 | Exclusão por usuário `dev`-tier exige adicionalmente o toggle de projeto `devPodeExcluirTarefa` habilitado (já implementado) | Código existente (TASK-02.3) |
| RN-003 | Criação e exclusão de card são bloqueadas se o projeto estiver finalizado (RN-015 do kanban-configuravel) | Discovery |
| RN-004 | Card criado sem responsável, se não informado no formulário | Entrevista |
| RN-005 | Card criado por padrão na etapa de menor `ordem` do workflow do projeto ("coluna 0") e na primeira raia do projeto (ou raia default global, se o projeto não tiver raia própria) | Entrevista |

---

## 6. Casos de Uso

### UC-001 — Criar card

**Ator:** Usuário com `tarefa:gerenciar`
**Fluxo principal:**
1. Usuário clica em "Novo card" no board
2. Preenche título, descrição, tipo e demais atributos no formulário
3. Salva — card é criado na etapa/raia padrão e aparece no board imediatamente

**Fluxo alternativo:** título não preenchido — formulário bloqueia o envio e sinaliza o campo obrigatório.

### UC-002 — Excluir card

**Ator:** Usuário com `tarefa:gerenciar` (e toggle habilitado, se `dev`-tier)
**Fluxo principal:**
1. Usuário clica no ícone de lixeira no card
2. Confirma a exclusão no modal
3. Card é removido do board

**Fluxo alternativo:** usuário cancela o modal — nenhuma alteração é feita.

---

## 7. Restrições e Premissas

**Restrições:**
- Backend já expõe `POST /api/tarefas` e `DELETE /api/tarefas/{id}` completos (RBAC + toggle já implementados desde TASK-02.1/02.3) — esta feature é escopo **somente frontend**, consumindo API existente.
- Gating de UI é só estético; backend revalida tudo (RNF-003/ADR-006).

**Premissas:**
- "Coluna 0" refere-se à etapa de menor `ordem` do workflow ativo do projeto.
- "Primeira raia" refere-se à raia de menor `ordem` do projeto (ou raia default global, se o projeto não tiver raia própria — mesmo padrão de fallback do `RaiaResolver`).

**Nota técnica (não é RNF, registrar para `/techspec`):** existe hoje o evento de broadcast `TAREFA_CRIADA` (tempo real via STOMP), mas não existe `TAREFA_EXCLUIDA` — a exclusão de card não notifica outros usuários conectados em tempo real. `/techspec` deve decidir se cria o evento ou aceita a lacuna (usuário não exigiu RNF de performance para isso nesta entrevista).

---

## 8. Dependências

| Dependência | Tipo | Impacto |
|-------------|------|---------|
| RBAC por projeto (TASK-04.2) | Técnica | Bloqueante — já concluída |
| CRUD de tarefa (TASK-02.1) e regras avançadas (TASK-02.3) | Técnica | Bloqueante — já concluída, endpoints prontos |
| Board principal (TASK-05.1) | Técnica | Bloqueante — já concluída |

---

## 9. Critérios de Sucesso (KPIs)

| KPI | Meta | Prazo |
|-----|------|-------|
| Criação de card pela UI | Usuário com `tarefa:gerenciar` consegue criar um card via board | — |
| Exclusão de card pela UI | Usuário com `tarefa:gerenciar` consegue excluir um card via board; usuário sem a permissão não vê a ação | — |

---

## 10. Fora do Escopo

- Importação em massa de cards
- Templates de card
- Duplicar card
- Anexos/arquivos
- Evento de broadcast em tempo real para exclusão (`TAREFA_EXCLUIDA`) — decisão adiada para `/techspec`

---

## 11. Histórico de Revisões

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-08-24 | Thiago Goncalves Cavalcante | Versão inicial |
