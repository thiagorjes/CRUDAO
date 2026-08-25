---
name: discovery
description: Conduz levantamento leve de problema, personas e contexto de negócio, inicializando o REASONS Canvas com dimensões R e E em DRAFT. Use como porta de entrada do pipeline antes do /prd.
canvas-dimensions: [R, E]
input-artifacts:
  - memory/state.md
output-artifacts:
  - docs/discovery/{{FEATURE}}-discovery.md
  - docs/spdd/{{FEATURE}}-canvas.md
---

## Objetivo

Conduzir um levantamento rápido e estruturado focado em problema, personas e contexto de negócio. Gera `discovery.md` e inicializa o REASONS Canvas com dimensões R (Requirements) e E (Entities) em DRAFT. É a porta de entrada do pipeline SSPDD — pular esta skill é válido, mas o /prd fará perguntas equivalentes.

## Pré-condições

- Nenhuma — pode ser executada a qualquer momento, inclusive em projetos sem workspace inicializado
- Se `memory/state.md` existir: ler para contexto do projeto
- Se discovery.md já existir para esta feature: perguntar se deseja atualizar ou iniciar novo

## Workflow

### Fase 0 — Verificação de contexto

1. Verificar se já existe `docs/discovery/[feature]-discovery.md`
   - Se sim: perguntar "Deseja atualizar o discovery existente ou começar do zero?"
   - Se não: prosseguir
2. Ler `memory/state.md` se existir — absorver contexto de projeto sem perguntar o que já está documentado

### Fase 1 — Entrevista de problema (uma pergunta por vez)

Fazer as perguntas abaixo **uma de cada vez**, aguardando resposta antes de prosseguir. Pular perguntas cujas respostas já são conhecidas pelo contexto.

**Módulo A — Problema:**
- "Qual problema você está resolvendo? Descreva em 1-3 frases como se explicasse para alguém de fora da empresa."
- "Como esse problema se manifesta hoje? Qual é a dor concreta do usuário?"
- "Como você sabe que é um problema real? Há dados, reclamações ou evidências?"

**Módulo B — Personas:**
- "Quem tem esse problema? Descreva o usuário principal (perfil, contexto, objetivo)."
- "Existe um usuário secundário ou stakeholder que precisa ser considerado?"

**Módulo C — Objetivos de negócio:**
- "Quais são os 2-3 objetivos de negócio que esta solução deve atingir?"
- "Como você medirá que a solução foi bem-sucedida? Qual métrica de sucesso?"

**Módulo D — Hipótese de solução:**
- "Qual é a sua hipótese de solução? Não precisa ser definitiva — é um ponto de partida."
- "O que está explicitamente fora do escopo desta solução?"

### Fase 2 — Consolidação

Após obter as respostas, consolidar o entendimento em um parágrafo e confirmar com o usuário:
> "Entendi que [resumo em 3-4 frases]. Está correto antes de gerar os artefatos?"

Aguardar confirmação ou correções.

### Fase 3 — Geração progressiva dos artefatos

**Salvar progressivamente — não esperar concluir tudo antes de escrever.**

3.1. Criar/atualizar `docs/discovery/[feature]-discovery.md`:
   - Usar template `.agents/templates/[lang]/discovery-template.md`
   - Substituir todos os `{{PLACEHOLDER}}` com as respostas coletadas
   - Salvar imediatamente após preencher cada seção

3.2. Criar `docs/spdd/[feature]-canvas.md` (status DRAFT):
   - Usar template `.agents/templates/[lang]/canvas-template.md`
   - Preencher dimensão **R** com objetivos de negócio e escopo in/out
   - Preencher dimensão **E** com entidades de domínio identificadas (rascunho)
   - Marcar `_Atualizado por: /discovery v1.0 — [data]_` em R e E
   - Deixar dimensões A, S, O, N, S-safeguards com placeholder
   - Status permanece `DRAFT` (dimensão O ainda vazia)
   - Salvar após cada dimensão preenchida

### Fase 4 — Handoff

Informar ao usuário:
- Caminho dos artefatos gerados
- Que o /prd pode pular os módulos A e B (já cobertos)
- Sugerir próximo passo: `/prd [feature]`

Escrever bloco de handoff em `memory/state.md` (seção Features Ativas).

## Artefatos

**Entrada:**
- `memory/state.md` (opcional — contexto)

**Saída:**
- `docs/discovery/{{FEATURE}}-discovery.md` — levantamento completo
- `docs/spdd/{{FEATURE}}-canvas.md` — canvas em DRAFT com R e E preenchidos

**Template usado:** `.agents/templates/[lang]/discovery-template.md`, `.agents/templates/[lang]/canvas-template.md`

## Canvas

Esta skill atualiza as dimensões **R** e **E** do REASONS Canvas:

**R — Requirements:**
- Preencher com: objetivos de negócio, critérios de sucesso, escopo IN e OUT
- Referências a BDRs criadas nesta fase: `> Decisões: BDR-001, ...` (ou `> Decisões: —` se nenhuma)
- Ownership: `_Atualizado por: /discovery v1.0 — [data]_`

**E — Entities:**
- Preencher com: personas identificadas, entidades de domínio rascunho
- Nota: rascunho provisório — /prd e /techspec refinarão
- Referências a DRs criadas nesta fase: `> Decisões: —` (tipicamente vazia nesta fase, refinada depois)
- Ownership: `_Atualizado por: /discovery v1.0 — [data]_`

**Regra:** canvas criado sempre com `_Status: DRAFT_` — nunca muda para READY nesta fase.

## Handoff

Ao concluir, atualizar `memory/state.md`:

```
## Features Ativas

| Feature | Sistemas afetados | PRD | TechSpec | Tasks | Status |
|---|---|---|---|---|---|
| [FEATURE] | [SISTEMA] | — | — | — | Discovery concluído |
```

E adicionar ao Artifact Registry:

```
| docs/discovery/[feature]-discovery.md | 1.0 | ok |
| docs/spdd/[feature]-canvas.md | — | draft |
```
