# TASK-04.2 — Mover tarefa: engine de transição + congelamento + lead-time

**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RF-002, RF-003, RF-006, RF-012
**Dependências:** TASK-04.1
**Paralelismo:** nenhum (mas TASK-04.3 e TASK-04.4 podem rodar em paralelo a esta)

## Contexto

Lógica de maior risco do domínio — movimentação de card, congelamento pós-início e cálculo de lead-time por etapa. **TDD obrigatório** (via `/tdd`), conforme `skill-conventions.md`.

## O que deve ser feito

- [ ] Implementar `POST /api/tarefas/{id}/mover`: valida transição configurada (RN-003 via Transicao), valida `tarefa:finalizar` se destino/origem for etapa final (RN-011), valida projeto não finalizado.
- [ ] Em transação: fechar `TarefaEtapaHistorico` atual (`saidaEm=now`), abrir novo, atualizar `etapaAtualId`, setar `iniciada=true` ao sair da primeira etapa, gravar `TarefaAuditoria` (campo `etapa`).
- [ ] Implementar "desfinalizar" (RN-004, RN-011): retorna tarefa a etapa selecionada, exige `tarefa:finalizar`.
- [ ] Implementar congelamento de campos estruturais (`titulo`, `descricaoEscopo`) via `PUT /api/tarefas/{id}` quando `iniciada=true` — permite apenas campos editáveis pós-início (`responsavelId`, `etapaAtualId` via transição, `impedida`/`impedidaDesde`).
- [ ] Implementar RN-012 (autoatribuição/reatribuição de responsável, achado do Comitê de Análise — QA, regra sem cobertura prévia): usuário do papel `dev` só pode alterar `responsavelId` para si mesmo ("puxar" a tarefa), mesmo se já atribuída a outro — nunca atribui a terceiros. Usuário com `tarefa:finalizar` implícito de `product_owner`/`project_admin`/`admin` (ou permissão equivalente de gestão) atribui/reatribui livremente a qualquer usuário. Toda troca de responsável gera `TarefaAuditoria` (campo `responsavel`).
- [ ] Implementar `GET /api/tarefas/{id}` com cálculo de lead-time por etapa (RN-001) a partir de `TarefaEtapaHistorico` (etapa em andamento: `now() - entradaEm`).

## Guia técnico

- `backend/src/main/java/.../tarefa/TarefaService.java` (métodos `mover`, `editar`, `detalhe`).
- Contrato: `docs/techspec/kanban-tarefas/contracts/tarefas.md`.
- Usar `/tdd` para esta task (lógica de maior risco).

## Critérios de aceite

- Transição bloqueada quando não configurada.
- Mover para/reabrir etapa final sem `tarefa:finalizar` → `403`.
- Edição de campo estrutural após início bloqueada; campos editáveis pós-início permanecem editáveis.
- Lead-time por etapa calculado corretamente, incluindo etapa em andamento (`saidaEm=null`).
- Toda movimentação gera linha em `TarefaAuditoria`.
- Dev tentando atribuir a tarefa a terceiro → `403`; dev autoatribuindo (mesmo tarefa já atribuída a outro) → permitido (RN-012).
- Product_owner/project_admin/admin atribuem/reatribuem livremente a qualquer usuário.
- Toda troca de responsável gera linha em `TarefaAuditoria` (campo `responsavel`) com valor anterior/novo.
