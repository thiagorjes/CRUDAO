---
name: techspec
description: Gera especificação técnica completa a partir do PRD e guidelines do sistema, incluindo decisões arquiteturais, data model, contratos de API, estratégia de testes e matriz de rastreabilidade. Atualiza dimensões E, A, S, N do canvas. Use após aprovar o PRD.
canvas-dimensions: [E, A, S, N]
input-artifacts:
  - memory/state.md
  - docs/prd/{{FEATURE}}-prd.md
  - systems/{{SYSTEM}}/guidelines/stack.md
  - systems/{{SYSTEM}}/guidelines/architecture.md
output-artifacts:
  - docs/techspec/{{FEATURE}}-techspec.md
  - docs/techspec/{{FEATURE}}/data-model.md
  - docs/spdd/{{FEATURE}}-canvas.md
---

## Objetivo

Transformar o PRD aprovado em especificação técnica executável, tomando todas as decisões de design antes da implementação. Integra guidelines do sistema, detecta dependências inter-sistemas com opção de mock, atualiza quatro dimensões do canvas e cria ADRs técnicos.

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir com status `ok` no Artifact Registry
- `systems/[sistema]/guidelines/` deve existir com guidelines gerados pelo /guidelines
- Se guidelines ausentes: **instruir o usuário** a executar `/guidelines --system [sistema]` e aguardar antes de prosseguir
- `memory/state.md` atualizado

## Workflow

### Fase 0 — Pré-condições e dependências

**0.1 — Verificar guidelines locais:**
```
Verificar: systems/[sistema]/guidelines/stack.md existe?
```
- Se **não existe**: instruir o usuário:
  > "Guidelines do sistema '[sistema]' não encontrados. Execute `/guidelines --system [sistema]` primeiro e retorne."
  Aguardar — não prosseguir.
- Se **existe**: ler todos os 9 arquivos de guidelines antes de continuar.

**0.2 — Verificar dependências inter-sistemas:**
Para cada sistema integrado mencionado no PRD:
- Verificar se `systems/[outro-sistema]/guidelines/` existe localmente
  - **Sistema próprio ausente:** instruir `git clone <repo> systems/[sistema]` e aguardar
  - **Sistema terceiro:** solicitar documentação da API ou swagger
  - **Indisponível:** oferecer criação de mock contract
    > "Sistema '[X]' não disponível localmente. Deseja criar um mock contract para prosseguir? O mock será marcado como PENDENTE DE VALIDAÇÃO e gerará uma task de substituição."
    - Se aceito: criar `docs/contracts/[X]-mock-contract.md` usando template mock-contract

**0.3 — Verificar stale no PRD:**
```
python .agents/scripts/validate.py --mode input \
  --rules .agents/skills/techspec/validate-rules.json \
  --artifact docs/prd/[feature]-prd.md \
  --system [sistema]
```
Se stale: alertar e aguardar confirmação para prosseguir com `--force` consciente.

### Fase 1 — Decisões técnicas (com o usuário)

Fazer perguntas técnicas necessárias **uma de cada vez**, absorvendo o máximo dos guidelines sem perguntar o que já está decidido:

**Módulo A — Abordagem técnica:**
- "Qual abordagem arquitetural para esta feature? (ex: REST API, event-driven, batch)"
- "Há alguma decisão técnica específica desta feature que difere do padrão dos guidelines?"

**Módulo B — Modelo de dados:**
- "Quais entidades novas esta feature introduz?"
- "Quais entidades existentes serão modificadas?"
- "Há migrações de banco de dados necessárias?"

**Módulo C — Integrações:**
- "Quais sistemas ou serviços externos esta feature consome ou expõe?"
- Para cada integração: "Qual o contrato esperado? Há documentação disponível?"

Para cada decisão técnica que envolva trade-off: criar ADR.

### Fase 2 — Geração progressiva da TechSpec

**Salvar a cada seção concluída.**

2.1. Criar `docs/techspec/[feature]-techspec.md` usando template techspec
   - Seção 1 (Visão Geral Técnica) → salvar
   - Seção 2 (Decisões Arquiteturais) com referências aos ADRs criados → salvar
   - Seção 3 (Modelo de Dados) — criar também `docs/techspec/[feature]/data-model.md` → salvar ambos
   - Seção 4 (Contratos de API/Interface) → salvar
   - Seção 5 (Arquitetura e Fluxo) → salvar
   - Seção 6 (Dependências Inter-Sistemas) — incluir mocks se criados → salvar
   - Seção 7 (Estratégia de Testes) → salvar
   - Seção 8 (Segurança e Observabilidade) → salvar
   - Seção 9 (Matriz de Rastreabilidade) — mapear cada RF do PRD → salvar
     - Executar `check_rf_coverage.py` como verificação automatizada complementar (não substitui o mapeamento manual):
       ```
       python .agents/skills/techspec/scripts/check_rf_coverage.py \
         --prd docs/prd/[feature]-prd.md \
         --techspec docs/techspec/[feature]-techspec.md
       ```
       Se detectar RF sem cobertura: completar a matriz antes de prosseguir.
   - Seção 10 (Questões em Aberto) → salvar

2.2. Se mock contracts criados: gerar task de substituição e documentar em Seção 6

2.3. Validação ao final:
```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/techspec/validate-rules.json \
  --artifact docs/techspec/[feature]-techspec.md \
  --system [sistema]
```

### Fase 3 — Atualização do Canvas (4 dimensões)

Salvar cada dimensão no canvas `docs/spdd/[feature]-canvas.md` individualmente:

**E — Entities:** diagrama/lista de entidades do data model
- `_Atualizado por: /techspec v1.0 — [data]_`
- `> Decisões: DDR-[NNN], ...`

**A — Approach:** estratégia técnica escolhida, trade-offs aceitos
- `_Atualizado por: /techspec v1.0 — [data]_`
- `> Decisões: ADR-[NNN], ...`

**S — Structure:** arquitetura de componentes, dependências externas
- `_Atualizado por: /techspec v1.0 — [data]_`
- `> Decisões: ADR-[NNN], ...`

**N — Norms:** padrões relevantes extraídos dos guidelines para esta feature
- `_Atualizado por: /techspec v1.0 — [data]_`
- `> Decisões: —`

Salvar canvas após cada dimensão atualizada.

### Fase 4 — Handoff

Atualizar `memory/state.md`:
- Artifact Registry: `docs/techspec/[feature]-techspec.md | 1.0 | ok`
- Se PRD foi mantido sem alteração: TechSpec status = `ok`
- Marcar feature como "Em especificação técnica → pronto para /tasks"

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)
- `docs/prd/[feature]-prd.md` (obrigatório — deve estar `ok`)
- `systems/[sistema]/guidelines/*.md` (obrigatório — todos os 9)

**Saída:**
- `docs/techspec/[feature]-techspec.md` — TechSpec principal
- `docs/techspec/[feature]/data-model.md` — modelo de dados detalhado
- `docs/contracts/[X]-mock-contract.md` — se sistemas externos indisponíveis
- `docs/decisions/ADR-[NNN]-*.md` — ADRs de decisões técnicas
- `docs/spdd/[feature]-canvas.md` — dimensões E, A, S, N atualizadas

## Canvas

Esta skill atualiza **4 dimensões** do REASONS Canvas:

**E — Entities:** entidades do data model com atributos e relacionamentos
**A — Approach:** estratégia de solução, padrão arquitetural, trade-offs
**S — Structure:** componentes, camadas, dependências externas
**N — Norms:** padrões dos guidelines mais relevantes para esta feature

Ao atualizar cada dimensão, adicionar referências às DRs (ADR/SDR/DDR) criadas na mesma fase na linha `> Decisões:` (ex: `> Decisões: ADR-002, SDR-001` ou `> Decisões: —` se nenhuma).

Após atualizar E, A, S, N: verificar se todas as 7 dimensões estão preenchidas.
- Se R e O ainda estiverem vazias: canvas permanece `DRAFT`
- Se apenas O estiver vazia: canvas permanece `DRAFT` (aguarda /tasks)

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
### [FEATURE_NAME]
- **Etapa concluída:** /techspec (v1.0) — [data]
- **Artefatos:** docs/techspec/[feature]-techspec.md + data-model.md
- **Sistemas afetados:** [lista]
- **Mock contracts:** [lista ou "nenhum"]
- **Próximo comando:** /tasks [feature]
```

Artifact Registry:
```
| docs/techspec/[feature]-techspec.md | 1.0 | ok |
| docs/techspec/[feature]/data-model.md | 1.0 | ok |
```
