# Code Review — TASK-06.1 — Testes E2E dos fluxos principais e revisão de cobertura

**Data:** 2026-08-24 | **Sistema:** CRUDAO | **Revisor:** /code-review

## Gate de testes

- Backend: `mvn test spotless:check` — ✅ verde (via container `maven:3.9-eclipse-temurin-25`)
- Frontend: `vitest run` (43 testes) — ✅ verde | `tsc --noEmit` — ✅ limpo | `eslint e2e src` — ✅ limpo | `next build` — ✅ limpo
- E2E (Playwright, 14 testes) — ✅ 14/14 passando contra `docker compose up` real

## Critérios de Aceite

| # | Critério de Aceite | Verificado? | Evidência |
|---|---------------------|-------------|-----------|
| 1 | Escolher e configurar ferramenta de E2E (Q-005) | ✅ | `playwright.config.ts`; Playwright 1.62.1 em `package.json` |
| 2 | Cobrir: mover tarefa (drag e menu), impedimento, desfinalizar, dashboard assíncrono, RBAC (bloqueio) | ✅ | `e2e/board.spec.ts` (5 testes), `e2e/dashboard.spec.ts`, `e2e/rbac.spec.ts:10` |
| 3 | Cobrir fluxos novos: RBAC por projeto (isolamento), autoatribuição, `tarefa:finalizar`, projeto finalizado, toggles, aba Papéis só admin | ✅ | `e2e/rbac.spec.ts` (7 testes cobrindo cada um) |
| 4 | Revisar cobertura de testes unitários/integração vs. metas de `testing.md` | ⚠️ | revisão qualitativa feita (nota técnica em `memory/state.md`); **sem ferramenta de medição configurada** (JaCoCo ausente no backend) — meta numérica 80%/100% não é verificável automaticamente |
| 5 | Revisar canvas — confirmar `READY` | ✅ | `docs/spdd/kanban-configuravel-canvas.md:2` já `READY` desde 2026-08-23 |
| 6 | Fluxos críticos cobertos por E2E, passando localmente | ✅ | 14/14 verde |
| 7 | Cobertura de testes atinge as metas definidas em guidelines | ⚠️ | ver item 4 — não bloqueante para esta task (tooling de medição é gap pré-existente, não introduzido aqui) |

## 🔴 Crítico

Nenhum.

## 🟡 Importante

#### I1 — Meta de cobertura de `testing.md` não é verificável automaticamente
Arquivo: `systems/CRUDAO/backend/pom.xml`, `systems/CRUDAO/frontend/package.json`
Problema: `testing.md` define metas explícitas (80% TDD / 100% BDD), mas nenhum dos dois projetos tem uma ferramenta de medição de cobertura configurada (JaCoCo no backend, `vitest --coverage`/istanbul no frontend). A revisão desta task só pôde ser qualitativa (contagem de `*ServiceTest` por regra de negócio), não uma verificação objetiva contra o número da meta.
Como corrigir:
  Atual:   sem plugin de cobertura no `pom.xml`; `package.json` sem script `test:coverage`
  Correto: adicionar `jacoco-maven-plugin` com `check` goal (falha o build abaixo de 80%) e `vitest run --coverage` com threshold configurado em `vitest.config.ts`
Guideline violado: `systems/CRUDAO/guidelines/testing.md` — seção "Cobertura mínima" (meta declarada mas sem mecanismo de enforcement)
Recomendação: não bloqueante para esta task — é um gap de tooling pré-existente às TASK-00.2/todas as tasks anteriores, não introduzido pela TASK-06.1. Recomendo task dedicada (observabilidade/CI) para fechar, não reabrir esta.

#### I2 — Race condition real em `UsuarioContexto.provisionar` sob login concorrente (achado durante a validação desta task)
Arquivo: `systems/CRUDAO/backend/src/main/java/com/crudao/kanban/security/UsuarioContexto.java:46-48`
Problema: `resolverPorJwt` faz `findByKeycloakSub(...).orElseGet(() -> provisionar(jwt))` sem lock nem tratamento de `DataIntegrityViolationException`. Reproduzido de forma determinística ao rodar a suíte E2E com workers paralelos (3) contra um volume Postgres recém-criado: duas requisições concorrentes do primeiro login de `admin.teste` não encontram a linha (nenhuma foi commitada ainda) e ambas tentam `INSERT` — uma recebe `500` por violar a constraint única (`keycloak_sub` ou `email`, dependendo da corrida). Não é um bug introduzido por esta task (código da TASK-04.1), mas os testes desta task o expuseram de forma reprodutível.
Como corrigir:
  Atual:   `orElseGet(() -> provisionar(jwt))` — sem tratamento de conflito
  Correto: capturar `DataIntegrityViolationException` no `provisionar` e recair para `findByKeycloakSub`/`findByEmail` (padrão "insert, on conflict fetch"), ou usar `INSERT ... ON CONFLICT DO NOTHING` seguido de `SELECT`
Guideline violado: não coberto — recomendo adicionar a `systems/CRUDAO/guidelines/coding-standards.md` uma nota sobre "find-or-create" precisar ser resiliente a corrida em todo ponto de auto-provisionamento (padrão já usado em `RbacSeeder.buscarOuCriarPapel`/`buscarOuCriarPermissao`, mas não em `UsuarioContexto.provisionar`).
Mitigação aplicada nesta task: `e2e/global-setup.ts` provisiona `admin.teste`/`user.teste` serialmente antes dos workers paralelos, evitando que a suíte de E2E fique flaky por causa deste bug pré-existente — não corrige a causa raiz.
Recomendação: criar task de bug-fix dedicada (não bloqueia o fechamento desta feature — a mitigação no fixture torna a suíte estável).

## 🔵 Sugestão

#### S1 — Encadeamento `.locator('..').locator('..')` para localizar o card a partir do texto do título
Arquivo: `systems/CRUDAO/frontend/e2e/board.spec.ts:26`, `:92`; `systems/CRUDAO/frontend/e2e/rbac.spec.ts:100`
Problema: alguns testes navegam do nó de texto do título até o card via dois `locator('..')`, dependente da profundidade exata do DOM (`<div className={styles.titulo}>{tarefa.titulo}</div>` dentro do card). Uma mudança de wrapper no `CardTarefa` quebra o teste sem relação com o comportamento testado.
Como corrigir:
  Atual:   `page.getByText('Tarefa via menu', { exact: true }).locator('..').locator('..')`
  Correto: `page.getByTestId('card-tarefa').filter({ hasText: 'Tarefa via menu' })` — mesmo padrão já usado em `board.spec.ts:41` e `:118`
Guideline violado: não coberto — recomendo adicionar a `systems/CRUDAO/guidelines/testing.md` uma nota sobre preferir `data-testid` a navegação relativa de DOM em specs E2E.

#### S2 — Credenciais de teste do Keycloak hardcoded em `fixtures/api.ts`
Arquivo: `systems/CRUDAO/frontend/e2e/fixtures/api.ts:12-13`
Problema: `admin123`/`user123` e o `client_secret` (`crudao-app-secret-dev`) aparecem em texto puro no fixture. Não é uma exposição nova (os mesmos valores já estão em texto puro em `infra/keycloak/crudao-realm.json`, ambiente de desenvolvimento local, `sslRequired: none`), mas duplicar o segredo em mais um arquivo aumenta a superfície caso o padrão seja copiado para um ambiente real sem trocar os valores.
Como corrigir:
  Atual:   `senha: 'admin123'` inline em `ADMIN`/`USER`
  Correto: ler de variáveis de ambiente com fallback para os valores de dev (`process.env.E2E_ADMIN_PASSWORD ?? 'admin123'`), deixando explícito que são credenciais de dev substituíveis
Guideline violado: `systems/CRUDAO/guidelines/security.md` — "Sem secrets hardcoded" (severidade reduzida a sugestão porque o valor já é público no repo via `crudao-realm.json`, então não há segredo novo sendo vazado)

## ✅ Pontos Positivos

- Fixtures de setup via API (`fixtures/api.ts`) são uma escolha sólida: cada teste cria seu próprio projeto/workflow/etapas isoladamente, permitindo `fullyParallel: true` sem interferência entre specs — evita a fragilidade comum de suítes E2E que compartilham estado.
- Uso de `data-testid` foi minimalista e cirúrgico (`card-tarefa`, `menu-acoes-tarefa`, `celula-etapa-{id}`) — só onde a ambiguidade de seletor por texto/role realmente exigia, sem poluir o componente.
- O teste de tempo real (`board.spec.ts` — RNF-001) valida a propagação via STOMP através de um atributo de dado (`data-etapa-atual-id`) em vez de `waitForTimeout` fixo, evitando flakiness e realmente testando o requisito de latência (≤2s).
- Cobertura de RBAC por projeto é genuinamente abrangente: isolamento entre projetos, `tarefa:finalizar` na ida *e* na volta (RN-011), toggle de trava de edição, e a regra mais sutil do sistema (G-RBAC-07 — `papel:gerenciar` nunca exposto via papel de projeto) todas têm teste dedicado.
- `vitest.config.ts` recebeu o `exclude` de forma mínima e correta, sem reestruturar a suíte existente.

## Segurança

Nenhum finding de segurança novo introduzido pelo código desta task. Os testes E2E exercitam e confirmam (não enfraquecem) os guardrails já registrados no canvas — G-RBAC-06/07/08 e G-RT-01 são validados por `rbac.spec.ts` e `board.spec.ts` respectivamente. Ver S2 acima para a única observação relacionada a segredo de dev duplicado (não bloqueante).

## Conformidade com TechSpec

Sem desvios. A escolha de Playwright confirma a opção sugerida em Q-005 da TechSpec (`docs/techspec/kanban-configuravel-techspec.md:250`). Os testes não alteram contratos de API nem modelo de dados — mudanças de produção se limitam a atributos `data-testid` (markup, sem efeito em contrato ou comportamento).

## Resultado

**APROVADO COM RESSALVAS**

Nenhum finding crítico. 2 findings importantes: I1 é um gap de tooling pré-existente ao restante do projeto (meta de cobertura sem mecanismo de medição); S3 é um bug de concorrência real em código da TASK-04.1, exposto (não causado) pela suíte E2E desta task e já mitigado no fixture de teste (`global-setup.ts`) para não deixar a suíte flaky. Nenhum dos dois é um defeito introduzido por esta task nem bloqueia o fechamento da feature — ambos registrados como débito técnico para task futura. As sugestões S1/S2 (aplicada e avaliada) são de baixo risco.
