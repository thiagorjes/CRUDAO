# Code Review — TASK-03.1 (Testes E2E de criação e exclusão de card)

_Feature: criacao-card-board | Sistema: CRUDAO | Data: 2026-08-24_

**Diff revisado** (staged/unstaged em `systems/CRUDAO/frontend/`):
- `e2e/criacao-exclusao-card.spec.ts` (novo — 8 cenários)
- `src/components/board/BoardApp.tsx` (fix de bug de duplicação de card, achado pelo próprio E2E)

## Gate de testes

- `tsc --noEmit`: limpo
- `eslint .`: limpo
- `next build`: limpo
- Suíte E2E completa (Playwright, contra `docker compose up` real): **22/22 passando** — 14 specs pré-existentes (`board.spec.ts`, `dashboard.spec.ts`, `rbac.spec.ts`) + 8 novos cenários desta task, sem regressão
- Gate satisfeito — prossegue para revisão.

## Critérios de Aceite

| # | Critério de Aceite | Verificado? | Evidência |
|---|---------------------|-------------|-----------|
| 1 | Cenário: criar card pela UI (etapa/raia padrão) | ✅ | `e2e/criacao-exclusao-card.spec.ts:17` |
| 2 | Cenário: botão "Novo card" oculto sem `tarefa:gerenciar` | ✅ | `e2e/criacao-exclusao-card.spec.ts:34` |
| 3 | Cenário: criação bloqueada em projeto finalizado | ✅ | `e2e/criacao-exclusao-card.spec.ts:44` |
| 4 | Cenário: excluir card pela UI (lixeira + confirmação) | ✅ | `e2e/criacao-exclusao-card.spec.ts:61` |
| 5 | Cenário: lixeira oculta sem permissão (dev-tier + toggle) | ✅ | `e2e/criacao-exclusao-card.spec.ts:77` |
| 6 | Cenário: exclusão propaga em tempo real ≤2s (RNF-001) | ✅ | `e2e/criacao-exclusao-card.spec.ts:95` |
| 7 | Cenário: workflow sem etapas → botão desabilitado (D-04) | ✅ | `e2e/criacao-exclusao-card.spec.ts:116` |
| 8 | Cenário combinado: criar + excluir na mesma sessão | ✅ | `e2e/criacao-exclusao-card.spec.ts:128` |
| 9 | Suíte completa revalidada sem regressão | ✅ | log de execução, 22/22 |
| 10 | `tsc`/`eslint`/`next build` limpos | ✅ | execução nesta sessão |

## 🔴 Crítico

Nenhum.

## 🟡 Importante

Nenhum.

## 🔵 Sugestão

#### S1 — Cenário "tempo real" testa propagação via API direta, não via 2 UIs
Arquivo: `e2e/criacao-exclusao-card.spec.ts:95-114`
Problema: o texto da task ("2 contextos de browser/sessão do Playwright") sugere que a ação que dispara o evento também deveria vir de uma segunda sessão de UI real; o teste dispara a exclusão via `admin.delete()` (API direta) e só observa a propagação em um segundo `browser.newContext()`. Cobre o requisito de propagação STOMP, mas não exercita uma segunda UI ativa gerando o evento.
Como corrigir: não bloqueante — é o mesmo padrão já usado em `board.spec.ts:104-125` (evento em tempo real de movimentação), que passou por review anterior sem esse ajuste. Manter consistência é preferível a divergir só nesta task; registrar como possível melhoria futura para toda a suíte de tempo real.
Guideline violado: não coberto — `testing.md` não especifica granularidade de fixture para cenários de tempo real.

#### S2 — `frontend/test-results/.last-run.json` aparece como modificado no git
Arquivo: `systems/CRUDAO/frontend/test-results/.last-run.json`
Problema: artefato de execução do Playwright está sendo versionado (não consta em `.gitignore`), gerando diff de ruído a cada execução local da suíte.
Como corrigir:
  Atual: `frontend/.gitignore` sem entrada para `test-results/`
  Correto: adicionar `/test-results/` ao `.gitignore` do frontend
Guideline violado: não coberto — nenhuma guideline trata de artefatos de teste versionados; recomendo adicionar a `git-workflow.md`.

## ✅ Pontos Positivos

- O fix da race condition em `BoardApp.tsx` foi encontrado pelo próprio teste E2E (flakiness sob paralelismo), não por inspeção manual — exatamente o valor que a suíte E2E deveria entregar. A correção é mínima e cirúrgica: reaproveita o guard de projeto já existente e apenas acrescenta a checagem de existência do id antes de inserir, sem alterar contrato ou comportamento em nenhum outro caminho.
- Os 8 cenários seguem fielmente o padrão de fixtures já estabelecido (`ApiCliente`/`criarCenario`/`loginUi`, projeto isolado por teste, seletores por `data-testid`), sem introduzir nenhuma variação de estilo.
- Uso de papéis `dev`/`gestor` do catálogo real do seed (não strings arbitrárias) para os cenários de permissão negada, consistente com `rbac.spec.ts`.

## Segurança

Nenhum finding de segurança. A alteração em `BoardApp.tsx` é puramente de estado local no cliente (dedupe de renderização); toda autorização continua validada no backend (RNF-003/ADR-006), inclusive nos dois cenários de permissão negada desta task, que testam apenas o gating de UI — consistente com o escopo da feature (D-05 da TechSpec: "nenhum RBAC novo"). Débito G-RT-01 (subscription STOMP sem checagem de membro do projeto) é herdado e já documentado na TechSpec §8; esta task não o agrava nem o corrige.

## Conformidade com TechSpec

Sem desvios. A task não alterou nenhuma decisão de D-01 a D-05; o fix em `BoardApp.tsx` é um ajuste de robustez do já implementado em TASK-02.2 (D-04), não uma mudança de abordagem.

## Resultado

**APROVADO**

Nenhum item bloqueia o merge. As 2 sugestões (🔵) ficam como débito não-bloqueante — S2 é rápida de aplicar em qualquer momento futuro; S1 é uma nota de consistência, não uma lacuna de cobertura.
