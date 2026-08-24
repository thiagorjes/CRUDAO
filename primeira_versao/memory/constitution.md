# Constituição — CRUDAO
_Criada em: 2026-08-22_

> Princípios estáveis, ADRs e decisões de design do workspace.
> Atualizado apenas quando os fundamentos mudarem.

---

## Contexto do Workspace

- **CRUDAO** — cenário: Novo (greenfield)
- **Propósito:** _(preencher após /guidelines)_
- **Idioma:** pt_BR

---

## Decision Records

> Ao criar uma nova DR, adicione uma linha na tabela do tipo correspondente.
> Coluna **ID** deve ser um link relativo para o arquivo em `docs/decisions/`:
> `[ADR-001](../docs/decisions/ADR-001-titulo-curto.md)`.
> O próximo NNN é o maior já usado no tipo + 1 (contadores independentes por tipo — ver ADR-012).

### ADR

| ID | Título | Status |
|----|--------|--------|
| [ADR-001](../docs/decisions/ADR-001-stack-backend-java-spring.md) | Stack de backend: Java 25 + Spring Boot (LTS) | accepted |
| [ADR-002](../docs/decisions/ADR-002-postgresql-sem-cache-tempo-real.md) | PostgreSQL como único armazenamento; sem cache/broker nesta fase | accepted |
| [ADR-003](../docs/decisions/ADR-003-rbac-hibrido-keycloak.md) | RBAC híbrido: Keycloak para autenticação, permissões na aplicação | accepted |
| [ADR-004](../docs/decisions/ADR-004-broadcast-multi-pod-listen-notify.md) | Broadcast de eventos entre pods via PostgreSQL LISTEN/NOTIFY | accepted |
| [ADR-005](../docs/decisions/ADR-005-dashboard-assincrono.md) | Cálculo do dashboard de gestão de forma assíncrona | accepted |

### BDR

| ID | Título | Status |
|----|--------|--------|
| [BDR-001](../docs/decisions/BDR-001-rbac-por-projeto.md) | RBAC por projeto com papéis acumuláveis | accepted |

### SDR

| ID | Título | Status |
|----|--------|--------|

### DDR

| ID | Título | Status |
|----|--------|--------|
| [DDR-001](../docs/decisions/ddr-001-design-tokens-base.md) | Tokens base de design: cores, tipografia e espaçamento | accepted |
| [DDR-002](../docs/decisions/ddr-002-drag-and-drop-board.md) | Interação do board: drag-and-drop com destaque + menu alternativo | accepted |
| [DDR-003](../docs/decisions/ddr-003-feedback-async-acessibilidade.md) | Padrões de feedback, loading assíncrono e nível de acessibilidade | accepted |

---

## Princípios Estáveis

_(preencher após /guidelines)_
