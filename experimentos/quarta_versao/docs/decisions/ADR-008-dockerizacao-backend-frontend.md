# ADR-008 — Execução integral em Docker

_Status: Aceito | Data: 2026-08-26 | Feature: kanban-tarefas_

## Contexto

RNF-004 (PRD) e `architecture.md`/`stack.md` exigem empacotamento em container. Para garantir paridade entre desenvolvimento, validação e homologação, nenhum componente executável deve depender de instalação no host. Backend, frontend, Keycloak e PostgreSQL precisam ser serviços Docker no ambiente local.

## Decisão

Fechar o gap com Dockerfiles multi-stage e um `docker-compose.yml` que orquestre todos os componentes executáveis:

- **`backend/Dockerfile`** — stage de build `maven:3.9-eclipse-temurin-25` (cache de dependências via `mvn dependency:go-offline` antes de copiar o código), stage de runtime `eclipse-temurin:25-jre` rodando o jar como usuário não-root. Flyway continua aplicando migrations no boot (ADR-005) — nenhuma mudança de comportamento, só de empacotamento.
- **`frontend/Dockerfile`** — stage de build `node:20-alpine` (`npm ci && npm run build`), stage de runtime servindo via `next start` (ou `output: standalone` do Next.js, reduzindo a imagem final). Variáveis de ambiente (`NEXT_PUBLIC_BACKEND_URL`, `SESSION_SECRET`, URLs do Keycloak) injetadas via `environment:`/`.env` do compose, nunca hardcoded na imagem.
- **`docker-compose.yml`** deve conter `postgres`, `keycloak`, `backend` (porta 8081) e `frontend` (porta 3000), com healthchecks e dependências ordenadas. Keycloak e PostgreSQL também são obrigatoriamente executados pelo Compose no ambiente local/homologação.
- Rede padrão do compose (bridge) — serviços se resolvem por nome (`postgres`, `keycloak`, `backend`), URLs internas trocam `localhost` por nome do serviço nas variáveis de ambiente do container.
- `docker compose up -d` sobe a stack completa (infra + app) e é o único fluxo suportado para validação. Execução direta via `mvnw`, `npm run dev` ou instalação local de Keycloak não é suportada.

## Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **Dockerfiles + extensão do compose existente (escolhida)** | Reaproveita a infra já validada (ADR-005/006/007, realm Keycloak); zero mudança de arquitetura de runtime; alinhado ao que `stack.md`/RNF-004 já previam | Imagem de dev não é a mesma de produção (produção real usaria manifests OpenShift/K8s, fora de escopo aqui) |
| Multi-stage único orquestrando tudo via script (sem compose) | — | Perde orquestração declarativa, healthcheck e dependência entre serviços que o compose já oferece |
| Manter backend/frontend fora do Docker, só documentar como rodar local | Zero esforço | Não atende ao requisito explícito do usuário nem ao RNF-004; quebra paridade dev/homologação |

## Consequências

- Homologação, onboarding e smoke tests passam a usar `docker compose up -d` único, sem instalar JDK 25, Node ou Keycloak no host.
- `quickstart.md` e a Seção 5 da TechSpec principal definem Docker como caminho único de execução.
- Gera task nova de implementação (Dockerfiles + compose) — não coberta pelas tasks do Epic 08 já escritas; adicionar ao `/tasks` antes de fechar o hardening final.
- CI/CD (`git-workflow.md`/`devops`) pode reusar as mesmas imagens em pipelines futuros — fora de escopo desta ADR.

## Referências

RNF-004, `stack.md` (convenção de Dockerfile já documentada), `architecture.md` (deploy containerizado on-premise via Docker).
