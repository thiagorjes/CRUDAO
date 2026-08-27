# Constituicao - CRUDAO
_Criada em: 2026-08-27_

> Principios estaveis, ADRs e decisoes de design do workspace.
> Atualizar somente quando os fundamentos do sistema mudarem.

---

## Contexto do Workspace

- **CRUDAO** - cenario: Novo (greenfield)
- **Proposito:** oferecer um kanban configuravel por projeto para acompanhar tarefas, impedimentos e lead-time com SSO e RBAC.
- **Idioma:** pt_BR

## Decision Records

> O proximo NNN de cada tipo e independente. Ao criar uma DR, adicionar o link na tabela correspondente.

### ADR

| ID | Titulo | Status |
|----|--------|--------|
| [ADR-001](../docs/decisions/ADR-001-stack-backend-java-spring.md) | Stack de backend: Java 25 + Spring Boot | Aceito |
| [ADR-002](../docs/decisions/ADR-002-postgresql-sem-cache-tempo-real.md) | PostgreSQL como unico armazenamento, sem cache/broker nesta fase | Aceito |
| [ADR-003](../docs/decisions/ADR-003-rbac-hibrido-keycloak.md) | RBAC hibrido: Keycloak para autenticacao e permissoes na aplicacao | Aceito |
| [ADR-004](../docs/decisions/ADR-004-broadcast-listen-notify.md) | Broadcast multi-pod via PostgreSQL LISTEN/NOTIFY | Aceito |
| [ADR-005](../docs/decisions/ADR-005-flyway-migrations.md) | Flyway para versionamento de schema | Aceito |
| [ADR-006](../docs/decisions/ADR-006-sem-fallback-auth-keycloak.md) | Sem fallback de autenticacao local quando Keycloak indisponivel | Aceito |
| [ADR-007](../docs/decisions/ADR-007-bootstrap-admin-global.md) | Bootstrap do primeiro admin via adminGlobal e e-mail configurado | Aceito |
| [ADR-008](../docs/decisions/ADR-008-dockerizacao-backend-frontend.md) | Dockerizacao de backend e frontend | Aceito |

### BDR

| ID | Titulo | Status |
|----|--------|--------|

### SDR

| ID | Titulo | Status |
|----|--------|--------|

### DDR

| ID | Titulo | Status |
|----|--------|--------|

## Principios Estaveis

1. PostgreSQL e a fonte unica de verdade; nao introduzir cache ou broker dedicado nesta fase.
2. Autenticacao usa Keycloak/OIDC; nao existe fallback de senha local.
3. Autorizacao e validada no backend por RBAC escopado por projeto; a UI nunca e a unica barreira.
4. Alteracoes de schema sao feitas exclusivamente por migrations Flyway; Hibernate usa `ddl-auto=validate`.
5. Eventos de board e notificacoes devem funcionar em uma ou mais instancias sem estado de negocio em memoria local.
6. Backend e frontend sao empacotados em imagens Docker; segredos e URLs variaveis entram por ambiente.
7. TDD e obrigatorio para transicoes, lead-time e resolucao de permissoes; testes de RF Must Have sao obrigatorios.