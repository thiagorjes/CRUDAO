# Code Review — TASK-07.6 (Dashboard UI)

_Revisor: /code-review v1.0 — 2026-09-01_
_Sistema: CRUDAO (frontend) · Feature: kanban-tarefas · RF-007_

## Gate de testes

- `npx tsc --noEmit` ✅ sem erros
- `npx vitest run` ✅ 5 testes, 2 arquivos (não há testes específicos de dashboard — frontend sem cobertura de UI, padrão pré-existente do repo)
- `npx eslint` ❌ não executável (config ESLint v9 ausente no repo — pré-existente, não introduzido por esta task)
- Revisão majoritariamente estática + verificação de contrato.

## Critérios de Aceite

| # | Critério | Verificado? | Evidência |
|---|----------|-------------|-----------|
| 1 | Dashboard acessível a gestor sem permissões de execução | ✅ | `app/(dashboard)/projetos/[id]/dashboard/page.tsx` e `app/api/dashboard/[projetoId]/route.ts` não aplicam nenhum gate de permissão client-side; autorização fica no backend (`permissaoGuard.membro()`), conforme princípio 3 da constituição. Middleware só exige sessão. |
| 2 | Dados batem com o retornado pelo backend | ✅ | `lib/types.ts:146-155` (`EtapaLeadTime`/`Dashboard`) agora idênticos ao contrato `dashboard-notificacoes.md:11-14` e ao record `DashboardResponse.java` (`leadTimeMedioPorEtapa`, `leadTimeMedioSegundos`, `tempoImpedimentoMedioSegundos`, `totalTarefasConsideradas`). `DashboardView.tsx` trata os valores como **segundos** (`formatarTempo`), unidade correta do contrato. |

## 🔴 Crítico

Nenhum.

## 🟡 Importante

Nenhum.

## 🔵 Sugestão

#### S1 — Import fora do padrão do barrel `@/lib/api`
Arquivo: `app/api/dashboard/[projetoId]/route.ts:2`
Problema: importa `apiProxyFetch` de `@/lib/api/proxy`; a rota irmã `app/api/board/[projetoId]/route.ts:2` importa de `@/lib/api` (barrel). Divergência estética sem impacto funcional.
Como corrigir: `import { apiProxyFetch } from "@/lib/api";`
Guideline violado: não coberto explicitamente — recomendo padronizar imports via barrel em `coding-standards.md`.

#### S2 — `projetoId` interpolado no path do backend sem validação de formato
Arquivo: `app/api/dashboard/[projetoId]/route.ts:12`
Problema: segue o padrão das demais rotas (board/tarefas), que também não validam. Risco residual baixo (segmento dinâmico do Next.js não contém `/`; backend valida UUID e responde 400). Validar o formato UUID no route handler reduziria superfície e daria erro mais claro.
Como corrigir: rejeitar com 400 quando `projetoId` não casar `/^[0-9a-f-]{36}$/i` — idealmente num helper compartilhado em `lib/api/`.
Guideline violado: `security.md` — validação de input em pontos de entrada externos (parcialmente; padrão já é frouxo no repo).

#### S3 — Sem teste automatizado da view/route
Arquivo: `components/dashboard/DashboardView.tsx`, `app/api/dashboard/[projetoId]/route.ts`
Problema: `formatarTempo` (conversão s→d/h/m/s) e o mapeamento de status 403/404 não têm teste. São candidatos naturais a teste unitário com vitest.
Guideline violado: `testing.md` — lógica de apresentação com ramificações merece teste; não bloqueante para UI neste momento do projeto.

## ✅ Pontos Positivos

- **Correção de contrato real**: o rascunho anterior de `types.ts` assumia campos inexistentes (`tempoMedioImpedimento` agregado em ms, `impedimentoPorEtapa`, `duracao`); agora alinhado 1:1 com backend + contrato. Bug evitado antes do merge.
- `formatarTempo` faz clamp de negativos (`Math.max(0, …)`) e trata o caso zero.
- Estados de `loading` / `erro` / `dashboard` nulo tratados explicitamente na página.
- Rota de API mantém o mesmo shape de tratamento de erro das rotas irmãs (try/catch + `console.error` com tag + status propagado).
- Nenhuma barreira de autorização client-side — respeita o princípio "UI nunca é a única barreira".

## Qualidade de Código

- Nomenclatura pt_BR consistente com o restante do frontend (`carregar`, `formatarTempo`, `erro`).
- `DashboardView` é função pura de apresentação; `page.tsx` concentra fetch/estado — separação adequada.
- `formatarTempo` reescrito para segundos com aritmética por resto (sem acúmulo de `%` sobre valores já divididos); clamp de negativo e caso zero cobertos.
- Sem duplicação: rota segue o mesmo esqueleto try/catch das rotas irmãs.
- Edge cases: lista vazia (`etapas.length === 0` → "Sem dados"), `dashboard` nulo, `loading`, `erro` — todos tratados.
- Débito menor: ausência de teste unitário para `formatarTempo` (ver S3) e import fora do barrel (ver S1).

## Segurança

- Autenticação: `apiProxyFetch` injeta `Authorization: Bearer` a partir do cookie `session`. ✅
- Autorização: delegada ao backend (`membro()` / RN-015 permite leitura em projeto FINALIZADO). Frontend não confia na UI. ✅
- Sem secrets hardcoded; sem logging de dados sensíveis (`console.error` registra só a tag + objeto de erro). ✅
- Path traversal: ver S2 — risco residual baixo.
- Dependências: nenhuma nova adicionada nesta task.

## Conformidade com TechSpec

- Contrato `GET /api/projetos/{projetoId}/dashboard` (`dashboard-notificacoes.md`) consumido conforme especificado (campos e unidade em segundos).
- RN-015 (dashboard acessível em projeto finalizado): respeitado — frontend não bloqueia; backend permite.
- Pendência de PO herdada de TASK-06.1 (RN-002 "total agregado" não exposto no payload) permanece aberta e **fora do escopo desta task** — a UI exibe `totalTarefasConsideradas`, único agregado disponível no contrato.

## Resultado

**APROVADO** — 0 críticos, 0 importantes, 3 sugestões adiáveis. Pronto para merge.
