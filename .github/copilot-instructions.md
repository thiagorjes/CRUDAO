# SSPDD Copilot Instructions

## /analyze


## Objetivo

Verificar a consistência entre PRD, TechSpec, Tasks e o REASONS Canvas de uma feature antes de iniciar (ou prosseguir com) a implementação. Detecta RFs sem task correspondente, tasks sem RF de origem, divergências entre o canvas e a TechSpec, e contradições entre artefatos. Gera relatório agrupado por tipo de achado.

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

### Fase 4 — Geração do relatório

Salvar progressivamente em `docs/analyze/[feature]-analysis.md`, agrupado por tipo de achado:

```markdown
## Sumário
[contagem por tipo: N gaps, N divergências, N contradições]

## Gaps
- RF-XXX presente no PRD sem task correspondente
- TASK-XX.X sem RF de origem declarado

## Divergências
- Dimensão [X] do canvas diverge da TechSpec em [ponto]

## Contradições
- [artefato A] afirma [X], [artefato B] afirma [Y]
```

Salvar o arquivo após concluir cada fase (0 gaps ainda é resultado válido a persistir, não motivo para pular a seção).

### Fase 5 — Validação e handoff

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/analyze/validate-rules.json \
  --artifact docs/analyze/[feature]-analysis.md
```

Se houver gaps críticos (RF sem task): alertar o usuário antes de recomendar avançar para `/implement`.

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
- **Gaps:** [N] | **Divergências:** [N] | **Contradições:** [N]
- **Artefato:** docs/analyze/[feature]-analysis.md
```

Artifact Registry:
```
| analyze/[feature]-analysis.md | 1.0 | ok |
```


## /checklist


## Objetivo

Aplicar um checklist de qualidade a um artefato (PRD ou TechSpec), tratando cada requisito como um caso a validar — não a implementação em si, mas a qualidade da especificação. Gera `docs/checklists/[feature]-[tipo].md` diferenciando itens críticos (bloqueiam a próxima etapa do pipeline) de itens não-críticos (melhorias sugeridas).

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir (mínimo)
- `docs/techspec/[feature]-techspec.md` — necessário apenas se o checklist for do tipo `techspec`
- `memory/state.md` com a feature registrada no Artifact Registry

## Workflow

### Fase 0 — Leitura de contexto

1. Perguntar (se não informado nos argumentos) qual artefato validar: PRD ou TechSpec
2. Ler o artefato-alvo por completo
3. `memory/state.md` — confirmar versão do artefato no Artifact Registry

### Fase 1 — Aplicação do checklist de qualidade

Para cada requisito (RF/RNF no PRD, decisão técnica na TechSpec), verificar:

| Critério | Pergunta |
|---|---|
| Completude | O requisito descreve o comportamento esperado por completo, sem lacunas? |
| Clareza | Está livre de termos vagos ("rápido", "adequado", "amigável")? |
| Mensurabilidade | RNFs têm limiar numérico? RFs têm critério de aceite testável (Gherkin)? |
| Consistência | Não contradiz outro requisito do mesmo documento? |
| Rastreabilidade | Tem ID único e referência a fonte (ex: RF-XXX)? |

Cada falha encontrada vira um item do checklist, com ID sequencial `CHK-NNN`.

### Fase 2 — Classificação crítico vs. não-crítico

Classificar cada item:
- **Crítico:** bloqueia a próxima etapa do pipeline (ex: RNF sem métrica impede `/techspec` de dimensionar a solução)
- **Não-crítico:** melhoria de qualidade que não impede avançar, mas deveria ser corrigida

### Fase 3 — Geração do checklist

Salvar progressivamente em `docs/checklists/[feature]-[tipo].md`:

```markdown
## Sumário
[N itens críticos, N itens não-críticos]

## Itens Críticos
- CHK-001 — [requisito] — [problema] — bloqueia: [etapa]

## Itens Não-Críticos
- CHK-002 — [requisito] — [sugestão de melhoria]
```

Salvar o arquivo assim que a Fase 1 identificar os primeiros itens — não aguardar terminar a varredura completa do documento.

### Fase 4 — Validação e handoff

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/checklist/validate-rules.json \
  --artifact docs/checklists/[feature]-[tipo].md
```

Se houver itens críticos: alertar o usuário que a próxima etapa do pipeline está bloqueada até resolução.

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)
- `docs/prd/[feature]-prd.md` ou `docs/techspec/[feature]-techspec.md` (o artefato-alvo)

**Saída:**
- `docs/checklists/[feature]-[tipo].md`

## Canvas

Esta skill **não atualiza** o canvas — atua sobre a qualidade de PRD/TechSpec, artefatos anteriores ao canvas no pipeline.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Checklist executado:** /checklist [tipo] — [data]
- **Itens críticos:** [N] | **Itens não-críticos:** [N]
- **Artefato:** docs/checklists/[feature]-[tipo].md
```

Artifact Registry:
```
| checklists/[feature]-[tipo].md | 1.0 | ok |
```


## /clarify


## Objetivo

Varrer o PRD em busca de ambiguidades — termos vagos, métricas não quantificadas, critérios de aceite imprecisos, casos de borda não cobertos — e resolvê-las com o usuário uma pergunta por vez, atualizando o PRD incrementalmente. Ao final, faz bump de versão MINOR do PRD e propaga o status `stale` aos artefatos downstream no Artifact Registry.

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir
- `memory/state.md` deve conter a entrada da feature no Artifact Registry

## Workflow

### Fase 0 — Leitura de contexto

1. `docs/prd/[feature]-prd.md` — ler o documento completo
2. `memory/state.md` — verificar versão atual do PRD e quais artefatos downstream (TechSpec, Tasks, Canvas) já existem no Artifact Registry

### Fase 1 — Identificação de ambiguidades

Varrer o PRD buscando:
- Termos vagos ("rápido", "simples", "adequado") sem métrica associada
- RNFs sem número/limiar quantificável
- Critérios de aceite sem Gherkin ou com passos incompletos
- Casos de borda mencionados na seção "Fora do Escopo" que na verdade deveriam estar cobertos
- RFs contraditórios entre si

Construir a lista de ambiguidades encontradas antes de perguntar — não interromper a leitura no meio.

### Fase 2 — Resolução (uma pergunta por vez)

**Regra crítica (princípio "one question at a time" do workspace):** apresentar uma ambiguidade por vez, nunca em lote. Para cada uma:

1. Citar o trecho ambíguo do PRD (referência de seção, não o texto completo se longo)
2. Perguntar a resposta específica que resolve a ambiguidade
3. Aguardar resposta do usuário
4. Atualizar o PRD imediatamente com a resposta antes de passar para a próxima ambiguidade

**Salvar o arquivo após cada resposta aplicada — não acumular mudanças para o final.**

### Fase 3 — Bump de versão e propagação de stale

Ao concluir todas as ambiguidades:

1. Incrementar a versão do PRD em MINOR (ex: `1.1` → `1.2`)
2. Atualizar `memory/state.md`:
   - Artifact Registry: nova versão do PRD, status `ok`
   - Para cada artefato downstream já gerado (TechSpec, Tasks, Canvas): marcar status `stale:prd@[nova-versão]`
3. Informar ao usuário quais artefatos downstream precisam ser regenerados

### Fase 4 — Validação e handoff

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/clarify/validate-rules.json \
  --artifact docs/prd/[feature]-prd.md
```

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)
- `docs/prd/[feature]-prd.md` (obrigatório)

**Saída:**
- `docs/prd/[feature]-prd.md` — atualizado in-place, versão MINOR incrementada

## Canvas

Atualiza a dimensão:
- **R — Reasons:** quando a clarificação envolve o motivo de negócio ou objetivo original do RF, ajustar a dimensão R do canvas (se já existir) refletindo o motivo esclarecido, marcando `_Atualizado por: /clarify v1.0 — [data]_`
- Se a clarificação originar uma nova BDR, adicionar sua referência na linha `> Decisões:` da dimensão R

Se o canvas ainda não existir para a feature, pular esta etapa — ele será gerado por `/spdd-canvas` ou `/techspec` posteriormente já com o PRD clarificado.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Clarificação executada:** /clarify — [data]
- **Ambiguidades resolvidas:** [N]
- **PRD:** v[nova-versão]
- **Artefatos marcados stale:** [lista, ou "nenhum"]
```

Artifact Registry:
```
| docs/prd/[feature]-prd.md | [nova-versão] | ok |
| techspec/[feature]-techspec.md | [versão atual] | stale:prd@[nova-versão] |
```


## /code-review


## Objetivo

Revisar o código implementado contra: TechSpec, guidelines do sistema, critérios de aceite da task e padrões de segurança (OWASP Top 10 mínimo). Ao concluir, extrai guardrails descobertos durante a revisão e atualiza a dimensão S do canvas — podendo transitar o canvas para READY se S for a última dimensão faltante.

## Pré-condições

- Código implementado disponível (diff ou arquivos)
- `docs/tasks/[feature]-tasks.md` com a task revisada
- `docs/techspec/[feature]-techspec.md`
- `docs/spdd/[feature]-canvas.md`
- `systems/[sistema]/guidelines/` para referência de padrões

## Workflow

### Fase 0 — Leitura de contexto

1. Identificar a task sendo revisada (ID e critérios de aceite)
2. Ler `docs/spdd/[feature]-canvas.md` — dimensão S atual (Safeguards já conhecidos)
3. Ler `docs/techspec/[feature]-techspec.md` — seção de Segurança e Observabilidade
4. Ler guidelines relevantes: `security.md`, `coding-standards.md`, `testing.md`

### Fase 1 — Revisão por categoria

Revisar o código em **5 categorias obrigatórias**, documentando findings com localização:

**1. Critérios de aceite da task:**
- Cada critério de aceite está implementado?
- O comportamento corresponde ao especificado no Gherkin?

**2. Qualidade de código:**
- Nomenclatura segue as normas de N do canvas e guidelines?
- Funções têm responsabilidade única e tamanho adequado?
- Sem código duplicado que deveria ser abstraído?
- Sem complexidade desnecessária ou over-engineering?
- Cobertura de erros e edge cases adequada?

**3. Segurança (obrigatória — nunca pular):**
- Input validation presente em todos os pontos de entrada externos?
- Sem secrets hardcoded (chaves, senhas, tokens)?
- SQL injection / command injection / path traversal prevenidos?
- Autenticação e autorização aplicadas corretamente?
- Logging não expõe dados sensíveis?
- Dependências sem vulnerabilidades conhecidas?

**4. Arquitetura e TechSpec:**
- Implementação segue a abordagem definida na TechSpec (dimensão A do canvas)?
- Entidades e estrutura de dados consistentes com data-model.md?
- Contratos de API respeitados?
- Decisões arquiteturais (ADRs) respeitadas?

**5. Observabilidade e operação:**
- Logs estruturados nos pontos críticos?
- Métricas instrumentadas se definido na TechSpec?
- Tratamento de erros com contexto suficiente para debug?

### Fase 2 — Geração do relatório

Criar `docs/checklists/[feature]-[task-id]-review.md` com:

**Formato de finding:**
```
[CRÍTICO|IMPORTANTE|SUGESTÃO]: [arquivo:linha] — [descrição do problema]
Recomendação: [o que fazer]
```

**Seções obrigatórias do relatório:**
- `## Segurança` — findings de segurança (vazio = "Nenhum finding de segurança")
- `## Qualidade de Código` — findings de qualidade
- `## Conformidade com TechSpec` — desvios da especificação
- `## Observabilidade` — findings de logs/métricas
- `## Resultado` — APROVADO | APROVADO COM RESSALVAS | REPROVADO

Salvar progressivamente por seção.

### Fase 3 — Extração de Safeguards e atualização do Canvas

**3.1 — Extrair guardrails da revisão:**
Identificar restrições e padrões "o que NÃO fazer" descobertos durante a revisão.

**3.2 — Atualizar dimensão S do canvas:**
```markdown
## S — Safeguards

_Atualizado por: /code-review v1.0 — [data]_
> Decisões: ADR-[NNN] (se houver ADR de debt técnico aceito)

**Restrições:**
- [guardrail 1 extraído da revisão]
- [guardrail 2]

**O que NÃO fazer:**
- [padrão negativo identificado]
```

**3.3 — Verificar completude do canvas:**
Após atualizar S, verificar se todas as 7 dimensões estão preenchidas:
- Se R, E, A, S (Structure), O, N também preenchidas → atualizar `_Status: READY_`
- Informar ao usuário: "Canvas transitou para READY — pronto para implementação paralela"

**3.4 — Criar ADR se necessário:**
Se durante a revisão foi aceita conscientemente uma dívida técnica ou refatoração foi adiada: criar ADR documentando a decisão.

### Fase 4 — Feedback ao desenvolvedor

Apresentar resumo estruturado:
- Nº de findings por severidade
- Itens que BLOQUEIAM o merge (CRÍTICOS não resolvidos)
- Itens que devem ser resolvidos antes do merge (IMPORTANTES)
- Sugestões para iterações futuras

Se REPROVADO: listar exatamente o que corrigir antes de re-review.
Se APROVADO ou APROVADO COM RESSALVAS: sugerir próximos passos.

Executar validação do relatório:
```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/code-review/validate-rules.json \
  --artifact docs/checklists/[feature]-[task]-review.md
```

## Artefatos

**Entrada:**
- Código implementado (diff ou arquivos)
- `docs/tasks/[feature]-tasks.md`
- `docs/techspec/[feature]-techspec.md`
- `docs/spdd/[feature]-canvas.md`
- `systems/[sistema]/guidelines/security.md`, `coding-standards.md`

**Saída:**
- `docs/checklists/[feature]-[task-id]-review.md` — relatório de review
- `docs/spdd/[feature]-canvas.md` — dimensão S atualizada; pode transitar para READY
- `docs/decisions/ADR-[NNN]-*.md` — se debt técnico aceito conscientemente

## Canvas

Esta skill atualiza a dimensão **S — Safeguards**:

- Guardrails extraídos da revisão de segurança e qualidade
- Padrões negativos ("o que NÃO fazer") identificados durante o review
- Referências a SDRs/ADRs criadas nesta fase: `> Decisões: SDR-001, ...` (ou `> Decisões: —` se nenhuma)
- Ownership: `_Atualizado por: /code-review v1.0 — [data]_`

**Transição para READY:** /code-review é tipicamente a última skill a preencher o canvas (S é a última dimensão). Quando S é preenchida e todas as outras 6 dimensões estão preenchidas, o canvas transita para `READY`.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Code review:** TASK-[ID] — [APROVADO|REPROVADO] — [data]
- **Findings:** [N] críticos, [M] importantes, [K] sugestões
- **Canvas:** [status após review]
- **Próximo passo:** [corrigir findings | próxima task | /spdd-sync]
```

Artifact Registry:
```
| docs/checklists/[feature]-[task]-review.md | 1.0 | ok |
```


## /designer


## Objetivo

Levantar, através de entrevista estruturada, os tokens visuais, componentes e padrões de interação necessários para a feature, produzindo um Design Brief pronto para prototipagem. Cada decisão de design system relevante gera um Decision Record do tipo DDR.

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir com status `ok` no Artifact Registry
- `memory/state.md` com a feature registrada

## Workflow

### Fase 0 — Leitura de contexto

1. `docs/prd/[feature]-prd.md` — objetivo de negócio, público-alvo, jornadas descritas
2. `memory/state.md` — confirmar versão do PRD no Artifact Registry
3. `docs/spdd/[feature]-canvas.md`, se existir — ler dimensão E já preenchida por `/prd`/`/techspec` para não duplicar entidades de domínio

### Fase 1 — Levantamento de tokens visuais (uma pergunta por vez)

Perguntar, **uma de cada vez**, absorvendo o que já estiver decidido em guidelines/PRD:

1. Paleta de cores (primária, secundária, fundo, superfície, erro, sucesso, texto) — ou "usar design system existente: [nome]"
2. Tipografia (fonte heading, fonte body, fonte mono, escala de tamanhos)
3. Grid e breakpoints (mobile/tablet/desktop)
4. Escala de espaçamento (base 4px ou 8px)

Salvar respostas incrementalmente no Design Brief à medida que forem obtidas.

### Fase 2 — Componentes

Para cada tela/fluxo relevante do PRD:
1. Identificar componentes necessários (ex: botão, card, modal, formulário)
2. Para cada componente: variantes, estados (default/hover/active/disabled/loading/error)
3. Salvar seção de Componentes do Design Brief

### Fase 3 — Padrões de interação

1. Levantar padrões de feedback (sucesso, erro, loading) e transições/animações
2. Confirmar requisitos de acessibilidade (contraste WCAG AA, foco visível, leitores de tela, tamanho mínimo de toque)
3. Salvar seções de Padrões de Interação e Acessibilidade

### Fase 4 — Decision Records de Design

Para cada decisão de design system relevante tomada nas Fases 1-3 (ex: escolha de paleta, escolha de grid, padrão de componente não trivial):
1. Verificar próximo número de sequência DDR no índice de `memory/constitution.md`
2. Criar `docs/decisions/ddr-[NNN]-[slug].md` a partir do template de Decision Record
3. Adicionar ao índice de DDRs em `memory/constitution.md`
4. Referenciar o DDR na seção 8 do Design Brief

### Fase 5 — Atualização do Canvas (dimensão E)

Atualizar dimensão **E — Entities** do canvas `docs/spdd/[feature]-canvas.md`, complementando (não substituindo) as entidades de domínio já registradas por `/prd`/`/techspec` com as entidades de UX/UI:
- Componentes principais e seus tokens
- `_Atualizado por: /designer v1.0 — [data]_`
- `> Decisões: DDR-[NNN], ...`

Salvar o canvas após a atualização.

### Fase 6 — Validação e Handoff

1. Validar o Design Brief:
   ```
   python .agents/scripts/validate.py --mode output \
     --rules .agents/skills/designer/validate-rules.json \
     --artifact docs/design/[feature]-design-brief.md
   ```
2. Atualizar `memory/state.md`:
   - Artifact Registry: `docs/design/[feature]-design-brief.md | 1.0 | ok`
3. Sugerir próximo passo: prototipagem (fora do pipeline SDD) ou `/techspec` se ainda não executado

## Artefatos

**Entrada:**
- `docs/prd/[feature]-prd.md` (obrigatório)
- `memory/state.md` (obrigatório)
- `docs/spdd/[feature]-canvas.md` (opcional — se já existir)

**Saída:**
- `docs/design/[feature]-design-brief.md`
- `docs/decisions/ddr-[NNN]-[slug].md` (um por decisão de design system)

## Canvas

Esta skill atualiza:
- **E — Entities:** entidades de UX/UI (componentes, tokens), complementando as entidades de domínio
- Referências a DDRs criadas nesta fase: `> Decisões: DDR-001, ...` (ou `> Decisões: —` se nenhuma)

## Handoff

Ao concluir, registrar em `memory/state.md` (seção da feature ativa):

```markdown
- **Etapa concluída:** /designer (v1.0) — [data]
- **Artefato:** docs/design/[feature]-design-brief.md
- **DDRs criados:** DDR-[NNN], ...
```


## /discovery


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


## /guidelines


## Objetivo

Gerar os 9 arquivos de guidelines para um sistema, capturando decisões de stack, arquitetura e padrões de engenharia através de entrevista interativa. Cada decisão técnica significativa gera um ADR (Architecture Decision Record). Os guidelines são a fonte de verdade para todas as skills subsequentes do pipeline.

## Pré-condições

- `memory/state.md` deve existir com o sistema registrado na tabela de Sistemas
- `systems/[sistema]/` deve existir (ou será criado)
- Se guidelines já existem: perguntar se deseja atualizar (bump de versão) ou substituir

## Workflow

### Fase 0 — Verificação

1. Identificar o sistema-alvo: ler `--system` do argumento ou perguntar
2. Verificar se `systems/[sistema]/guidelines/` já existe
   - Se sim: listar os arquivos existentes e perguntar "Atualizar guidelines existentes?"
3. Criar diretório `systems/[sistema]/guidelines/` se não existir

### Fase 1 — Entrevista de stack (uma pergunta por vez)

**Módulo A — Linguagem e runtime:**
- "Qual linguagem principal? (ex: Python, TypeScript, Go, Rust)"
- "Qual versão mínima? Há requisito de compatibilidade?"
- "Há um runtime específico? (ex: Node 20, Python 3.10+, JVM 21)"

**Módulo B — Frameworks e bibliotecas:**
- "Qual framework web/API principal? (ex: FastAPI, Express, Gin, Rails)"
- "Qual ORM ou camada de dados? (ex: SQLAlchemy, Prisma, GORM)"
- "Há bibliotecas obrigatórias pelo padrão da empresa?"

**Módulo C — Infraestrutura:**
- "Qual banco de dados principal? Qual versão?"
- "Há cache? (ex: Redis, Memcached)"
- "Qual plataforma de deploy? (ex: AWS, GCP, Docker, Kubernetes)"

**Módulo D — Padrões de código:**
- "Qual linter/formatter? (ex: ruff, ESLint, golangci-lint)"
- "Há convenções de nomenclatura específicas? (ex: snake_case, camelCase)"
- "Qual é o tamanho máximo de função aceito? (ex: 50 linhas)"

**Módulo E — Testes:**
- "Qual framework de testes? (ex: pytest, Jest, Go test)"
- "Qual cobertura mínima exigida?"
- "TDD é obrigatório ou recomendado?"

**Módulo F — Segurança:**
- "Há padrões de autenticação/autorização definidos? (ex: JWT, OAuth2, RBAC)"
- "Há requisitos de compliance? (ex: LGPD, SOC2, OWASP Top 10)"

**Módulo G — Observabilidade:**
- "Qual stack de logs? (ex: structured JSON, ELK, Datadog)"
- "Há APM ou tracing? (ex: OpenTelemetry, Jaeger)"

**Módulo H — Git e CI/CD:**
- "Qual estratégia de branching? (ex: trunk-based, GitFlow)"
- "Há pipeline de CI/CD? Qual ferramenta?"
- "Quais checks são obrigatórios antes do merge?"

### Fase 2 — Criação de ADRs

Para cada decisão técnica significativa tomada durante a entrevista (escolha de stack, framework, padrão arquitetural), criar um ADR:

1. Determinar próximo número ADR no índice de `memory/constitution.md`
2. Criar `docs/decisions/ADR-[NNN]-[slug].md` usando template de decision-record
3. Preencher: Decisão, Motivação, Consequências, Alternativas Consideradas
4. Adicionar ao índice em `memory/constitution.md` seção `### ADR`

**Quando criar ADR:** decisão que envolve trade-off relevante, escolha entre alternativas comparáveis, ou que impactará o desenvolvimento por meses.

### Fase 3 — Geração progressiva dos guidelines

**Salvar cada arquivo imediatamente após gerar — não aguardar concluir todos.**

Gerar em ordem, salvando após cada um:

1. `systems/[sistema]/guidelines/stack.md` — linguagens, versões, dependências principais
2. `systems/[sistema]/guidelines/architecture.md` — padrões arquiteturais, camadas, responsabilidades
3. `systems/[sistema]/guidelines/coding-standards.md` — convenções de código, linting, formatação
4. `systems/[sistema]/guidelines/testing.md` — frameworks, cobertura, TDD, tipos de teste
5. `systems/[sistema]/guidelines/security.md` — autenticação, autorização, compliance, OWASP
6. `systems/[sistema]/guidelines/observability.md` — logs, métricas, tracing, alertas
7. `systems/[sistema]/guidelines/git-workflow.md` — branching, commits, PR process, CI/CD
8. `systems/[sistema]/guidelines/skill-conventions.md` — padrões de uso das skills do pipeline
9. `systems/[sistema]/guidelines/spdd-integration.md` — como SPDD/canvas se integra ao workflow do time

Executar validação ao final:
```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/guidelines/validate-rules.json \
  --artifact systems/[sistema]/guidelines/stack.md \
  --system [sistema]
```

### Fase 4 — Handoff

Atualizar `memory/state.md`:
- Tabela de Sistemas: marcar Guidelines como `ok` para o sistema
- Artifact Registry: adicionar cada arquivo de guidelines com status `ok`

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)

**Saída:**
- `systems/{{SYSTEM}}/guidelines/` — 9 arquivos de guidelines
- `docs/decisions/ADR-[NNN]-*.md` — um ou mais ADRs de decisões de stack

**Validação:** `python .agents/scripts/validate.py --mode output --rules .agents/skills/guidelines/validate-rules.json --artifact [guideline] --system [sistema]`

## Canvas

Esta skill contribui com a dimensão **N** do REASONS Canvas durante o /techspec (não diretamente — /techspec lê os guidelines e extrai as normas relevantes para a feature):

**N — Norms:**
- Não é atualizada pelo /guidelines diretamente
- Os guidelines são a *fonte* da qual /techspec extrai N para o canvas
- Cada skill de implementação lê N do canvas como contexto de padrões

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
## Sistemas

| Sistema | Caminho | Cenário | Guidelines | Observações |
|---|---|---|---|---|
| [SISTEMA] | `systems/[sistema]/` | [cenário] | **ok** | [data] |
```

Artifact Registry — adicionar uma linha por arquivo:
```
| systems/[sistema]/guidelines/stack.md | 1.0 | ok |
| systems/[sistema]/guidelines/architecture.md | 1.0 | ok |
| ... (demais 7 arquivos) ...
```


## /implement


## Objetivo

Implementar uma task específica do documento de tasks com fidelidade à TechSpec e ao REASONS Canvas. O canvas fornece contexto crítico de Norms (padrões a seguir) e Safeguards (restrições a respeitar) antes de qualquer linha de código. Cada task implementada é rastreável aos RFs de origem.

## Pré-condições

- `docs/tasks/[feature]-tasks.md` deve existir com a task solicitada
- `docs/techspec/[feature]-techspec.md` deve existir com status `ok`
- Canvas `docs/spdd/[feature]-canvas.md` deve existir
  - Se status `READY`: prosseguir normalmente
  - Se status `DRAFT`: **alertar o usuário**:
    > "⚠️ Canvas em DRAFT — dimensão O (Operations) ainda não preenchida. Recomendo executar `/tasks [feature]` antes de implementar. Deseja continuar assim mesmo?"
  - Se canvas não existe: alertar e sugerir `/spdd-canvas [feature]`

## Workflow

### Fase 0 — Leitura de contexto (obrigatória)

**Ler nesta ordem, sem pular:**

1. `docs/tasks/[feature]-tasks.md` — localizar a task solicitada pelo ID (ex: TASK-01.1)
2. `docs/spdd/[feature]-canvas.md` — extrair:
   - **N — Norms:** padrões e convenções obrigatórios para esta feature
   - **S — Safeguards:** restrições, o que NÃO fazer, guardrails de segurança
3. `docs/techspec/[feature]-techspec.md` — seções relevantes para a task
4. `systems/[sistema]/guidelines/[arquivo].md` — guidelines específicos referenciados na task

**Confirmar internamente antes de codificar:**
- Qual é o critério de aceite desta task?
- Quais normas de N se aplicam ao código que vou escrever?
- Quais restrições de S devo respeitar?

### Fase 1 — Decisão TDD (por tipo de task)

Avaliar automaticamente se TDD é aplicável:

| Tipo de task | TDD aplicável? |
|---|---|
| Lógica de negócio, validações, parsers | **Sim — usar TDD** |
| Scripts de CLI, utilitários | **Sim — usar TDD** |
| Configuração, templates, YAML/JSON | Não — criar e verificar manualmente |
| Documentação, SKILL.md, templates Markdown | Não |
| Migração de banco de dados | Não (testar integração separada) |

Se TDD aplicável: seguir ciclo Red → Green → Refactor antes de implementar.
Se TDD não aplicável: implementar diretamente com verificação manual.

### Fase 2 — Implementação

2.1. **Verificar se arquivo-alvo já existe:**
   - Se sim: ler conteúdo antes de modificar (nunca sobrescrever cegamente)
   - Se não: criar novo seguindo os padrões de N

2.2. **Implementar seguindo os critérios de aceite da task:**
   - Cada item do checklist "O que deve ser feito" deve ser implementado
   - Respeitar todas as restrições de S (Safeguards)
   - Seguir convenções de N (nomenclatura, estrutura, padrões)
   - Usar os caminhos de arquivo exatos do guia técnico da task

2.3. **Se TDD:** escrever testes antes de cada funcionalidade
   - Red: escrever teste que falha
   - Green: escrever código mínimo para passar
   - Refactor: limpar sem quebrar

2.4. **Rastreabilidade:** ao implementar, mapear mentalmente qual RF de origem cada trecho de código atende

### Fase 3 — Verificação dos critérios de aceite

Após implementar, verificar **cada critério de aceite** da task:
- Executar testes se houver
- Verificar comportamento esperado descrito nos critérios
- Confirmar que nenhuma restrição de S foi violada
- Confirmar que as normas de N foram respeitadas

Se algum critério não for atendido: corrigir antes de reportar conclusão.

### Fase 4 — Sugestão de validação e próximos passos

Após implementação concluída, informar ao usuário:

1. Arquivos criados/modificados (lista concisa)
2. Critérios de aceite verificados
3. Sugerir execução de validate.py se aplicável:
   ```
   python .agents/scripts/validate.py --mode output \
     --rules .agents/skills/[skill]/validate-rules.json \
     --artifact [artefato-gerado]
   ```
4. Sugerir próximos passos: `/code-review` ou próxima task

Atualizar `memory/state.md` se task for a última do Epic:
- Marcar Epic como concluído no status da feature

## Artefatos

**Entrada:**
- `docs/tasks/[feature]-tasks.md` (obrigatório)
- `docs/spdd/[feature]-canvas.md` (obrigatório — lê N e S)
- `docs/techspec/[feature]-techspec.md` (obrigatório)
- `systems/[sistema]/guidelines/*.md` (lidos conforme necessário)

**Saída:**
- Código implementado (fora do workspace de artefatos SDD)
- Testes (se TDD aplicável)

## Canvas

Esta skill **não atualiza** o canvas diretamente.

Lê as dimensões:
- **N — Norms:** padrões obrigatórios lidos ANTES de escrever qualquer código
- **S — Safeguards:** restrições e guardrails lidos ANTES de escrever qualquer código

A leitura de N e S é obrigatória — pular esta etapa viola o princípio Canvas-as-prompt do SSPDD.

## Handoff

Ao concluir cada task, registrar progresso em `memory/state.md` (seção da feature ativa):

```markdown
- **Task implementada:** TASK-[EPIC].[SEQ] — [descrição breve] — [data]
- **Arquivos:** [lista dos arquivos criados/modificados]
- **Testes:** [passando | não aplicável]
- **Próxima task:** TASK-[EPIC].[SEQ+1] ou /code-review
```


## /prd


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


## /spdd-canvas


## Objetivo

Gerar (ou completar) o REASONS Canvas de uma feature a partir dos artefatos já existentes — PRD, TechSpec e, se disponível, Tasks. É o caminho de recuperação para features que não passaram por `/discovery` no início do pipeline, e também a skill usada para editar manualmente uma dimensão do canvas fora do fluxo automático de push de cada skill.

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir com status `ok`
- `docs/techspec/[feature]-techspec.md` deve existir com status `ok`
- `docs/tasks/[feature]-tasks.md` — opcional; se ausente, dimensão O fica vazia e o canvas permanece `DRAFT`
- Se `docs/spdd/[feature]-canvas.md` já existir: ler antes de sobrescrever, preservar dimensões já preenchidas por outras skills (nunca reverter ownership de uma dimensão mais recente)

## Workflow

### Fase 0 — Leitura de contexto

1. `memory/state.md` — confirmar status `ok` de PRD e TechSpec no Artifact Registry
2. `docs/prd/[feature]-prd.md` — objetivos de negócio, escopo, RFs, entidades de domínio
3. `docs/techspec/[feature]-techspec.md` — abordagem de solução, trade-offs, arquitetura, dependências
4. `docs/tasks/[feature]-tasks.md` se existir — lista de tasks ordenada
5. Se `docs/spdd/[feature]-canvas.md` já existir: ler para preservar dimensões preenchidas e evitar retrabalho

### Fase 1 — Geração progressiva, dimensão por dimensão

**Salvar o arquivo após cada dimensão preenchida — nunca aguardar o canvas completo.**

Preencher, nesta ordem, cada uma com heading `## [LETRA] — [Nome]`, linha de ownership `_Atualizado por: /spdd-canvas v1.0 — [data]_` e linha `> Decisões: [DRs relevantes ou —]`:

1. **R — Requirements:** objetivos de negócio e escopo IN/OUT, extraídos do PRD
2. **E — Entities:** entidades de domínio e diagrama, extraídos do PRD/data model da TechSpec
3. **A — Approach:** estratégia de solução e trade-offs, extraídos da TechSpec
4. **S — Structure:** arquitetura e dependências, extraídos da TechSpec
5. **O — Operations:** lista de tasks ordenada por dependência, extraída de Tasks (se ausente, deixar vazia e manter `DRAFT`)
6. **N — Norms:** padrões relevantes da feature, extraídos de `systems/[sistema]/guidelines/`

**Regra crítica:** nunca publicar o canvas com a dimensão **O** vazia como se estivesse `READY` — se Tasks não existir ainda, o canvas permanece `DRAFT` mesmo com as outras 6 dimensões preenchidas.

A dimensão **S — Safeguards** não é preenchida por esta skill — é ownership exclusivo de `/code-review`.

### Fase 2 — Verificação de completude

Ao final da geração:
- Se todas as 7 dimensões (R, E, A, S, O, N, S-safeguards) estiverem preenchidas e sem placeholder vazio → status `READY`
- Se qualquer uma estiver vazia (mais comumente O ou S-safeguards, ainda não geradas) → manter `DRAFT`

### Fase 3 — Validação

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/spdd-canvas/validate-rules.json \
  --artifact docs/spdd/[feature]-canvas.md
```

Isso executa também os `custom_steps`: `check_canvas_ownership.py` (toda dimensão tem linha de ownership) e `check_canvas_decisions.py` (toda dimensão tem linha `> Decisões:`).

### Fase 4 — Handoff

Atualizar `memory/state.md`:
- Artifact Registry: `spdd/[feature]-canvas.md | — | [ok se READY | draft se DRAFT]`
- Se canvas ficou `READY`: informar ao usuário que `/implement` já pode ser usado com contexto completo

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)
- `docs/prd/[feature]-prd.md` (obrigatório — `ok`)
- `docs/techspec/[feature]-techspec.md` (obrigatório — `ok`)
- `docs/tasks/[feature]-tasks.md` (opcional)

**Saída:**
- `docs/spdd/[feature]-canvas.md` — canvas completo ou parcialmente preenchido

## Canvas

Esta skill atualiza as dimensões **R, E, A, S (Structure), O, N** do REASONS Canvas — todas exceto **S (Safeguards)**, que é ownership de `/code-review`.

Cada dimensão preenchida recebe ownership `_Atualizado por: /spdd-canvas v1.0 — [data]_`, mesmo quando o conteúdo é derivado de outro artefato (ex: R vem do PRD) — porque é esta skill, e não `/prd` diretamente, quem escreveu a dimensão no canvas nesta execução.

**Transição DRAFT → READY:** só ocorre quando as 7 dimensões estiverem preenchidas, incluindo O (requer Tasks já gerada).

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Canvas gerado/atualizado:** docs/spdd/[feature]-canvas.md — [data]
- **Dimensões preenchidas:** [lista, ex: R, E, A, S, N — O pendente]
- **Status do canvas:** [DRAFT | READY]
- **Próximo comando:** [/tasks (se O pendente) | /implement (se READY)]
```

Artifact Registry:
```
| spdd/[feature]-canvas.md | — | [ok | draft] |
```


## /spdd-sync


## Objetivo

Comparar o REASONS Canvas com o código efetivamente implementado (via diff) e detectar divergências por dimensão. Para cada divergência encontrada, apresentar ao usuário e resolver na direção escolhida — corrigir o canvas para refletir a realidade do código, ou reverter o código para seguir o canvas — respeitando o princípio "fix the prompt first" do OpenSPDD, mas sem impedir evolução legítima de design durante a implementação. Todo desvio, resolvido ou não, é registrado em `docs/spdd/[feature]-deviations.md`.

## Pré-condições

- `docs/spdd/[feature]-canvas.md` deve existir
- Diff de código disponível (arquivos criados/modificados desde o último `/implement` ou `/code-review`)
- Se o canvas estiver em `DRAFT`: alertar que a comparação pode ser parcial (dimensões ainda vazias não têm o que divergir)

## Workflow

### Fase 0 — Leitura de contexto

1. `docs/spdd/[feature]-canvas.md` — ler todas as dimensões preenchidas
2. Diff de código da feature — arquivos criados/modificados desde a última sincronização (ou desde o início da implementação, se for a primeira execução)
3. `docs/spdd/[feature]-deviations.md` se já existir — para não duplicar DEVs já registrados

### Fase 1 — Detecção de divergências por dimensão

Comparar código com canvas, dimensão por dimensão:

| Dimensão | O que verificar no diff |
|---|---|
| **E — Entities** | Nova entidade/campo no código sem correspondência no canvas |
| **A — Approach** | Estratégia de solução implementada diverge da abordagem descrita |
| **S — Structure** | Nova dependência, componente ou padrão de arquitetura não previsto |
| **O — Operations** | Task implementada de forma diferente do descrito na lista de operations |
| **N — Norms** | Convenção do código foge do padrão declarado |
| **S — Safeguards** | Restrição declarada foi violada no código implementado |

Não inferir divergência de dimensões vazias (`DRAFT` sem conteúdo naquela dimensão) — não há o que comparar.

### Fase 2 — Apresentação e decisão (uma divergência por vez)

**Regra crítica:** apresentar cada divergência individualmente, nunca em bloco. Para cada uma:

1. Mostrar: dimensão afetada, o que o canvas diz, o que o código faz
2. Perguntar ao usuário a direção de resolução:
   - **Canvas corrigido** — código está certo, canvas desatualizado (evolução legítima de design)
   - **Código revertido** — canvas está certo, código se desviou por engano
   - **Aceito com justificativa** — divergência é intencional e temporária, registrar sem alterar nada agora
3. Aguardar decisão antes de seguir para a próxima divergência

### Fase 3 — Aplicação da resolução

- **Canvas corrigido:** atualizar a dimensão afetada no canvas, mantendo ownership da skill que originalmente a preencheu (não atribuir a `/spdd-sync`) e acrescentando nota de revisão
- **Código revertido:** orientar o usuário sobre o que precisa ser ajustado no código (esta skill não edita código de produção diretamente — apenas orienta e registra)
- **Aceito com justificativa:** nenhuma alteração em canvas ou código; apenas registro

### Fase 4 — Registro em deviations.md

Para cada divergência processada, adicionar entrada em `docs/spdd/[feature]-deviations.md` seguindo o schema:

```markdown
## DEV-[NNN] — [data]
- **Dimensão afetada:** [R | E | A | S | O | N | S-safeguards]
- **Descrição:** [o que diverge]
- **Direção de resolução:** canvas corrigido | código revertido | aceito com justificativa
- **Justificativa:** [motivo]
- **Status:** resolved | pending | accepted
```

Numeração sequencial `DEV-NNN` contínua no arquivo. Atualizar a tabela `## Sumário` ao final com todas as entradas.

**Salvar o arquivo após cada DEV registrado — não aguardar processar todas as divergências.**

### Fase 5 — Validação e handoff

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/spdd-sync/validate-rules.json \
  --artifact docs/spdd/[feature]-deviations.md
```

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)
- `docs/spdd/[feature]-canvas.md` (obrigatório)
- Diff de código da feature

**Saída:**
- `docs/spdd/[feature]-deviations.md` — registro de todos os desvios detectados
- `docs/spdd/[feature]-canvas.md` — atualizado, apenas nas dimensões cuja resolução foi "canvas corrigido"

## Canvas

Esta skill **não tem ownership de dimensão própria** — quando corrige uma dimensão, preserva o ownership original daquela dimensão (skill que a preencheu primeiro) e apenas acrescenta uma nota de revisão referenciando o DEV correspondente. Lê todas as dimensões preenchidas para comparação, mas não assina nenhuma como `_Atualizado por: /spdd-sync_`.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Sincronização executada:** /spdd-sync — [data]
- **Divergências encontradas:** [N]
- **Resolvidas:** [N-resolved] | **Pendentes:** [N-pending] | **Aceitas:** [N-accepted]
- **Artefato:** docs/spdd/[feature]-deviations.md
```

Artifact Registry:
```
| spdd/[feature]-deviations.md | 1.0 | ok |
```


## /tasks


## Objetivo

Decompor a TechSpec em tasks de implementação executáveis, agrupadas em Epics e User Stories, com dependências explícitas e oportunidades de paralelismo identificadas. Cada task é auto-contida: tem contexto, critérios de aceite e guia técnico suficientes para ser implementada sem consultar outros documentos.

## Pré-condições

- `docs/techspec/[feature]-techspec.md` deve existir com status `ok` no Artifact Registry
- `docs/prd/[feature]-prd.md` deve existir com status `ok`
- Verificar stale antes de prosseguir:
  ```
  python .agents/scripts/validate.py --mode input \
    --rules .agents/skills/tasks/validate-rules.json \
    --artifact docs/techspec/[feature]-techspec.md
  ```
  Se stale: alertar e aguardar confirmação

## Workflow

### Fase 0 — Leitura e análise

1. Ler `docs/prd/[feature]-prd.md` — extrair todos os RFs com prioridades
2. Ler `docs/techspec/[feature]-techspec.md` — extrair: decisões arquiteturais, data model, contratos, matriz de rastreabilidade
3. Ler `docs/techspec/[feature]/data-model.md` se existir
4. Mapear internamente: RF → componentes técnicos → tasks candidatas

### Fase 1 — Decisões de planejamento (com o usuário)

Fazer perguntas **uma de cada vez**:

- "Há alguma restrição de ordem de implementação que não está explícita na TechSpec? (ex: dependência de outro time, prazo de infra)"
- "Prefere granularidade maior (tasks menores, mais paralelismo) ou menor (tasks maiores, menos overhead)?"
- "Há tasks que devem obrigatoriamente ser feitas por uma pessoa específica ou em uma sprint específica?"

Para decisões de priorização ou escopo tomadas aqui: criar BDR (Business Decision Record).

### Fase 2 — Geração progressiva do documento de tasks

**Salvar a cada Epic concluído — não aguardar o documento completo.**

2.1. Criar estrutura de Epics:
   - Agrupar por área funcional ou camada técnica (ex: Infra Base, Engine, UI, Testes)
   - Cada Epic contém User Stories que contêm Tasks

2.2. Para cada Task, preencher obrigatoriamente:
   - **ID:** TASK-[EPIC].[SEQ] (ex: TASK-01.1)
   - **Título** com tamanho estimado: [P] ≤4h | [M] 4-8h | [G] 1-2 dias
   - **Sistema** e **RF de origem**
   - **Dependências** explícitas (outras TASKs)
   - **[P] com TASK-X.Y** se pode ser executada em paralelo
   - **Contexto:** por que esta task existe, o que ela resolve
   - **O que deve ser feito:** checklist de ações concretas
   - **Guia técnico:** arquivo a criar/modificar, padrão a seguir
   - **Critérios de aceite:** mensuráveis e verificáveis

2.3. Salvar após cada Epic completo

2.4. Gerar tabela de Sumário de Epics e Grafo de Dependências no início

2.5. Gerar seção "Backlog Priorizado" ao final com ordem de início recomendada

2.6. Validação ao concluir:
```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/tasks/validate-rules.json \
  --artifact docs/tasks/[feature]-tasks.md
```

### Fase 3 — Atualização do Canvas (dimensão O)

Atualizar dimensão **O — Operations** no canvas `docs/spdd/[feature]-canvas.md`:

```markdown
## O — Operations

_Atualizado por: /tasks v1.0 — [data]_
> Decisões: BDR-[NNN] (se houver decisões de priorização)

**Tasks ordenadas por dependência:**
- [ ] TASK-01.1 — [descrição breve]
- [ ] TASK-01.2 — [descrição breve]
...
```

Após salvar O: verificar se todas as 7 dimensões estão preenchidas.
- Se R, E, A, S, N, S-safeguards já preenchidas + O agora preenchida → atualizar status para `READY`
- Se alguma outra dimensão ainda vazia → manter `DRAFT`

**Regra crítica:** canvas só transita para `READY` quando O é preenchida e todas as outras 6 também estão preenchidas. Nunca publicar canvas com O vazia.

### Fase 4 — Handoff

Atualizar `memory/state.md`:
- Artifact Registry: `docs/tasks/[feature]-tasks.md | 1.0 | ok`
- Se canvas transitou para READY: atualizar `docs/spdd/[feature]-canvas.md | — | ok`
- Feature status: "Pronto para implementação"

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)
- `docs/prd/[feature]-prd.md` (obrigatório — `ok`)
- `docs/techspec/[feature]-techspec.md` (obrigatório — `ok`)

**Saída:**
- `docs/tasks/[feature]-tasks.md` — plano completo de tasks
- `docs/spdd/[feature]-canvas.md` — dimensão O atualizada; pode transitar para READY
- `docs/decisions/BDR-[NNN]-*.md` — BDRs de priorização (se houver)

## Canvas

Esta skill atualiza a dimensão **O** do REASONS Canvas:

**O — Operations:**
- Lista de tasks ordenada por dependência com IDs e descrições breves
- Referências a DRs criadas nesta fase: `> Decisões: ADR-003, ...` (ou `> Decisões: —` se nenhuma)
- Ownership: `_Atualizado por: /tasks v1.0 — [data]_`
- Esta é a dimensão que pode fazer o canvas transitar de DRAFT → READY

**Transição DRAFT → READY:** ocorre quando O é preenchida e todas as outras 6 dimensões já estão preenchidas.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
### [FEATURE_NAME]
- **Etapa concluída:** /tasks (v1.0) — [data]
- **Artefato:** docs/tasks/[feature]-tasks.md
- **Total de tasks:** [N] tasks em [M] epics
- **Canvas:** [DRAFT | READY]
- **Próximo comando:** /implement TASK-[EPIC].[SEQ]
```

Artifact Registry:
```
| docs/tasks/[feature]-tasks.md | 1.0 | ok |
```


## /tdd


## Objetivo

Implementar uma task seguindo o ciclo TDD rigoroso: escrever testes que falham antes de qualquer código de produção, implementar o mínimo para passá-los e refatorar sem quebrar. Inclui review integrado ao final. A diferença do /implement: aqui os testes são escritos *antes* do código, não depois.

## Pré-condições

- `docs/tasks/[feature]-tasks.md` com a task solicitada
- `docs/techspec/[feature]-techspec.md` com status `ok`
- `docs/spdd/[feature]-canvas.md` — se DRAFT, alertar mas permitir continuar
- Framework de testes configurado no projeto (ver `systems/[sistema]/guidelines/testing.md`)

## Workflow

### Fase 0 — Leitura de contexto (obrigatória)

Ler nesta ordem antes de qualquer código:

1. Task alvo: ID, critérios de aceite, guia técnico
2. `docs/spdd/[feature]-canvas.md`:
   - **N — Norms:** padrões obrigatórios (nomenclatura, estrutura, convenções)
   - **S — Safeguards:** restrições, o que NÃO implementar
3. `docs/techspec/[feature]-techspec.md` — seções relevantes para a task
4. `systems/[sistema]/guidelines/testing.md` — framework e convenções de teste

### Fase 1 — RED: Escrever testes que falham

**Regra de ouro:** nenhuma linha de código de produção antes de ter pelo menos um teste falhando.

1.1. Mapear cada critério de aceite da task em um ou mais casos de teste:
   - Critério Gherkin → teste de comportamento
   - Edge cases identificados → testes de borda
   - Casos de erro/exceção → testes negativos

1.2. Para cada caso de teste:
   - Escrever nome descritivo: `test_[comportamento]_when_[condição]_should_[resultado]`
   - Escrever arrange/act/assert (ou given/when/then)
   - **Confirmar que o teste FALHA** antes de prosseguir (se não falha, o teste é inútil)

1.3. Salvar arquivo(s) de teste

**Output desta fase:** suite de testes falhando, cobrindo todos os critérios de aceite.

### Fase 2 — GREEN: Implementar o mínimo

**Regra:** escrever o código mínimo necessário para fazer os testes passarem. Sem otimizações prematuras, sem features extras.

2.1. Implementar funcionalidade respeitando N (Norms) e S (Safeguards) do canvas
2.2. Executar testes após cada implementação parcial
2.3. Continuar até todos os testes passarem
2.4. **Não refatorar ainda** — apenas fazer os testes passarem

**Output desta fase:** todos os testes verdes, código funcionando (mas possivelmente não limpo).

### Fase 3 — REFACTOR: Limpar sem quebrar

**Regra:** melhorar estrutura e legibilidade sem alterar comportamento. Testes devem permanecer verdes ao final.

3.1. Identificar: código duplicado, nomes ruins, funções longas, abstrações desnecessárias
3.2. Aplicar uma refatoração por vez, executando testes após cada mudança
3.3. Verificar conformidade com normas de N (nomenclatura, tamanho de função, etc.)
3.4. Verificar que nenhuma restrição de S foi introduzida inadvertidamente

**Output desta fase:** código limpo, testes verdes, normas respeitadas.

### Fase 4 — REVIEW integrado

Review rápido focado nos pontos mais críticos (review completo via /code-review):

4.1. **Segurança:** verificar os 3 mais prováveis para este tipo de código:
   - Input validation nos pontos de entrada
   - Sem secrets hardcoded
   - Sem vulnerabilidades óbvias (injection, path traversal)

4.2. **Cobertura:** os critérios de aceite da task estão 100% cobertos por testes?

4.3. **Conformidade:** implementação está alinhada com TechSpec (abordagem, data model, contratos)?

4.4. Se encontrado algo crítico: corrigir antes de reportar conclusão.

### Fase 5 — Conclusão e próximos passos

Reportar ao usuário:
- Arquivos criados/modificados
- Nº de testes escritos e resultado (todos passando)
- Cobertura dos critérios de aceite
- Sugestão: `/spdd-sync` para verificar desvios do canvas, ou `/code-review` para review completo

Atualizar `memory/state.md` com progresso da task.

## Artefatos

**Entrada:**
- `docs/tasks/[feature]-tasks.md`
- `docs/spdd/[feature]-canvas.md` (lê N e S)
- `docs/techspec/[feature]-techspec.md`
- `systems/[sistema]/guidelines/testing.md`

**Saída:**
- Código de produção implementado
- Suite de testes (escrita antes do código)

## Canvas

Esta skill **não atualiza** o canvas.

Lê obrigatoriamente:
- **N — Norms:** padrões aplicados durante Red, Green e Refactor
- **S — Safeguards:** verificados durante o Refactor e Review

A leitura de N e S é idêntica ao /implement — o canvas guia tanto implementação direta quanto TDD.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Task TDD:** TASK-[EPIC].[SEQ] — [descrição] — [data]
- **Testes:** [N] testes escritos, todos passando
- **Ciclo:** Red → Green → Refactor → Review concluídos
- **Próximo passo:** /code-review ou próxima TASK
```


## /techspec


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


## /tests


## Objetivo

Gerar e executar a suíte de testes de uma task com base nos critérios de aceite e nos blocos Gherkin dos RFs de origem, seguindo a estratégia de testes da TechSpec e as convenções de `testing.md` do sistema. Suporta dois modos: **TDD** (testes gerados antes do código, para orientar a implementação) e **audit** (testes gerados após o código já existir, para fechar cobertura).

## Pré-condições

- `docs/tasks/[feature]-tasks.md` deve existir com a task solicitada
- `docs/techspec/[feature]-techspec.md` deve existir com status `ok`
- `systems/[sistema]/guidelines/testing.md` deve existir — se ausente, alertar e sugerir `/guidelines` antes de prosseguir
- Definir o modo antes de iniciar:
  - **TDD mode:** nenhum código de produção da task ainda existe
  - **Audit mode:** código da task já está implementado

## Workflow

### Fase 0 — Leitura de contexto

1. `docs/tasks/[feature]-tasks.md` — localizar a task pelo ID, extrair critérios de aceite
2. `docs/techspec/[feature]-techspec.md` — seção de Estratégia de Testes (framework, tipos de teste exigidos, cobertura mínima)
3. `systems/[sistema]/guidelines/testing.md` — framework, convenções de nomenclatura, estrutura de arquivos de teste
4. RFs de origem da task (no PRD) — extrair blocos Gherkin (`Dado/Quando/Então`) associados

### Fase 1 — Determinar o modo

- Perguntar ao usuário, se não estiver explícito no pedido: "Task ainda não implementada (TDD) ou código já existe e você quer fechar cobertura (audit)?"
- **TDD mode:** cada bloco Gherkin do RF vira um teste que falha (Red) antes de qualquer código de produção — delega a implementação em si para `/tdd` ou `/implement`
- **Audit mode:** ler o código já implementado, mapear caminhos e branches não cobertos, gerar testes complementares para os critérios de aceite ainda sem teste

### Fase 2 — Geração da suíte

2.1. Para cada critério de aceite da task, gerar pelo menos um caso de teste:
   - Nome do teste descreve o comportamento esperado, não a implementação
   - Cobrir caminho feliz + edge cases citados no Gherkin
   - Seguir estrutura de arquivo e nomenclatura de `testing.md`

2.2. Priorizar tipos de teste conforme a estratégia da TechSpec (unitário, integração, contrato) — não gerar tipos não previstos sem necessidade

2.3. Salvar arquivos de teste incrementalmente à medida que forem gerados, não aguardar a suíte inteira

### Fase 3 — Execução e relatório de cobertura

3.1. Executar a suíte com o runner definido em `testing.md`

3.2. Reportar:
   - Testes passando / falhando
   - Critérios de aceite ainda sem teste correspondente (se houver)
   - Cobertura obtida vs. mínimo exigido pela TechSpec

3.3. Se algum teste falhar em audit mode: reportar como possível bug real, não corrigir silenciosamente o teste para passar

### Fase 4 — Próximos passos

- **TDD mode:** sugerir `/tdd` ou `/implement` para o código que fará os testes passarem (Green)
- **Audit mode:** sugerir `/code-review` se a task já estiver com código revisável

## Artefatos

**Entrada:**
- `docs/tasks/[feature]-tasks.md` (obrigatório)
- `docs/techspec/[feature]-techspec.md` (obrigatório — `ok`)
- `systems/[sistema]/guidelines/testing.md` (obrigatório)

**Saída:**
- Arquivos de teste (fora do workspace de artefatos SDD, seguindo estrutura de `testing.md`)
- Relatório de cobertura (comunicado ao usuário, não persistido como artefato)

## Canvas

Esta skill **não atualiza** o canvas diretamente. Não requer leitura de dimensões do canvas — a fonte de contexto é a TechSpec e as guidelines de testing.

## Handoff

Ao concluir, registrar em `memory/state.md` (seção da feature ativa):

```markdown
- **Testes gerados:** TASK-[EPIC].[SEQ] — [modo: TDD | audit] — [data]
- **Resultado:** [N] passando / [M] falhando
- **Cobertura:** [%] (mínimo exigido: [%])
- **Próximo passo:** [/tdd | /implement | /code-review]
```

