# TASK-04.5 — GET board + GET detalhe com projeção DTO (sem N+1)

**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RF-001, RF-006
**Dependências:** TASK-04.2, TASK-04.3
**Paralelismo:** nenhum

## Contexto

Endpoint mais consultado do sistema — exige projeção DTO obrigatória (achado do Comitê de Análise — Database, ver `data-model.md` seção final "Nota de performance") para evitar N+1 sob volume de tarefas.

## O que deve ser feito

- [ ] Implementar `GET /api/projetos/{projetoId}/board` retornando etapas na ordem configurada, cada uma com as tarefas correspondentes, agrupadas por raia — via JPQL `SELECT NEW` ou `@EntityGraph` cobrindo `Etapa`, `Raia`, `Tarefa`, indicador de impedimento.
- [ ] Confirmar `GET /api/tarefas/{id}` (iniciado em TASK-04.2) usa a mesma estratégia de projeção para as associações de histórico.
- [ ] Validar via Hibernate Statistics/Testcontainers que a contagem de queries não escala com o número de tarefas retornadas — critério de aceite explícito da TechSpec.

## Guia técnico

- `backend/src/main/java/.../tarefa/BoardService.java`
- `backend/src/main/java/.../tarefa/dto/BoardDTO.java` (MapStruct/JPQL)
- Contrato: `docs/techspec/kanban-tarefas/contracts/tarefas.md` (seção GET board).
- **Não usar** relações `lazy` percorridas em loop — proibido pela TechSpec.

## Critérios de aceite

- Board retorna etapas na ordem configurada, cada uma com as tarefas correspondentes.
- Teste de integração comprova ausência de N+1 (contagem de queries fixa independentemente do volume de tarefas retornadas).
