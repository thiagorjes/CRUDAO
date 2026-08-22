# TASK-01.1 — Modelo de domínio: Projeto, Workflow, Etapa e Transição [G]

**Epic:** EPIC-01 — Domínio: Projeto, Workflow, Etapas e Raias | **User Story:** US-01.1 — Estrutura configurável de workflow por projeto
**Sistema:** CRUDAO | **RF:** RF-008, RF-009, RF-010, RF-002 | **Dependências:** TASK-00.2

---

## Contexto

Base do sistema: projetos configuráveis com workflows próprios, etapas ordenadas e transições que definem o que é permitido mover (RF-002, engine central do produto).

## O que deve ser feito

- [x] Implementar entidades Projeto, Workflow, Etapa, Transição (ver `docs/techspec/kanban-configuravel/data-model.md`)
- [x] CRUD de Projeto (RF-008), com bloqueio de exclusão se houver tarefas ativas (RN-005 — ver nota)
- [x] CRUD de Workflow por projeto (RF-009), editável (incrementa `versao`), afetando todas as tarefas do projeto
- [x] CRUD de Etapa (RF-010), com flag `etapaFinal` (renomeada de `e_final`, ver nota técnica), bloqueio de exclusão se houver tarefas na etapa (RN-005 — ver nota), exigência de ao menos uma transição de saída para etapas não-finais (RN-003, exposta em `GET /api/workflows/{id}/consistencia`)
- [x] CRUD de Transição, incluindo tipo `REABERTURA` para suportar "desfinalizar" (RF-012)
- [x] Engine de validação de transição (`TransicaoEngine`): dado etapa atual + etapa destino, retorna se é permitida

## Guia técnico

- Pacote: `domain/projeto`, `domain/workflow`
- Referência: `docs/techspec/kanban-configuravel-techspec.md` seções 3 e 4, `docs/techspec/kanban-configuravel/data-model.md`

## Critérios de aceite

- [x] Endpoints REST de CRUD funcionando com validação de regras de negócio (RN-003, RN-005)
- [x] Testes unitários da engine de transição cobrindo casos permitido/proibido/reabertura (TDD — 6 testes, 100% dos cenários da engine)

## Status: Concluída — 2026-08-22

Fluxo completo validado via `docker compose` + chamadas REST reais: criar projeto → workflow → etapas → transição, `GET /consistencia` (RN-003) e exclusão (RN-005). TDD aplicado à `TransicaoEngine` (Red→Green, 6 testes) e a um teste de integração (`WorkflowFluxoIT`, Testcontainers) cobrindo o mesmo fluxo contra Postgres real — validado por compilação nesta sessão (mesma limitação de Docker-in-Docker da TASK-00.2).

**Achado técnico importante (registrado em `coding-standards.md` e `data-model.md`):** o campo `eFinal` colide com a convenção de introspecção JavaBeans — `isEFinal()` resolve para a propriedade `EFinal` (não `eFinal`), quebrando o mapeamento MapStruct/Jackson silenciosamente (sem erro de compilação). Renomeado para `etapaFinal` em toda a stack.

**Nota sobre RN-005:** a entidade Tarefa só é criada na TASK-02.1. A verificação de "tarefas ativas" está implementada via uma porta (`VerificadorDeTarefasAtivas`), hoje sempre retornando "sem tarefas" — a TASK-02.1 deve substituir essa implementação por uma consulta real ao repositório de Tarefa. Endpoints e regra estão prontos; falta apenas o dado real para bloquear de fato.

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
