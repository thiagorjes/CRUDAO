---
id: DDR-002
type: DDR
status: accepted
date: 2026-08-22
supersedes: —
superseded-by: —
---

# DDR-002 — Interação do board: drag-and-drop com destaque de colunas válidas + menu alternativo

## Decisão

A movimentação de tarefas no board é primariamente por drag-and-drop: ao iniciar o arraste, as colunas com transição permitida (RF-002) recebem destaque visual (cor diferenciada); soltar fora de uma coluna destacada não move a tarefa. Como alternativa ao drag-and-drop, cada card tem um menu (dropdown) com opções de avançar/retroceder no workflow, incluindo "desfinalizar" quando a tarefa está na etapa final (RF-012).

## Motivação

Drag-and-drop é o padrão esperado em ferramentas kanban; o menu alternativo cobre casos de acessibilidade motora, precisão em boards densos, e a ação específica de reabertura de tarefa finalizada.

**Problema que resolve:**
Tornar visível, no momento da interação, quais movimentos são permitidos pelo workflow configurado (RF-002), e prover um caminho alternativo ao drag-and-drop.

**Restrições consideradas:**
- RF-004: impedimento não bloqueia nem libera movimentação — a regra de transição do workflow prevalece.
- RF-012: etapa final permite "desfinalizar", exposto no menu do card quando a tarefa está finalizada.

## Consequências

**Positivas:**
- Feedback visual imediato evita tentativas de movimento inválido.
- Menu cobre o caso de reabertura sem exigir um gesto de drag específico.

**Negativas / trade-offs:**
- Duas formas de mover a tarefa (drag + menu) exigem que ambas apliquem exatamente a mesma validação de transição no backend.

**Downstream afetado:**
- Frontend: componente de Card com estado de drag e menu dropdown.
- Backend: endpoint único de movimentação (`PATCH /api/tarefas/{id}/mover`) reaproveitado por ambas as interações.

## Alternativas Consideradas

### Alternativa 1 — Somente drag-and-drop
**Descartada porque:** usuário pediu explicitamente uma opção de menu para avançar/retroceder.

### Alternativa 2 — Somente menu (sem drag-and-drop)
**Descartada porque:** drag-and-drop é o padrão esperado de UX em boards kanban.
