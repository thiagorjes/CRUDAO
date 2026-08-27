---
name: analyze
description: Realiza análise cross-artefato entre PRD, TechSpec e Tasks no processo SDD, detectando inconsistências, lacunas de cobertura, ambiguidades e contradições. Use ao validar a consistência entre artefatos ou revisar especificações antes de iniciar a implementação.
canvas-dimensions: []
input-artifacts:
  - docs/prd/{{FEATURE}}-prd.md
  - docs/techspec/{{FEATURE}}-techspec.md
  - docs/tasks/{{FEATURE}}-tasks.md
  - docs/spdd/{{FEATURE}}-canvas.md
output-artifacts:
  - docs/analyze/{{FEATURE}}-analysis.md
---

## Objetivo

Verificar a consistência entre PRD, TechSpec, Tasks e o REASONS Canvas de uma feature antes de iniciar (ou prosseguir com) a implementação. Detecta RFs sem task correspondente, tasks sem RF de origem, divergências entre o canvas e a TechSpec, contradições entre artefatos e riscos de segurança não endereçados. Gera relatório agrupado por severidade, com métricas de cobertura e sugestão de remediação assistida.

## Argumentos recebidos

- (sem argumento) — analisa os artefatos mais recentes da feature ativa em `memory/state.md`
- `"nome-da-feature"` — analisa artefatos de uma feature específica
- `--security` — ativa a Fase 3.5 (passe de segurança) mesmo sem sinais óbvios no texto
- `--prd-only` — limita a análise à consistência interna do PRD (sem TechSpec/Tasks/Canvas)

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir
- `docs/techspec/[feature]-techspec.md` deve existir
- `docs/tasks/[feature]-tasks.md` deve existir
- `docs/spdd/[feature]-canvas.md` — opcional; se ausente, pular verificação de divergência de canvas e informar no relatório

## Workflow

### Fase 0 — Leitura de contexto

Ler nesta ordem, sem pular:

1. `docs/prd/[feature]-prd.md` — extrair todos os RFs e RNFs
2. `docs/techspec/[feature]-techspec.md` — extrair decisões técnicas e Matriz de Rastreabilidade
3. `docs/tasks/[feature]-tasks.md` — extrair todas as TASKs e seus RFs de origem declarados
4. `docs/spdd/[feature]-canvas.md`, se existir — extrair dimensões preenchidas

### Fase 1 — Mapeamento RF → Task (gaps)

1. Construir o conjunto de RFs do PRD e o conjunto de RFs referenciados em tasks
2. Identificar RFs do PRD sem nenhuma task correspondente (**gap de cobertura**)
3. Identificar tasks sem RF de origem declarado (**task órfã**)
4. Executar `check_rf_coverage.py` como verificação automatizada complementar (não substitui a leitura manual — RFs mencionados só no texto sem tag podem escapar ao regex)

### Fase 2 — Mapeamento Canvas ↔ TechSpec (divergências)

Se o canvas existir, comparar dimensão por dimensão com a TechSpec:

| Dimensão do canvas | Comparar com |
|---|---|
| E — Entities | Data model da TechSpec |
| A — Approach | Seção de decisões de arquitetura |
| S — Structure | Estrutura de módulos/componentes descrita |
| O — Operations | Lista de tasks do documento de Tasks |
| N — Norms | Guidelines referenciados |

Registrar toda divergência encontrada como **divergência de canvas**.

### Fase 3 — Contradições entre artefatos

Verificar se PRD, TechSpec e Tasks se contradizem entre si (ex: RNF de performance no PRD não refletido em nenhuma decisão técnica; critério de aceite de task que conflita com regra de negócio do PRD). Registrar como **contradição**.

### Fase 3.5 — Passe de segurança (`--security` ou quando encontrado em passagem normal)

Verificar, sem duplicar o trabalho do `/code-review` (que audita código já escrito — aqui a checagem é sobre a **especificação**):
- RFs com dados sensíveis (PII, financeiro, saúde) sem requisito de proteção declarado no PRD/TechSpec
- Endpoints do TechSpec sem autenticação/autorização declaradas
- Ausência de requisito de auditoria/log para operações críticas (criação, exclusão, mudança de permissão)
- RNFs de segurança sem critério mensurável (ex: "dados criptografados" sem algoritmo/padrão)

Registrar cada achado como **risco de segurança**.

### Fase 4 — Atribuição de severidade

Classificar cada achado das Fases 1-3.5:

| Severidade | Critério |
|---|---|
| 🔴 CRÍTICO | RF sem cobertura de tasks; violação de princípio DEVE da `constitution.md`; contradição direta entre documentos; dado sensível sem proteção declarada |
| 🟡 ALTO | RNF sem métrica mensurável; divergência de canvas em dimensão crítica (E/A/S); endpoint sem contrato ou sem auth |
| 🟠 MÉDIO | Task órfã (sem RF rastreável); inconsistência terminológica em área não crítica |
| 🔵 BAIXO | Melhoria de clareza; redundância não problemática |

### Fase 5 — Geração do relatório

Salvar progressivamente em `docs/analyze/[feature]-analysis.md`, agrupado por severidade:

```markdown
## Sumário
- Findings: 🔴 N críticos | 🟡 N altos | 🟠 N médios | 🔵 N baixos
- RFs com cobertura de tasks: N/N (N%)
- Veredicto: ✅ Aprovado para implementação | ⚠️ Aprovado com ressalvas | ❌ Requer correções

## Findings

| ID | Tipo | Severidade | Localização | Resumo | Recomendação |
|----|------|-----------|-------------|--------|-------------|
| G1 | Gap de cobertura | 🔴 | PRD: RF-005 | RF sem task associada | Criar task para RF-005 em /tasks |

## Cobertura RF × Tasks

| RF | Tasks | Status |
|----|-------|--------|
| RF-001 | TASK-1.1 | ✅ Coberto |
| RF-002 | — | ❌ Sem cobertura |
```

Salvar o arquivo após concluir cada fase (0 findings ainda é resultado válido a persistir, não motivo para pular a seção).

### Fase 6 — Remediação assistida

Ao final do relatório, perguntar:

> "Deseja que eu aplique as correções dos findings 🔴/🟡 nos artefatos correspondentes? Informe quais (ex: 'G1, S2') ou 'todos os críticos'. As alterações serão apresentadas para aprovação antes de salvar."

Esta skill **nunca aplica correções sem essa confirmação explícita** — a análise em si é somente leitura.

### Fase 7 — Validação e handoff

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/analyze/validate-rules.json \
  --artifact docs/analyze/[feature]-analysis.md
```

Se houver findings 🔴: alertar o usuário que `/implement` está bloqueado até resolução.

## Artefatos

**Entrada:**
- `docs/prd/[feature]-prd.md` (obrigatório)
- `docs/techspec/[feature]-techspec.md` (obrigatório)
- `docs/tasks/[feature]-tasks.md` (obrigatório)
- `docs/spdd/[feature]-canvas.md` (opcional)

**Saída:**
- `docs/analyze/[feature]-analysis.md`

## Canvas

Esta skill **não atualiza** o canvas. Lê as dimensões preenchidas apenas para comparação com a TechSpec na Fase 2, sem assinar nenhuma dimensão como `_Atualizado por: /analyze_`.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Análise executada:** /analyze — [data]
- **Findings:** 🔴 [N] | 🟡 [N] | 🟠 [N] | 🔵 [N]
- **Veredicto:** [✅|⚠️|❌]
- **Artefato:** docs/analyze/[feature]-analysis.md
```

Artifact Registry:
```
| analyze/[feature]-analysis.md | 1.0 | ok |
```
