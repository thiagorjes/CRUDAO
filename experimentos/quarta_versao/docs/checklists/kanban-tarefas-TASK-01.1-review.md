# Code Review — TASK-01.1

_Data: 2026-08-27 | Escopo: setup backend/frontend, Docker Compose e Keycloak dev_

## Critérios de Aceite

| # | Critério | Verificado? | Evidência |
|---|---|---|---|
| 1 | `docker compose up -d` sobe PostgreSQL, Keycloak, backend e frontend | ❌ | `docker compose config` válido, mas a configuração contém somente `postgres` e `keycloak`; os serviços `backend` e `frontend` não existem |
| 2 | Backend inicia dentro do container Docker e conecta ao PostgreSQL | ❌ | O Dockerfile existe, mas não há serviço backend no Compose para executar; `application-dev.yml` ainda aponta para `localhost` |
| 3 | Frontend inicia dentro do container Docker e serve a página padrão | ❌ | O Dockerfile existe, mas não há serviço frontend no Compose; HTTP 200 anterior foi obtido via `next dev` no host |
| 4 | Realm importado contém client e dois usuários de teste | ✅ | `keycloak/realm-export.json` contém client `kanban-frontend` e usuários `dev.teste`/`admin.teste`; discovery do realm respondeu HTTP 200 |

## 🔴 Crítico

Nenhum finding crítico de segurança foi identificado.

## 🔴 Crítico

#### [C1] A stack obrigatória não inclui backend nem frontend

Arquivo: `systems/CRUDAO/docker-compose.yml`

Problema: a TechSpec v1.2 e os critérios atuais exigem que `docker compose up -d` execute PostgreSQL, Keycloak, backend e frontend. O Compose atual declara somente `postgres` e `keycloak`, tornando impossível validar a aplicação completa pelo fluxo oficial.

Como corrigir: adicionar os serviços `backend` e `frontend`, com `build`, portas, `depends_on` com healthchecks e variáveis de ambiente necessárias.

## 🟡 Importante

#### [I1] Configuração do backend usa hosts incompatíveis com a rede Docker

Arquivo: `systems/CRUDAO/backend/src/main/resources/application-dev.yml:3,16`

Problema: `jdbc:postgresql://localhost:5432/kanban` e o endpoint Keycloak `http://localhost:8080/...` apontam para o próprio container quando o backend for executado em Docker.

Como corrigir: usar `postgres` e `keycloak` como nomes de serviço na configuração injetada pelo Compose, mantendo URLs públicas separadas apenas quando necessárias ao browser.

## 🔵 Sugestão

#### [S1] Adicionar smoke test de inicialização do frontend

O teste atual valida somente o runner (`expect(true).toBe(true)`), não a renderização da página. Adicionar um teste de componente/rota quando a camada de UI começar a ser implementada.

## ✅ Pontos Positivos

- Compose possui healthchecks funcionais para PostgreSQL e Keycloak.
- O realm é montado como somente leitura e contém client e usuários de desenvolvimento.
- Backend usa imagem de build com Java 25 e usuário não-root no runtime.
- Frontend usa dependências travadas por `package-lock.json` e build de produção passou.
- `.dockerignore` evita incluir `node_modules`, `.next`, logs e arquivos `.env.local` no contexto de build.

## Segurança

O bloqueio é de execução e integração, não de vulnerabilidade de segurança.

- As credenciais `admin/admin`, `kanban/kanban` e usuários de teste estão explicitamente restritas ao realm/ambiente dev. Não devem ser reutilizadas em produção.
- O segredo do client está versionado no realm de desenvolvimento; deve ser substituído por injeção de segredo fora do repositório em ambientes reais.
- Não houve código de autenticação/autorização de negócio nesta task para validar além da configuração de dependências.

## Qualidade de Código

- Estrutura inicial está simples e coerente com Spring Boot/Next.js.
- Configuração backend mantém `ddl-auto: validate` e Flyway habilitado.
- O frontend possui script de teste e build reproduzível após `npm ci`.
- A stack não é reproduzível pelo comando oficial porque dois serviços exigidos não foram declarados.

## Conformidade com TechSpec

- Conforme: Java 25, Spring Boot 3.5.16, PostgreSQL, Keycloak/OIDC, Flyway, Dockerfiles e estrutura inicial Next.js.
- Não conforme: o Compose não sobe backend/frontend e a configuração dev não resolve os serviços pela rede Docker.

## Resultado

**REPROVADO — BLOQUEADO**

Os artefatos individuais de backend/frontend existem, mas a TASK-01.1 não atende o requisito central de execução integral via Docker. Corrigir C1 e I1, executar `docker compose up -d` em ambiente limpo e repetir o review.
