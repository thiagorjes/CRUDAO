# TASK-08.3 — Dockerização integral da stack (RNF-004, ADR-008) [M]

**Status: Implementada** — 2026-08-27

- **Sistema:** CRUDAO
- **RF de origem:** RNF-004
- **Dependências:** nenhuma (não depende de código funcional — só empacota o que já existe)
- **[P] com:** TASK-08.1, TASK-08.2

## Contexto

Débito técnico registrado desde a TechSpec inicial: RNF-004 (PRD) e `systems/CRUDAO/guidelines/stack.md`/`architecture.md` já previam deploy containerizado, mas a stack ainda não garante a execução integral dos componentes. Backend, frontend, Keycloak e PostgreSQL devem rodar via Docker, sem exigir JDK 25, Node ou Keycloak instalados localmente.

Formalizado como requisito explícito pelo usuário em 2026-08-26 e ampliado em 2026-08-27 pela TechSpec v1.2, registrado em [ADR-008](../../decisions/ADR-008-dockerizacao-backend-frontend.md). Esta task fecha a implementação real (Dockerfiles, Compose completo) e valida ponta a ponta.

## O que deve ser feito

- [x] Criar `backend/Dockerfile` multi-stage:
  - Stage de build: `maven:3.9-eclipse-temurin-25`, copiar `pom.xml` primeiro e rodar `mvn dependency:go-offline` antes de copiar o código-fonte (cache de camada Docker), depois `mvn package -DskipTests` (testes já rodam em CI/local, não repetir no build da imagem).
  - Stage de runtime: `eclipse-temurin:25-jre`, copiar só o jar final (`target/*.jar`), rodar como usuário não-root (`useradd`/`USER`), `ENTRYPOINT ["java", "-jar", "app.jar"]`.
- [x] Criar `frontend/Dockerfile` multi-stage:
  - Stage de build com Node LTS dentro da imagem, `npm ci`, `npm run build`.
  - Stage de runtime: avaliar `output: standalone` no `next.config.js` para copiar só o necessário (reduz tamanho final); `CMD ["node", "server.js"]` (standalone) ou `next start` como alternativa mais simples se `standalone` gerar complexidade extra não justificada.
- [x] Revisar/corrigir `docker-compose.yml` (`systems/CRUDAO/`) — os serviços `backend`/`frontend` seguem a TechSpec v1.2; nesta task:
  - Confirmar os nomes reais das env vars Spring (`SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD`, issuer do resource server opaco) contra `backend/src/main/resources/application.yml` e `application-dev.yml`.
  - Confirmar as env vars do frontend (`NEXT_PUBLIC_BACKEND_URL`, `SESSION_SECRET`, URLs do Keycloak) contra `frontend/.env.local.example`.
  - Manter `depends_on` com `condition: service_healthy` para `postgres`/`keycloak` no `backend`, e `depends_on: backend` no `frontend`.
- [x] Validar resolução de nomes na rede do compose: o **backend dentro do container** deve falar com `postgres`/`keycloak` pelo nome do serviço (não `localhost`); o **frontend no browser do usuário** continua falando `localhost:8081`/`localhost:8080` (URLs client-side não resolvem nomes internos do Docker) — variáveis separadas para uso server-side (Next.js BFF, dentro do container) vs. client-side (`NEXT_PUBLIC_*`, expostas ao browser).
- [x] Rodar `docker compose up -d` de ponta a ponta a partir de um ambiente limpo (sem volumes/imagens pré-existentes: `docker compose down -v` antes) e validar manualmente: stack saudável, frontend HTTP 200, Keycloak discovery HTTP 200 e backend conectado ao PostgreSQL.
- [x] Atualizar `docs/techspec/kanban-tarefas/quickstart.md`/`README.md` se algum detalhe real (nome de env var, porta, comportamento) divergir do que a TechSpec v1.2 já documentou.

## Guia técnico

- Arquivos novos: `backend/Dockerfile`, `frontend/Dockerfile`.
- Arquivo a ajustar: `docker-compose.yml` (já tem os serviços esboçados, conferir env vars reais).
- Convenção de multi-stage já documentada em `systems/CRUDAO/guidelines/stack.md` (seção de linguagem/build do backend).
- Decisão de arquitetura: [ADR-008](../../decisions/ADR-008-dockerizacao-backend-frontend.md).
- Sem migration nova, sem mudança de schema — só empacotamento.

## Critérios de aceite

- `docker compose up -d` (comando único, sem passo manual adicional) sobe `postgres`, `keycloak`, `backend`, `frontend`, e a aplicação fica utilizável em `http://localhost:3000` — login, board, mover card funcionam.
- Nenhuma credencial/URL sensível hardcoded na imagem — tudo injetado via `environment:`/`.env` do compose.
- Backend continua aplicando migrations Flyway automaticamente no boot do container, sem mudança de comportamento (ADR-005).
- Backend, frontend, Keycloak e PostgreSQL executam pela stack Docker, sem dependência de JDK, Node ou Keycloak instalados no host.
- Tamanho da imagem final do backend e do frontend razoável (multi-stage não deve carregar toolchain de build na imagem de runtime — validar com `docker images`).
