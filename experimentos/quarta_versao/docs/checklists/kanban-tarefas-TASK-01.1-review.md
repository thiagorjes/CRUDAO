# Code Review — TASK-01.1

_Data: 2026-08-27 | Escopo: setup backend/frontend, Docker Compose e Keycloak dev_

## Critérios de Aceite

| # | Critério | Verificado? | Evidência |
|---|---|---|---|
| 1 | `docker compose up -d` sobe PostgreSQL, Keycloak, backend e frontend | ⚠️ | Compose atual declara os quatro serviços; execução runtime não pôde ser repetida porque o Docker Desktop está indisponível nesta sessão |
| 2 | Backend inicia dentro do container Docker e conecta ao PostgreSQL | ⚠️ | Variáveis e configuração usam `postgres`/`keycloak`; smoke runtime anterior foi bem-sucedido, mas não foi repetido nesta sessão |
| 3 | Frontend inicia dentro do container Docker e serve a página padrão | ⚠️ | Serviço e imagem estão configurados; smoke runtime anterior foi bem-sucedido, mas não foi repetido nesta sessão |
| 4 | Realm importado contém client e dois usuários de teste | ✅ | `keycloak/realm-export.json` contém client `kanban-frontend` e usuários `dev.teste`/`admin.teste`; discovery do realm respondeu HTTP 200 |

## 🔴 Crítico

Nenhum finding crítico de segurança foi identificado.

## 🔴 Crítico

Nenhum finding crítico permanece. O finding C1 foi resolvido: o Compose agora declara `postgres`, `keycloak`, `backend` e `frontend`, com dependências e healthchecks.

## 🟡 Importante

Nenhum finding importante permanece. O finding I1 foi resolvido: o backend usa `postgres` e `keycloak` na configuração da rede Docker; URLs públicas do frontend permanecem em `localhost`.

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
- A vulnerabilidade transitiva do PostCSS foi resolvida atualizando o Next.js para `16.3.3`; `npm audit --omit=dev` não reporta vulnerabilidades.
- Não houve código de autenticação/autorização de negócio nesta task para validar além da configuração de dependências.

## Qualidade de Código

- Estrutura inicial está simples e coerente com Spring Boot/Next.js.
- Configuração backend mantém `ddl-auto: validate` e Flyway habilitado.
- O frontend possui script de teste e build reproduzível após `npm ci`.
- A stack está declarada de forma reproduzível; falta apenas repetir a execução runtime com o daemon Docker disponível.

## Conformidade com TechSpec

- Conforme: Java 25, Spring Boot 3.5.16, PostgreSQL, Keycloak/OIDC, Flyway, Dockerfiles e estrutura inicial Next.js.
- Conforme: o Compose sobe os quatro componentes previstos e a configuração interna resolve os serviços pelos nomes da rede Docker.

## Resultado

**APROVADO COM VALIDAÇÃO RUNTIME PENDENTE**

Os findings C1 e I1 foram corrigidos. A aprovação final depende de repetir `docker compose up -d` em ambiente limpo e confirmar os quatro healthchecks quando o Docker Desktop estiver disponível.
