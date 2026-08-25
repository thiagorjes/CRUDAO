# TASK-07.1 — Shell Next.js + autenticação

**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RF-014
**Dependências:** TASK-01.1, TASK-02.1
**Paralelismo:** nenhum (bloqueante de todo o frontend)

## Contexto

Base de navegação e sessão para todas as demais telas.

## O que deve ser feito

- [ ] Implementar fluxo de login (redirect a `/oauth2/authorization/keycloak`), consumo de `GET /api/me`, logout.
- [ ] Implementar guarda de rota (redireciona não autenticado ao login).
- [ ] Implementar shell de navegação (lista de projetos do usuário, acesso a board/dashboard/admin conforme permissões retornadas por `/api/me`).
- [ ] Aplicar tokens visuais do Design Brief.
- [ ] Garantir responsividade desktop do shell (RNF-005): layout correto nas resoluções/navegadores desktop principais da equipe — base para todas as telas subsequentes de Epic 07.

## Guia técnico

- `frontend/app/` — rotas, layout raiz.
- `frontend/lib/auth.ts` — integração OIDC.
- Referência visual: `docs/design/kanban-tarefas-design-brief.md`, protótipo `docs/design/kanban-tarefas/prototypes/tl-01-login.html`.

## Critérios de aceite

- Login/logout funcionam de ponta a ponta contra o ambiente dev (TASK-01.1).
- Usuário não autenticado é redirecionado ao login em qualquer rota protegida.
- Menu reflete apenas ações permitidas ao usuário (validação real permanece no backend — RNF-003).
- Shell renderiza corretamente nos principais navegadores desktop da equipe (RNF-005) — sem quebra de layout nas resoluções desktop usuais.
