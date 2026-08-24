# Análise Cross-Artefato — Kanban Configurável
_Data: 2026-08-23 | PRD v1.3 | TechSpec v1.2 (analisada) → v1.3 (corrigida) | Tasks v1.1 | Canvas: READY_

> **Remediação aplicada em 2026-08-23:** G1, G2, S1 e S2 corrigidos nos artefatos (TASK-04.2 ampliada para incluir migração de `TarefaController`/`TarefaService` e checagem dupla de `mover-projeto`; TechSpec v1.3 com contrato explícito de `mover-projeto`; tags RN-008/RN-014/RN-013 adicionadas). M1 e B1 não corrigidos (não bloqueantes, decisão registrada no próprio finding). Status de cada finding marcado abaixo.

---

## Sumário

- Findings: 🔴 2 críticos (✅ corrigidos) | 🟡 2 altos (✅ corrigidos) | 🟠 1 médio (não corrigido, não bloqueante) | 🔵 1 baixo (não corrigido, não bloqueante)
- RFs com cobertura de tasks: 17/17 (100%)
- RNs com cobertura de tasks (citação explícita): 6/16 na análise original → 9/16 após remediação (RN-008, RN-014, RN-013 agora citadas em TASK-04.2)
- **Veredicto: ✅ Aprovado para implementação** (após remediação de G1, G2, S1, S2 em 2026-08-23)

---

## Gaps

- **G1** 🔴 ✅ corrigido — Migração de `@ExigePermissao` → `AutorizacaoProjetoService` não cobria os 7 endpoints de `TarefaController` — TASK-04.2 ampliada.
- **S1** 🟡 ✅ corrigido — RN-008/RN-014 sem tag explícita em TASK-04.2 — tags adicionadas.
- **S2** 🟡 ✅ corrigido — RN-013 sem task/critério de aceite verificável — adicionado a TASK-04.2.
- Nenhum RF do PRD sem task correspondente (17/17 cobertos — ver Cobertura RF × Tasks).

## Divergências

Nenhuma divergência de Canvas × TechSpec encontrada — dimensões E/A/S/N/O consistentes com data-model v1.2 e techspec v1.2/v1.3.

## Contradições

- **G2** 🔴 ✅ corrigido — `data-model.md` (nota de Tarefa, desde v1.0) exige permissão em **ambos** os projetos para `moverParaProjeto`; TechSpec v1.2 só descrevia `exigirPermissao` para um projeto por chamada. TechSpec v1.3 ganhou contrato explícito com a checagem dupla; TASK-04.2 ganhou o item de checklist correspondente.

## Findings

| ID | Tipo | Severidade | Localização | Resumo | Status |
|----|------|-----------|-------------|--------|--------|
| G1 | Gap de cobertura (dentro de task) | 🔴 | TASK-04.2, `TarefaController`/`TarefaService` | TASK-04.2 lista explicitamente a migração de `@ExigePermissao` → `AutorizacaoProjetoService` só para Workflow/Etapa/Transição/Raia — **omite Tarefa**, que tem 7 endpoints de escrita hoje usando `@ExigePermissao` global. | ✅ **Corrigido** — TASK-04.2 ampliada (checklist + guia técnico + critério de aceite) para incluir os 7 endpoints de `TarefaController`/`domain/tarefa` |
| G2 | Contradição / regra não carregada | 🔴 | data-model.md (nota de Tarefa, desde v1.0) vs. TechSpec v1.1/v1.2 e TASK-04.2 | Regra "mover tarefa entre projetos exige permissão em ambos" nunca propagada à TechSpec/tasks — risco de se perder na migração para `AutorizacaoProjetoService` (1 projeto por chamada). | ✅ **Corrigido** — TechSpec v1.3 ganhou contrato explícito de `PATCH /mover-projeto` (checagem dupla); TASK-04.2 ganhou item de checklist e critério de aceite correspondentes |
| S1 | Divergência de rastreabilidade | 🟡 | TASK-04.2 | RN-008 e RN-014 são centrais ao escopo da TASK-04.2 mas sem tag explícita. | ✅ **Corrigido** — tags adicionadas nos itens correspondentes do checklist |
| S2 | Gap de cobertura (regra não citada) | 🟡 | PRD RN-013 (impedimento restrito a dev/product_owner) | RN-013 não citada em nenhuma task nem verificável por critério de aceite. | ✅ **Corrigido** — item de checklist e critério de aceite adicionados à TASK-04.2 |
| M1 | Rastreabilidade | 🟠 | PRD RN-004 (etapa final sem transição padrão, permite desfinalizar) | Não citada em nenhuma task — mas funcionalmente coberta desde TASK-02.1 (já concluída, suporte a `REABERTURA`/RF-012) e reforçada por TASK-02.3 (`tarefa:finalizar`). Gap é só de tag, não funcional | Nenhuma ação obrigatória — citar a tag em TASK-02.1/02.3 se quiser rastreabilidade completa |
| B1 | Melhoria de clareza | 🔵 | `TarefaController.listarPorProjeto`/`buscar` (leitura) | Endpoints de leitura de tarefa não têm nenhum controle de acesso (nem checagem de vínculo com o projeto) — padrão já existente desde TASK-02.1, não uma regressão desta revisão, e nenhum RF exige diferente. Fica registrado como observação para uma futura revisão de "quem pode ler o quê" | Nenhuma ação nesta rodada — considerar RF futuro se leitura precisar de escopo |

---

## Cobertura RF × Tasks

| RF | Tasks | Status |
|----|-------|--------|
| RF-001 | TASK-05.1 | ✅ Coberto |
| RF-002 | TASK-01.1, TASK-02.1 | ✅ Coberto |
| RF-003 | TASK-02.1, TASK-02.3 | ✅ Coberto |
| RF-004 | TASK-02.1 | ✅ Coberto |
| RF-005 | TASK-02.2 | ✅ Coberto |
| RF-006 | TASK-03.1 | ✅ Coberto |
| RF-007 | TASK-03.1, TASK-05.2 | ✅ Coberto |
| RF-008 | TASK-01.1, TASK-01.3 | ✅ Coberto |
| RF-009 | TASK-01.1 | ✅ Coberto |
| RF-010 | TASK-01.1 | ✅ Coberto |
| RF-011 | TASK-01.2 | ✅ Coberto |
| RF-012 | TASK-02.1, TASK-02.3 | ✅ Coberto |
| RF-013 | TASK-04.1, TASK-04.2, TASK-05.3 | ✅ Coberto |
| RF-014 | TASK-00.1, TASK-04.1, TASK-05.0 | ✅ Coberto |
| RF-015 | TASK-04.2, TASK-05.3 | ✅ Coberto |
| RF-016 | TASK-01.3, TASK-05.3 | ✅ Coberto |
| RF-017 | TASK-02.3, TASK-05.4 | ✅ Coberto |

## Cobertura RN × Tasks (citação explícita)

| RN | Tasks (tag explícita) | Status |
|----|-------|--------|
| RN-001, RN-002 | TASK-03.1 | ✅ Coberto (implícito no conteúdo, não citado por tag — pré-existente v1.0) |
| RN-003 | TASK-01.1 | ✅ Coberto |
| RN-004 | — | 🟠 Coberto funcionalmente (TASK-02.1/02.3), sem tag |
| RN-005 | TASK-01.1, TASK-01.2, TASK-02.1 | ✅ Coberto |
| RN-006 | TASK-04.1, TASK-04.2 | ✅ Coberto |
| RN-007 | TASK-02.2 | ✅ Coberto (implícito) |
| RN-008 | — | 🟡 Coberto funcionalmente (TASK-04.2), sem tag (S1) |
| RN-009, RN-010 | TASK-02.3 | ✅ Coberto |
| RN-011 | TASK-02.3 | ✅ Coberto |
| RN-012 | TASK-02.3 | ✅ Coberto |
| RN-013 | — | 🟡 Não referenciada em nenhuma task (S2) |
| RN-014 | — | 🟡 Coberto funcionalmente (TASK-04.2), sem tag (S1) |
| RN-015 | TASK-01.3, TASK-04.2 | ✅ Coberto |
| RN-016 | TASK-02.3 | ✅ Coberto |

---

## Divergências de Canvas × TechSpec

Nenhuma divergência encontrada — canvas e TechSpec foram atualizados juntos nesta revisão (dimensões E/A/S/N/O consistentes com data-model v1.2 e techspec v1.2).

## Contradições entre artefatos

Ver G2 acima (única contradição relevante encontrada: regra de `moverParaProjeto` presente no data-model desde v1.0, não propagada à TechSpec v1.1/v1.2).

## Passe de segurança

- Nenhum endpoint novo (v1.1/v1.2) sem autenticação/autorização declarada na TechSpec — todos os contratos novos (`/usuarios/me`, `/membros`, `/configuracao`, `/finalizar`, `/historico`, `/responsavel`) têm erros 401/403 documentados.
- Auditoria de operações críticas: coberta por RF-017/`AuditoriaTarefa` para tarefa; não há requisito de auditoria para CRUD de Papel/Permissão nem para associação usuário-projeto-papel (RF-015) — ambas são ações administrativas sensíveis (mudança de RBAC) sem trilha de auditoria própria. Não bloqueante para esta rodada (RF-017 é escopado só a Tarefa no PRD), mas vale registrar como Q-010 na TechSpec para avaliação futura.
- G1/G2 acima já cobrem o risco de autorização mais crítico desta revisão.

---

_Origem: PRD v1.3, TechSpec v1.2, docs/tasks/kanban-configuravel-tasks.md v1.1, docs/spdd/kanban-configuravel-canvas.md_
