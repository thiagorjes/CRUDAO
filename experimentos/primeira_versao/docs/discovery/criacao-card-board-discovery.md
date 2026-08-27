# Discovery — Criação e gerenciamento de cards no board
_Data: 2026-08-24 | Facilitador: Claude (SSPDD)_

---

## 1. Problema

**Qual problema estamos resolvendo?**
Hoje o board do CRUDAO permite visualizar e movimentar cards (tarefas), mas não existe forma de criar um novo card pela interface — a criação só é possível via chamada direta à API. RF-003 (criação de tarefa) ficou explicitamente fora do escopo da TASK-05.1 (board principal).

**Para quem é o problema?**
Usuários com a permissão `tarefa:gerenciar` no projeto (papéis como `project_admin` e `dev`-tier, conforme modelo RBAC por projeto do BDR-001), que precisam popular e manter o board sem depender de acesso direto à API.

**Como sabemos que é um problema real?**
Evidência visual de uso do sistema (board vazio sem caminho de criação) e pendência já mapeada — RF-003 documentado como lacuna conhecida desde a TASK-05.1.

---

## 2. Personas

### Persona Principal: Usuário com `tarefa:gerenciar`

- **Perfil:** membro de projeto com papel que concede a permissão `tarefa:gerenciar` (ex: `project_admin`, `dev`-tier).
- **Objetivo principal:** criar, editar e excluir cards diretamente pelo board, sem sair da ferramenta.
- **Frustrações atuais:** não existe botão/fluxo de criação; card só pode ser criado via API.
- **Como usa a solução:** aciona "Novo card" no board, preenche formulário mínimo, e passa a gerenciar (editar/excluir) o card criado.

### Persona Secundária: Usuário sem `tarefa:gerenciar`

- **Perfil:** membro do projeto sem a permissão (ex: papel só de visualização/movimentação).
- **Objetivo:** continuar visualizando e usando o board normalmente, sem exposição a controles que não pode usar.
- **Restrição:** o botão/ação de criar (e excluir) card não deve ser visível para este perfil.

---

## 3. Objetivos de Negócio

| Objetivo | Métrica de sucesso | Prazo |
|----------|--------------------|-------|
| Permitir criação de card pelo board para quem tem `tarefa:gerenciar` | Validado na ferramenta: usuário com a permissão consegue criar um card via UI | — |
| Permitir gerenciamento completo do card (criar/editar/excluir) | Validado na ferramenta: usuário com a permissão consegue editar e excluir o card criado; usuário sem a permissão não vê a ação | — |

---

## 4. Hipótese de Solução

**Acreditamos que** adicionar um botão "Novo card" no board (visível só a quem tem `tarefa:gerenciar` no projeto), abrindo um formulário/modal com campos mínimos (título, descrição, raia, etapa inicial), e uma ação de exclusão de card (na tela de detalhe ou no próprio card) resolve a lacuna.

**Para** usuários com a permissão `tarefa:gerenciar`

**Resultará em** gerenciamento completo do ciclo de vida do card (criar/editar/excluir) sem sair do board, com edição já coberta desde TASK-02.1/05.4.

**Saberemos que funcionou quando** um usuário com a permissão adequada conseguir criar e excluir um card pela ferramenta, e um usuário sem a permissão não visualizar essas ações.

---

## 5. Contexto Adicional

### Restrições conhecidas
- Enforcement de permissão deve seguir o padrão RBAC por projeto (ADR-006, `AutorizacaoProjetoService`) — gating de UI é só estético, backend revalida (RNF-003).
- Projeto finalizado (RN-015) já bloqueia escrita — criação/exclusão de card deve respeitar essa regra.

### Dependências identificadas
- RBAC por projeto (TASK-04.2), CRUD de tarefa existente (TASK-02.1), board principal (TASK-05.1).
- Exclusão de card ainda não existe no backend (endpoint a confirmar em /techspec).

### O que está fora do escopo
- Importação em massa de cards.
- Templates de card.
- Duplicar card.
- Anexos/arquivos.

### Referências e materiais de apoio
- `memory/state.md` — histórico da feature `kanban-configuravel` (TASK-02.1, 05.1, 05.4).
