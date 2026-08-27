---
id: BDR-001
type: BDR
status: accepted
date: 2026-08-23
supersedes: —
superseded-by: —
---

# BDR-001 — RBAC por projeto com papéis acumuláveis

## Decisão

O papel `admin` continua global (sem vínculo de projeto, acesso total ao sistema). Os demais papéis (`project_admin`, `product_owner`, `dev`, `gestor`, e o legado `user`) passam a ser atribuídos por par (usuário, projeto): um usuário pode ter mais de um papel no mesmo projeto (permissões acumuladas) e papéis diferentes em projetos diferentes. `project_admin` administra usuários do seu projeto associando-os a papéis já existentes, mas não cria/edita papéis nem permissões — isso é exclusivo do admin global (RF-013).

## Motivação

O modelo anterior (Usuário→Papel único global, TASK-04.1) não suporta a necessidade real de negócio: uma pessoa pode ser `project_admin` de um projeto e `dev` de outro, ou acumular `dev` + `product_owner` no mesmo projeto. Sem escopo por projeto, qualquer permissão concedida vale para todos os projetos do sistema — o que a G-RT-01 (canvas, code review da TASK-05.1) já havia sinalizado como lacuna.

**Problema que resolve:**
Permitir controle de acesso realista por projeto, sem exigir um papel global por combinação de responsabilidade.

**Restrições consideradas:**
- RN-006 (papel `admin` protegido) continua valendo.
- RNF-003 reforçada: toda checagem de permissão exibida no frontend precisa ser revalidada no backend — nenhuma autorização pode depender de dado enviado pelo cliente.
- Papel `user` (legado) mantido coexistindo como fallback sem permissões (RN-014), para não quebrar o seed/autoprovisionamento existente (TASK-04.1).

## Consequências

**Positivas:**
- Modelo de permissão alinhado à forma real como as equipes trabalham (multi-projeto, múltiplas responsabilidades).
- Não introduz uma segunda trilha de autorização (sem RBAC granular customizável por projeto) — mantém consistência com o mecanismo de permissão existente (`@ExigePermissao`), só adiciona escopo de projeto e um conjunto fechado de toggles pré-definidos (RF-016) para os casos de variação de comportamento default já identificados.

**Negativas / trade-offs:**
- Expande o modelo de dados de RBAC (TASK-04.1 precisa de retrabalho): tabela de associação usuário↔projeto↔papel substitui o vínculo direto usuário→papel único.
- Toda checagem de permissão existente (`@ExigePermissao`) precisa passar a considerar o projeto da requisição, não só o papel global do usuário — retrabalho em todos os controllers de CRUD administrativo (Projeto, Workflow, Etapa, Transição, Raia) e no board (Tarefa).
- Endpoint novo necessário para o frontend saber as permissões do usuário atual por projeto (`GET /api/usuarios/me` ou equivalente) — não existia antes.

**Downstream afetado:**
- TechSpec: novo modelo de dados (Usuario↔Projeto↔Papel), nova permissão `tarefa:finalizar`, entidade de toggles por projeto, entidade de auditoria de tarefa.
- Tasks: TASK-04.1 (RBAC) precisa de task de retrabalho/extensão antes da TASK-05.3 (painel de administração) poder ser reimplementada com fidelidade ao PRD v1.2.
- Canvas: dimensão S (Safeguards) — G-RT-01 deixa de ser só uma nota, passa a ser resolvida por este BDR.

## Alternativas Consideradas

### Alternativa 1 — RBAC granular totalmente configurável por projeto (project_admin monta permissão×papel livremente)
**Descartada porque:** cria uma segunda camada de autorização paralela ao catálogo de papéis global, aumentando muito a complexidade de implementação e o modelo mental do usuário, sem um caso de uso concreto que justifique essa flexibilidade total — os casos concretos levantados (edição de tarefa iniciada, visibilidade do board para gestor) foram resolvidos com o conjunto fechado de toggles (RF-016).

### Alternativa 2 — Manter papel único global por usuário (modelo atual da TASK-04.1)
**Descartada porque:** não representa a realidade de um usuário atuar com responsabilidades diferentes em projetos diferentes, e não restringe corretamente project_admin a "seu" projeto — permissão concedida a um papel vale para o sistema inteiro.
