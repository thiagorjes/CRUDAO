# Divergências — Kanban Configurável

> Registro de desvios entre o REASONS Canvas e o código implementado, detectados por `/spdd-sync`.

---

## DEV-001 — 2026-08-23
- **Dimensão afetada:** E
- **Descrição:** o código da TASK-05.2 introduziu `DashboardResultado`/`StatusJobDashboard` (`frontend/src/lib/api/types.ts`), espelhando o `DashboardResultadoDTO`/`StatusJobDashboard` do backend (já existentes desde a TASK-03.1). A dimensão E do canvas lista apenas entidades de domínio persistidas (Projeto, Workflow, Etapa, Transição, Raia, Tarefa, RegistroEtapa, Impedimento, Observador, Usuário/Papel/Permissão) e não inclui o resultado agregado do dashboard.
- **Direção de resolução:** aceito com justificativa
- **Justificativa:** `DashboardResultado` é um view-model/DTO de agregação transiente (resultado de um job assíncrono, não persistido como registro próprio além do `DashboardJob`), diferente em natureza das entidades de domínio listadas em E — a lista da dimensão E é intencionalmente restrita a entidades persistidas do modelo de dados (ver `docs/techspec/kanban-configuravel/data-model.md`).
- **Status:** accepted

---

## DEV-002 — 2026-08-24
- **Dimensão afetada:** O
- **Descrição:** a dimensão O listava TASK-04.2, TASK-01.3, TASK-02.3 e TASK-06.1 como pendentes (`[ ]`), mas `memory/state.md` registra as 4 como implementadas e revisadas em code review (última, TASK-06.1, em 2026-08-24) — canvas ficou desatualizado desde o último `/tasks` (v1.1), sem ser tocado a cada `/implement` subsequente.
- **Direção de resolução:** canvas corrigido
- **Justificativa:** código está correto e completo; a lista de operations é só um índice de progresso, sem custo de alterar para refletir a realidade.
- **Status:** resolved

---

## Sumário

| DEV | Dimensão | Direção | Status |
|---|---|---|---|
| DEV-001 | E | aceito com justificativa | accepted |
| DEV-002 | O | canvas corrigido | resolved |
