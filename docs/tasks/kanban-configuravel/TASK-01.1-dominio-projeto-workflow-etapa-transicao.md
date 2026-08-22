# TASK-01.1 — Modelo de domínio: Projeto, Workflow, Etapa e Transição [G]

**Epic:** EPIC-01 — Domínio: Projeto, Workflow, Etapas e Raias | **User Story:** US-01.1 — Estrutura configurável de workflow por projeto
**Sistema:** CRUDAO | **RF:** RF-008, RF-009, RF-010, RF-002 | **Dependências:** TASK-00.2

---

## Contexto

Base do sistema: projetos configuráveis com workflows próprios, etapas ordenadas e transições que definem o que é permitido mover (RF-002, engine central do produto).

## O que deve ser feito

- [ ] Implementar entidades Projeto, Workflow, Etapa, Transição (ver `docs/techspec/kanban-configuravel/data-model.md`)
- [ ] CRUD de Projeto (RF-008), com bloqueio de exclusão se houver tarefas ativas (RN-005)
- [ ] CRUD de Workflow por projeto (RF-009), editável, afetando todas as tarefas do projeto
- [ ] CRUD de Etapa (RF-010), com flag `e_final`, bloqueio de exclusão se houver tarefas na etapa (RN-005), exigência de ao menos uma transição de saída para etapas não-finais (RN-003)
- [ ] CRUD de Transição, incluindo tipo `REABERTURA` para suportar "desfinalizar" (RF-012)
- [ ] Engine de validação de transição: dado etapa atual + etapa destino, retornar se é permitida

## Guia técnico

- Pacote: `domain/projeto`, `domain/workflow`
- Referência: `docs/techspec/kanban-configuravel-techspec.md` seções 3 e 4, `docs/techspec/kanban-configuravel/data-model.md`

## Critérios de aceite

- Endpoints REST de CRUD funcionando com validação de regras de negócio (RN-003, RN-005)
- Testes unitários da engine de transição cobrindo casos permitido/proibido/reabertura (TDD, 80%+)

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
