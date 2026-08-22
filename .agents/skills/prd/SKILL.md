---
name: prd
description: Conduz entrevista estruturada de requisitos e gera PRD completo com RFs, RNFs, regras de negócio e critérios de aceite Gherkin. Use no início de qualquer nova feature após /discovery (ou direto, se discovery não foi executado).
canvas-dimensions: [R]
input-artifacts:
  - memory/state.md
  - docs/discovery/{{FEATURE}}-discovery.md
output-artifacts:
  - docs/prd/{{FEATURE}}-prd.md
  - docs/spdd/{{FEATURE}}-canvas.md
---

## Objetivo

Capturar e documentar requisitos funcionais, não-funcionais, regras de negócio e critérios de aceite em formato PRD padronizado. Integra com `/discovery` (pula módulos já respondidos) e atualiza a dimensão R do REASONS Canvas com RFs validados com Gherkin.

## Pré-condições

- `memory/state.md` deve existir (criado pelo `init.py`)
- Se `/discovery` foi executado: `docs/discovery/[feature]-discovery.md` deve existir — será lido automaticamente
- Se discovery não existe: coletar informações equivalentes durante a entrevista

## Workflow

### Fase 0 — Leitura de contexto

1. Ler `memory/state.md` — absorver contexto do projeto sem perguntar o que já está documentado
2. Verificar se existe `docs/discovery/[feature]-discovery.md`:
   - **Se existe:** ler e absorver Módulos A (problema) e B (personas) — **pular essas perguntas** na entrevista
   - **Se não existe:** coletar essas informações durante os módulos abaixo
3. Verificar se já existe `docs/prd/[feature]-prd.md`:
   - Se sim: perguntar "Deseja atualizar o PRD existente (bump de versão) ou criar do zero?"

### Fase 1 — Entrevista de requisitos (uma pergunta por vez)

Fazer perguntas **uma de cada vez**, aguardando resposta. Pular módulos já cobertos pelo discovery.

**Módulo A — Contexto do produto** (pular se discovery cobriu):
- "Qual o problema central que esta feature resolve?"
- "Quem são os usuários afetados?"

**Módulo B — Requisitos funcionais** (sempre executar):
- "Liste as funcionalidades que o sistema DEVE ter (Must Have)."
- Para cada funcionalidade: "Como um usuário saberia que isso funciona? Descreva o comportamento esperado."
- "Há funcionalidades que seriam boas ter mas não são críticas para o lançamento (Should Have)?"
- "Alguma funcionalidade foi discutida mas ficará para versões futuras (Won't Have agora)?"

**Módulo C — Requisitos não-funcionais:**
- "Há requisitos de performance? (ex: tempo de resposta, volume de usuários)"
- "Há requisitos de segurança ou compliance?"
- "Há restrições de portabilidade ou compatibilidade?"

**Módulo D — Regras de negócio:**
- "Há regras de negócio específicas que o sistema deve respeitar? (validações, limites, cálculos)"

**Módulo E — Escopo e stakeholders:**
- "O que está explicitamente fora do escopo?"
- "Quem precisa aprovar este PRD antes de passar para TechSpec?"

### Fase 2 — Geração progressiva do PRD

**Salvar a cada seção concluída — não esperar o documento completo.**

2.1. Criar `docs/prd/[feature]-prd.md` usando template `.agents/templates/[lang]/prd-template.md`

2.2. Preencher e salvar seção por seção:
   - Seção 1 (Visão Geral) → salvar
   - Seção 2 (Stakeholders) → salvar
   - Seção 3 (RFs) — para cada RF:
     - Atribuir ID sequencial (RF-001, RF-002, ...)
     - Escrever em formato "Como [persona], quero [ação], para [objetivo]"
     - Adicionar critério de aceite Gherkin: **Dado que** / **Quando** / **Então**
     - Atribuir prioridade (Must/Should/Could/Won't)
     - Salvar após cada RF
   - Seção 4 (RNFs) com métricas mensuráveis → salvar
   - Demais seções → salvar ao final de cada uma

2.3. Ao concluir: executar validação
   ```
   python .agents/scripts/validate.py --mode output \
     --rules .agents/skills/prd/validate-rules.json \
     --artifact docs/prd/[feature]-prd.md
   ```
   - Se exit 1: corrigir os ERROs antes de prosseguir
   - Se exit 0: informar ao usuário que o PRD foi validado

### Fase 3 — Atualização do Canvas

Atualizar dimensão **R** do canvas `docs/spdd/[feature]-canvas.md`:
- Se canvas não existe: criar usando template
- Preencher R com: objetivos do PRD, lista de RFs Must Have, escopo IN/OUT
- Atualizar ownership: `_Atualizado por: /prd v1.0 — [data]_`
- Adicionar referência a BDRs criados (se houver decisões de escopo/priorização)
- Salvar canvas imediatamente

### Fase 4 — Handoff

Atualizar `memory/state.md`:
- Seção Features Ativas: adicionar/atualizar entry com PRD v1.0 e status
- Artifact Registry: adicionar entrada `docs/prd/[feature]-prd.md | 1.0 | ok`
- Se canvas foi criado/atualizado: `docs/spdd/[feature]-canvas.md | — | draft`

Informar ao usuário:
- Caminho do PRD gerado
- Resultado da validação
- Próximo passo sugerido: `/clarify` (se houver questões em aberto) ou `/techspec [feature] --system [sistema]`

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)
- `docs/discovery/[feature]-discovery.md` (opcional — pula módulos A e B)

**Saída:**
- `docs/prd/[feature]-prd.md` — PRD completo e validado
- `docs/spdd/[feature]-canvas.md` — dimensão R atualizada

**Validação:** `python .agents/scripts/validate.py --mode output --rules .agents/skills/prd/validate-rules.json --artifact [prd]`

## Canvas

Esta skill atualiza a dimensão **R** do REASONS Canvas:

**R — Requirements:**
- Preencher com: lista de RFs Must Have com IDs, objetivos de negócio, escopo IN/OUT
- Referências a BDRs de decisão de escopo: `> Decisões: BDR-001, ...`
- Ownership: `_Atualizado por: /prd v1.0 — [data]_`

**Nota:** dimensão R refinada em relação ao /discovery — RFs têm IDs formais e Gherkin.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
### [FEATURE_NAME]
- **Etapa concluída:** /prd (v1.0) — [data]
- **Artefato:** docs/prd/[feature]-prd.md
- **RFs Must Have:** RF-001, RF-002, ...
- **Questões em aberto:** [listar ou "nenhuma"]
- **Próximo comando:** /techspec [feature] --system [sistema]
```

Artifact Registry:
```
| docs/prd/[feature]-prd.md | 1.0 | ok |
```
