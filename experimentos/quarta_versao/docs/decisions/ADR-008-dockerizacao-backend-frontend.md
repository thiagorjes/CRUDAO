# ADR-008 — Dockerização de backend e frontend

_Status: Aceito | Data: 2026-08-26 | Feature: kanban-tarefas_

## Contexto

RNF-004 (PRD) e `architecture.md`/`stack.md` já exigiam empacotamento em container (Docker/OpenShift/Kubernetes), e `stack.md` já documentava a convenção de `backend/Dockerfile` (multi-stage `maven:3.9-eclipse-temurin-25` → `eclipse-temurin:25-jre`). Na prática, `systems/CRUDAO/docker-compose.yml` só sobe a infra (`postgres`, `keycloak`) — backend e frontend rodam local via `mvnw spring-boot:run`/`npm run dev`, quebrando a homologação/portabilidade prevista. Requisito reforçado explicitamente pelo usuário em 2026-08-26: back e front devem rodar em Docker.

## Decisão

Fechar o gap com dois Dockerfiles multi-stage e a extensão do `docker-compose.yml` existente:

- **`backend/Dockerfile`** — stage de build `maven:3.9-eclipse-temurin-25` (cache de dependências via `mvn dependency:go-offline` antes de copiar o código), stage de runtime `eclipse-temurin:25-jre` rodando o jar como usuário não-root. Flyway continua aplicando migrations no boot (ADR-005) — nenhuma mudança de comportamento, só de empacotamento.
- **`frontend/Dockerfile`** — stage de build `node:20-alpine` (`npm ci && npm run build`), stage de runtime servindo via `next start` (ou `output: standalone` do Next.js, reduzindo a imagem final). Variáveis de ambiente (`NEXT_PUBLIC_BACKEND_URL`, `SESSION_SECRET`, URLs do Keycloak) injetadas via `environment:`/`.env` do compose, nunca hardcoded na imagem.
- **`docker-compose.yml`** ganha os serviços `backend` (porta 8081, `depends_on: postgres` com `condition: service_healthy`, `depends_on: keycloak`) e `frontend` (porta 3000, `depends_on: backend`). `postgres`/`keycloak` inalterados.
- Rede padrão do compose (bridge) — serviços se resolvem por nome (`postgres`, `keycloak`, `backend`), URLs internas trocam `localhost` por nome do serviço nas variáveis de ambiente do container.
- `docker compose up -d` sobe a stack completa (infra + app); `docker compose up -d postgres keycloak` continua válido para quem quiser rodar backend/frontend local durante desenvolvimento ativo (hot reload).

## Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **Dockerfiles + extensão do compose existente (escolhida)** | Reaproveita a infra já validada (ADR-005/006/007, realm Keycloak); zero mudança de arquitetura de runtime; alinhado ao que `stack.md`/RNF-004 já previam | Imagem de dev não é a mesma de produção (produção real usaria manifests OpenShift/K8s, fora de escopo aqui) |
| Multi-stage único orquestrando tudo via script (sem compose) | — | Perde orquestração declarativa, healthcheck e dependência entre serviços que o compose já oferece |
| Manter backend/frontend fora do Docker, só documentar como rodar local | Zero esforço | Não atende ao requisito explícito do usuário nem ao RNF-004; quebra paridade dev/homologação |

## Consequências

- Homologação e onboarding passam a ser `docker compose up -d` único, sem instalar JDK 25/Node localmente.
- `quickstart.md` e a Seção 5 da TechSpec principal atualizados para refletir o setup via Docker como caminho padrão (mantendo o setup local como alternativa de desenvolvimento ativo).
- Gera task nova de implementação (Dockerfiles + compose) — não coberta pelas tasks do Epic 08 já escritas; adicionar ao `/tasks` antes de fechar o hardening final.
- CI/CD (`git-workflow.md`/`devops`) pode reusar as mesmas imagens em pipelines futuros — fora de escopo desta ADR.

## Referências

RNF-004, `stack.md` (convenção de Dockerfile já documentada), `architecture.md` (deploy containerizado on-premise via Docker).
