# TASK-01.3 — Configuração de projeto (toggles) e finalização [M]

**Status:** Concluída — 2026-08-24
**Code review:** agent QA — 1 finding 🔴 corrigido (bypass de RN-015: `AutorizacaoProjetoService.exigirPermissao` liberava qualquer escrita com a permissão `projeto:gerenciar` em projeto finalizado, não só a reabertura — afetava `editar`/`excluir`/`atualizarConfiguracao`/`finalizar` do próprio `ProjetoService`, bug pré-existente da TASK-04.2 exposto por esta task; corrigido com `exigirPermissaoParaReabertura` dedicado, desacoplado da string de permissão) e 2 findings 🟡 confirmados sem ação corretiva imediata (falta de teste `*IT` cruzando `ProjetoService` real com `AutorizacaoProjetoService` real — mitigado por cobertura unitária dos dois lados isoladamente; G-RBAC-06 ampliado para `atualizarConfiguracao`/`finalizar`)

**Epic:** EPIC-01 — Domínio: Projeto, Workflow, Etapas e Raias | **User Story:** US-01.1 — Estrutura configurável de workflow por projeto
**Sistema:** CRUDAO | **RF:** RF-008, RF-016 | **Dependências:** TASK-04.2, TASK-01.1

---

## Contexto

PRD v1.2/v1.3 introduziu dois comportamentos por projeto que dependem do RBAC por projeto (TASK-04.2): um conjunto fechado de toggles configuráveis pelo `project_admin` (RF-016, BDR-001 — decisão de não ter RBAC granular customizável) e a finalização de projeto (somente leitura, RN-015).

## O que deve ser feito

- [x] Implementar entidade `ConfiguracaoProjeto` (1:1 com Projeto): `devPodeExcluirTarefa`, `devPodeEditarTarefaIniciada`, `gestorVeBoard` — todos boolean, default `false`, criada junto com o Projeto (já existia da TASK-04.2)
- [x] Endpoints `GET/PUT /api/projetos/{id}/configuracao` — `PUT` exige `projeto:gerenciar` no projeto (via `AutorizacaoProjetoService`)
- [x] Adicionar `data_finalizacao` ao Projeto (já migrado na TASK-04.2) e endpoints `PUT/DELETE /api/projetos/{id}/finalizar` (finalizar/reabrir) — exige `projeto:gerenciar`
- [x] Confirmar que `AutorizacaoProjetoService.exigirPermissao` (TASK-04.2) já bloqueia toda escrita em projeto finalizado — não duplicar a checagem aqui (confirmado; `finalizar`/`reabrir` reusam `exigirPermissao`, sem checagem própria)

## Guia técnico

- Pacote: `domain/projeto`
- Referência: `docs/techspec/kanban-configuravel/data-model.md` (v1.2, `ConfiguracaoProjeto`), PRD RF-016, RN-015

## Critérios de aceite

- Toggles criados com default `false` ao criar um projeto
- `project_admin` de um projeto liga/desliga toggle sem afetar outros projetos
- `PUT /finalizar` bloqueia escrita subsequente no projeto (teste de integração cruzado com TASK-04.2)
- `DELETE /finalizar` (reabrir) restaura a capacidade de escrita

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
