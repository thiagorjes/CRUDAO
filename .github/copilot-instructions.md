# SSPDD Copilot Instructions

## /analyze


## Objetivo

Verificar a consistência entre PRD, TechSpec, artefatos de design (`/designer`), Tasks e o REASONS Canvas de uma feature antes de gerar Tasks ou de iniciar a implementação. Detecta RFs sem task correspondente, tasks sem RF de origem, telas/estados do protótipo sem requisito ou decisão técnica correspondente, divergências entre o canvas e a TechSpec, contradições entre artefatos e riscos de segurança não endereçados. Gera relatório agrupado por severidade, com métricas de cobertura e remediação assistida que devolve o achado ao artefato de origem (PRD ou TechSpec).

## Argumentos recebidos

- (sem argumento) — analisa os artefatos mais recentes da feature ativa em `memory/state.md`; se `docs/tasks/[feature]-tasks.md` ainda não existe, roda automaticamente em modo `--pre-tasks`
- `"nome-da-feature"` — analisa artefatos de uma feature específica
- `--pre-tasks` — gate a ser executado **antes** do `/tasks`: analisa PRD × TechSpec × artefatos de `/designer` (screen-map, matriz de estados), sem exigir Tasks. É o modo indicado quando a feature tem front-end/UI, para evitar que lacunas de tela/estado só apareçam em homologação
- `--security` — ativa a Fase 3.5 (passe de segurança) mesmo sem sinais óbvios no texto
- `--prd-only` — limita a análise à consistência interna do PRD (sem TechSpec/Design/Tasks/Canvas)

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir
- `docs/techspec/[feature]-techspec.md` deve existir
- `docs/tasks/[feature]-tasks.md` deve existir — **exceto em `--pre-tasks`**, onde é ignorado
- `docs/design/[feature]/screen-map.md` — opcional; se ausente (feature sem `/designer`), pular Fase 1.5 e informar no relatório
- `docs/spdd/[feature]-canvas.md` — opcional; se ausente, pular verificação de divergência de canvas e informar no relatório

## Workflow

### Fase 0 — Leitura de contexto

Ler nesta ordem, sem pular:

1. `docs/prd/[feature]-prd.md` — extrair todos os RFs e RNFs
2. `docs/techspec/[feature]-techspec.md` — extrair decisões técnicas e Matriz de Rastreabilidade
3. `docs/tasks/[feature]-tasks.md` — extrair todas as TASKs e seus RFs de origem declarados
4. `docs/spdd/[feature]-canvas.md`, se existir — extrair dimensões preenchidas

### Fase 1 — Mapeamento RF → Task (gaps)

**Pular esta fase em `--pre-tasks`** (Tasks ainda não existe — é exatamente o que este modo antecede).

1. Construir o conjunto de RFs do PRD e o conjunto de RFs referenciados em tasks
2. Identificar RFs do PRD sem nenhuma task correspondente (**gap de cobertura**)
3. Identificar tasks sem RF de origem declarado (**task órfã**)
4. Executar `check_rf_coverage.py` como verificação automatizada complementar (não substitui a leitura manual — RFs mencionados só no texto sem tag podem escapar ao regex)

### Fase 1.5 — Mapeamento Tela/Estado → PRD/TechSpec (gaps de protótipo)

Executar sempre que `docs/design/[feature]/screen-map.md` existir (obrigatório em `--pre-tasks` quando a feature tem UI; nas demais chamadas, best-effort).

1. Ler o screen-map e a matriz de estados por tela (loading, erro, vazio, sucesso, variações de interação)
2. Para cada tela e cada estado da matriz, verificar:
   - Existe RF no PRD que cubra esse estado? Se não → **gap de cobertura de protótipo** (o estado existe visualmente mas não é requisito rastreável)
   - Existe decisão técnica na TechSpec (componente, contrato, regra de validação) que sustente esse estado? Se não → **gap de cobertura de protótipo** (RF existe mas TechSpec não desceu ao nível de detalhe do protótipo)
3. Verificar o inverso: RFs de UI no PRD sem tela correspondente no screen-map (**divergência protótipo-requisito** — pode indicar protótipo desatualizado)
4. Este é o gate que evita o cenário "protótipo aprovado ≠ implementado": se um estado do protótipo não tem RF nem decisão técnica registrada antes do `/tasks`, nenhuma task nem BDD vai cobri-lo.

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
| 🔴 CRÍTICO | RF sem cobertura de tasks; estado de tela do protótipo sem RF nem decisão técnica (gap de protótipo); violação de princípio DEVE da `constitution.md`; contradição direta entre documentos; dado sensível sem proteção declarada |
| 🟡 ALTO | RNF sem métrica mensurável; divergência de canvas em dimensão crítica (E/A/S); endpoint sem contrato ou sem auth; RF de UI sem tela correspondente no screen-map |
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
| G2 | Gap de protótipo | 🔴 | screen-map: Tela Checkout, estado "erro de pagamento" | Estado sem RF nem decisão técnica | Devolver ao /prd (novo RF) ou /techspec (decisão técnica) |

## Cobertura RF × Tasks

| RF | Tasks | Status |
|----|-------|--------|
| RF-001 | TASK-1.1 | ✅ Coberto |
| RF-002 | — | ❌ Sem cobertura |

## Cobertura Tela/Estado × PRD/TechSpec (se screen-map existir)

| Tela | Estado | RF | Decisão TechSpec | Status |
|------|--------|----|--------------------|--------|
| Checkout | erro de pagamento | — | — | ❌ Sem cobertura |
| Checkout | sucesso | RF-010 | Seção 4.2 | ✅ Coberto |
```

Salvar o arquivo após concluir cada fase (0 findings ainda é resultado válido a persistir, não motivo para pular a seção).

### Fase 6 — Remediação assistida (com roteamento ao artefato de origem)

Ao final do relatório, perguntar:

> "Deseja que eu encaminhe os findings 🔴/🟡 para correção? Informe quais (ex: 'G1, S2') ou 'todos os críticos'."

Para cada finding aceito, rotear pelo tipo — **nunca editar Tasks/código para compensar um gap de especificação**:

| Tipo de finding | Destino | Ação |
|---|---|---|
| Gap de cobertura (RF sem task) | `/tasks` | Reexecutar `/tasks` após o artefato de origem estar corrigido, se o gap vier de PRD/TechSpec incompletos |
| Gap de protótipo (estado sem RF) | `/prd` (ou `/clarify`) | Devolver ao PRD para registrar o RF/critério de aceite faltante; se ambíguo, sugerir `/clarify` |
| Gap de protótipo (RF existe, sem decisão técnica) | `/techspec` | Devolver à TechSpec para detalhar a decisão/componente que sustenta o estado |
| Divergência de canvas | Skill dona da dimensão divergente | Apontar qual skill deve reexecutar (ex: divergência em E → `/designer`/`/techspec`) |
| Contradição entre artefatos | Documento mais recente é considerado desatualizado | Perguntar ao usuário qual versão prevalece antes de editar |
| Risco de segurança | `/prd` ou `/techspec`, conforme a lacuna | Requisito de proteção ausente → PRD; controle técnico ausente → TechSpec |

Esta skill **nunca edita o artefato de origem sem confirmação explícita** por finding (ou grupo confirmado) — a análise em si é somente leitura até essa confirmação. Após aplicar a correção no artefato de origem, marcar o artefato correspondente como `stale` no Artifact Registry para os artefatos downstream (ex: corrigir PRD → TechSpec e Tasks ficam `stale:prd@[nova versão]`), forçando a checagem de stale já existente nas pré-condições de `/techspec` e `/tasks`.

### Fase 7 — Validação e handoff

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/analyze/validate-rules.json \
  --artifact docs/analyze/[feature]-analysis.md
```

Se houver findings 🔴: alertar o usuário que `/implement` está bloqueado até resolução. Em modo `--pre-tasks`, alertar adicionalmente que **`/tasks` está bloqueado** até o veredicto ser ✅ ou ⚠️ (sem 🔴).

## Artefatos

**Entrada:**
- `docs/prd/[feature]-prd.md` (obrigatório)
- `docs/techspec/[feature]-techspec.md` (obrigatório)
- `docs/tasks/[feature]-tasks.md` (obrigatório — dispensado em `--pre-tasks`)
- `docs/design/[feature]/screen-map.md` (opcional; obrigatório de fato em `--pre-tasks` quando a feature tem UI)
- `docs/spdd/[feature]-canvas.md` (opcional)

**Saída:**
- `docs/analyze/[feature]-analysis.md`

## Canvas

Esta skill **não atualiza** o canvas. Lê as dimensões preenchidas apenas para comparação com a TechSpec na Fase 2, sem assinar nenhuma dimensão como `_Atualizado por: /analyze_`.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Análise executada:** /analyze [--pre-tasks] — [data]
- **Findings:** 🔴 [N] | 🟡 [N] | 🟠 [N] | 🔵 [N]
- **Veredicto:** [✅|⚠️|❌]
- **Artefato:** docs/analyze/[feature]-analysis.md
- **Se `--pre-tasks` e veredicto ✅/⚠️:** liberado para `/tasks`
```

Artifact Registry:
```
| analyze/[feature]-analysis.md | 1.0 | ok |
```


## /checklist


## Objetivo

Aplicar um checklist de qualidade a um artefato (PRD ou TechSpec), tratando cada requisito como um caso a validar — não a implementação em si, mas a qualidade da especificação. Gera `docs/checklists/[feature]-[tipo].md` diferenciando itens críticos (bloqueiam a próxima etapa do pipeline) de itens não-críticos (melhorias sugeridas).

## Argumentos recebidos

- (sem argumento) — gera/atualiza o checklist do artefato mais recente, perguntando o tipo (PRD/TechSpec) se ambíguo
- `"nome-da-feature"` — aplica ao artefato de uma feature específica
- `--audit` — não gera checklist novo; audita todos os checklists existentes em `docs/checklists/` e reporta % de aprovação por arquivo

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

## Modo `--audit`

Quando invocado com `--audit`, pular as Fases 0-4 e executar:

1. Ler todos os arquivos em `docs/checklists/`
2. Para cada um, contar: total de itens `CHK-NNN` / itens marcados `[x]` (aprovados) / itens críticos ainda abertos
3. Reportar:

```markdown
## Auditoria de Checklists — [data]

| Arquivo | Total | Aprovados [x] | % | Críticos abertos |
|---------|-------|--------------|---|-------------------|
| [feature]-prd.md | N | N | N% | N |

### Checklists com baixa aprovação (< 80%) ou críticos abertos
- [arquivo] — [N itens pendentes — resumo do que falta]

### Recomendação
- [arquivo]: [ação sugerida — ex: "resolver os N críticos antes de /techspec"]
```

Não persistir a auditoria como artefato — é um relatório de sessão apresentado ao usuário.

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

## Argumentos recebidos

- (sem argumento) — revisa as mudanças staged/unstaged atuais
- `TASK-2.1` — revisa com foco nos critérios de aceite desta task
- `--security` — aprofunda a categoria 3 (todas as categorias OWASP Top 10, análise de dependências, verificação de secrets hardcoded)
- `--full` — revisão completa com todas as categorias em detalhe máximo

## Pré-condições

- Código implementado disponível (diff ou arquivos)
- `docs/tasks/[feature]-tasks.md` com a task revisada
- `docs/techspec/[feature]-techspec.md`
- `docs/spdd/[feature]-canvas.md`
- `systems/[sistema]/guidelines/` para referência de padrões

## Workflow

### Fase 0 — Leitura de contexto e gate de testes

1. **Resolver o sistema em revisão**: pelo campo `Sistema:` da task, ou o repositório onde estão os arquivos alterados. O diff e os comandos git rodam **dentro de `systems/[sistema]/`** — nunca na raiz do workspace.
2. Identificar a task sendo revisada (ID e critérios de aceite)
3. Ler `docs/spdd/[feature]-canvas.md` — dimensão S atual (Safeguards já conhecidos)
4. Ler `docs/techspec/[feature]-techspec.md` — seção de Segurança e Observabilidade
5. Ler guidelines relevantes: `security.md`, `coding-standards.md`, `testing.md`
6. **Gate obrigatório — executar a suíte de testes antes de revisar:**
   - Se os testes **falharem**: reportar imediatamente como 🔴 CRÍTICO "Testes falhando" e encerrar com veredicto `❌ Requer alterações`. Não prosseguir para a Fase 1 — um código com testes falhando não está pronto para review.
   - Se os testes **passarem**: prosseguir normalmente.
   - Se não for possível executar os testes (ambiente sem runtime): sinalizar no relatório e continuar com revisão estática apenas.

### Fase 1 — Revisão por categoria

Revisar o código em **5 categorias obrigatórias**, documentando findings com localização. Com `--security`: aprofundar a categoria 3 cobrindo todo o OWASP Top 10 e análise de dependências (`npm audit`/`pip audit` ou equivalente). Com `--full`: aplicar o mesmo nível de detalhe às 5 categorias.

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

### Fase 1.5 — Verificação dos critérios de aceite (se task fornecida)

Para cada critério de aceite da task, produzir uma linha de evidência — não apenas afirmar que foi atendido:

| # | Critério de Aceite | Verificado? | Evidência |
|---|---------------------|-------------|-----------|
| 1 | [AC da task] | ✅ / ❌ / ⚠️ | [arquivo:linha ou "não encontrado"] |

### Fase 1.6 — Auto-fix limitado para findings de código puro (ADR-015)

Para cada finding 🟡/🔵 identificado nas categorias 2 (qualidade de código) e 5 (observabilidade), e para findings 🔴/🟡 de segurança que **não** exigem mudança de requisito (ex: input validation ausente, secret hardcoded, log expondo dado sensível — corrigíveis só no código):

1. Classificar o finding como **código puro** (não altera comportamento especificado, não exige reescrever RF/RNF/critério de aceite) ou **revela problema de spec** (comportamento especificado é ambíguo, incompleto ou o próprio critério de aceite está errado).
2. **Findings de spec puro:** nunca entram no loop de auto-fix — registrar normalmente no relatório (Fase 2) e sinalizar explicitamente que a causa raiz é de especificação, recomendando `/analyze` ou `/clarify` em vez de correção de código.
3. **Findings de código puro:** tentar corrigir automaticamente, até **3 tentativas** por finding:
   - Aplicar a correção no código de produção/teste
   - Reexecutar a suíte de testes relevante
   - Se passar e o finding não reaparecer na releitura: marcar como corrigido automaticamente
   - Se falhar após 3 tentativas: reverter para o estado anterior, manter o finding como pendente no relatório (não deixar código quebrado por causa do loop)
   - **Nunca editar, nesta fase, `docs/prd/`, `docs/techspec/`, `docs/spdd/*-canvas.md` ou `docs/tasks/`** — se a correção "óbvia" exigiria tocar algum desses, o finding é de spec, não de código, e cai na regra do item 2
4. Registrar cada finding auto-corrigido com nota `[auto-fix aplicado — N tentativa(s)]` no relatório da Fase 2, mantendo a descrição do problema original para rastreabilidade.

### Fase 2 — Geração do relatório

Criar `docs/checklists/[feature]-[task-id]-review.md` com:

**Formato de finding — cada um cita o guideline violado e mostra antes/depois quando aplicável:**
```
#### [C1|I1|S1] [Título conciso do problema]
Arquivo: [caminho:linha]
Problema: [o que está errado e por que é um problema]
Como corrigir:
  Atual:   [trecho problemático]
  Correto: [como deve ficar]
Guideline violado: [arquivo.md — seção específica] (obrigatório — se não houver guideline cobrindo o caso, dizer explicitamente "não coberto — recomendo adicionar")
```

**Seções obrigatórias do relatório:**
- `## Critérios de Aceite` — tabela da Fase 1.5 (se task fornecida)
- `## 🔴 Crítico` — bloqueiam o merge (vazio = "nenhum")
- `## 🟡 Importante` — devem ser corrigidos antes do merge (vazio = "nenhum")
- `## 🔵 Sugestão` — melhorias que não bloqueiam (vazio = "nenhuma")
- `## ✅ Pontos Positivos` — algo bem feito no código revisado (cultura de feedback — nunca omitir, mesmo que curto)
- `## Segurança` — findings de segurança (vazio = "Nenhum finding de segurança")
- `## Conformidade com TechSpec` — desvios da especificação
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
- Nº de findings corrigidos automaticamente (Fase 1.6) vs. pendentes
- Itens que BLOQUEIAM o merge (CRÍTICOS não resolvidos)
- Itens que devem ser resolvidos antes do merge (IMPORTANTES)
- Findings sinalizados como "problema de spec" — recomendar `/analyze` ou `/clarify`, nunca corrigidos aqui
- Sugestões para iterações futuras

Se REPROVADO: listar exatamente o que corrigir antes de re-review.
Se APROVADO ou APROVADO COM RESSALVAS: sugerir próximos passos, incluindo `/spdd-sync` quando algum finding (auto-corrigido ou não) tocou entidade, dependência ou padrão de arquitetura fora do que o canvas já descrevia.

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

Você é o **Design Lead** do pipeline: conduz a discovery de UX/UI **depois do PRD de negócio e antes da TechSpec** — o Design Brief que você gera informa as decisões de arquitetura frontend do `/techspec`. **Você não escreve HTML nesta skill** — ao final, aciona o agente prototipador (`.agents/agents/designer.md`) para materializar tokens e protótipos em background. Cada decisão de design system relevante gera um Decision Record do tipo DDR.

Pipeline: `/prd` → **`/designer`** → `/techspec` → `/tasks` → `/implement`

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir com status `ok` no Artifact Registry
- `memory/state.md` com a feature registrada

## Regra fundamental — interação interativa

- Perguntar **uma de cada vez**, aguardando resposta antes de avançar.
- Pular qualquer tópico já respondido pela Fase 0 (detecção) — nunca perguntar o que já está decidido em um design system existente.

## Workflow

### Fase 0 — Detecção de projeto existente (silenciosa, sempre primeiro)

Antes de qualquer pergunta, ler nesta ordem de prioridade (fonte superior prevalece em caso de conflito):

1. `systems/[sistema]/guidelines/design.md` — design system corporativo do sistema, se existir e tiver conteúdo real (tokens, inventário de componentes). **Fonte de verdade** — não perguntar sobre nada já coberto.
2. `DESIGN.md` na raiz do sistema — alternativa agnóstica ao item 1, mesma autoridade.
3. `docs/design/[outra-feature]-design-brief.md` de feature anterior já aprovada no mesmo sistema — ler para não repetir decisões de marca.
4. `docs/design/[outra-feature]/design-tokens.json` mais recente — extrair valores exatos já em uso.
5. Diretório de tema no código do sistema (`src/theme/`, `styles/tokens/`, `design-system/` etc.) — ler para extrair tokens reais já implementados.
6. `docs/design/*/prototypes/` — listar protótipos existentes para entender o padrão visual estabelecido.

| Situação detectada | Comportamento |
|---|---|
| Design system (`guidelines/design.md` ou `DESIGN.md`) com conteúdo | Pular integralmente as perguntas de marca/cor/tipografia/navegação da Fase 2. Informar: "Design system detectado em [caminho] — padrões visuais carregados." |
| Brief anterior + tema no código, sem design system formal | Pular perguntas de marca/cor/tipografia; focar em escopo, fluxos e estados da feature atual |
| Tema no código, sem brief e sem design system | Extrair tokens do código; perguntar apenas tom, navegação e escopo |
| Projeto novo, sem nenhum artefato | Executar a Fase 2 completa |

### Fase 1 — Leitura de contexto

1. `docs/prd/[feature]-prd.md` — objetivo de negócio, personas, jornadas, RFs
2. `memory/state.md` — confirmar versão do PRD no Artifact Registry
3. `docs/spdd/[feature]-canvas.md`, se existir — ler dimensão E já preenchida por `/prd`/`/techspec` para não duplicar entidades de domínio
4. Identificar quais RFs implicam interface visual (telas, formulários, listagens, modais, notificações)

### Fase 2 — Entrevista de design

Perguntar apenas o que a Fase 0 não resolveu:

**[Somente projeto novo] 1. Personalidade da marca** — tom do produto (sério/corporativo, jovem/vibrante, minimalista, lúdico).

**[Somente projeto novo] 2. Paleta e temas** — cor primária/acento/fundo/superfície/erro/sucesso/texto; suporte a Light only / Dark only / Ambos.

**[Somente projeto novo] 3. Referências visuais** — produto concorrente ou não que serve de inspiração.

**[Somente projeto novo] 4. Tipografia e grid** — fonte heading/body/mono, escala de tamanhos, base de espaçamento (4px/8px), breakpoints mobile/tablet/desktop.

**[Somente projeto novo] 5. Estrutura de navegação** — sidebar, topbar, bottom nav, tabs etc.

**[Sempre] 6. Inventário de telas e fluxos** — para cada tela/view no escopo da feature:
- Nome da tela, RF(s) atendido(s), persona(s) que a utiliza, rota sugerida, origem/destino na navegação
- Mapear o happy path de ponta a ponta
- Mapear ao menos um fluxo de erro crítico

**[Sempre] 7. Estados por tela** — para cada tela do inventário, marcar quais estados são obrigatórios no protótipo: idle, loading, preenchido, erro, sucesso, vazio.

**[Sempre] 8. Responsividade, acessibilidade e i18n** — plataformas-alvo e breakpoint prioritário; nível de acessibilidade (WCAG AA mínimo, navegação por teclado, leitor de tela); suporte a múltiplos idiomas (impacta layout).

**[Sempre] 9. Decisões em aberto** — decisões de produto que o protótipo deve ajudar a responder; quantas variações de layout explorar.

Salvar respostas incrementalmente no Design Brief à medida que forem obtidas — não aguardar o fim da entrevista.

### Fase 3 — Geração do Design Brief

Gerar/atualizar `docs/design/[feature]-design-brief.md` cobrindo (uma seção por bloco da entrevista):

1. Contexto e objetivo (referência ao PRD)
2. Identidade visual (tom, paleta com hex, tipografia, radius, espaçamento, tema suportado, referência visual)
3. Navegação e layout (padrão, breakpoints, componentes existentes a reutilizar)
4. Inventário de telas (tabela: ID, nome, RF(s), persona, rota)
5. Fluxos de navegação (happy path + fluxo de erro)
6. Estados por tela (tabela: tela × idle/loading/preenchido/erro/sucesso/vazio, marcando obrigatórios)
7. Acessibilidade e internacionalização
8. Decisões em aberto (questão, opções, impacto)
9. Escopo do protótipo (quais telas, quantas variações, estados obrigatórios)
10. Decision Records de Design (DDRs desta fase)

### Fase 4 — Confirmação com o usuário

Apresentar resumo objetivo do brief salvo (N telas no escopo, fluxos mapeados, estados obrigatórios, requisitos de acessibilidade) e perguntar se está correto ou precisa de ajuste antes de acionar o prototipador. Aplicar correções no arquivo salvo se necessário.

### Fase 5 — Decision Records de Design

Para cada decisão de design system relevante tomada nas Fases 2-3 (ex: escolha de paleta, escolha de grid, padrão de componente não trivial):
1. Verificar próximo número de sequência DDR no índice de `memory/constitution.md`
2. Criar `docs/decisions/ddr-[NNN]-[slug].md` a partir do template de Decision Record
3. Adicionar ao índice de DDRs em `memory/constitution.md`
4. Referenciar o DDR na seção 10 do Design Brief

### Fase 6 — Atualização do Canvas (dimensão E)

Atualizar dimensão **E — Entities** do canvas `docs/spdd/[feature]-canvas.md`, complementando (não substituindo) as entidades de domínio já registradas por `/prd`/`/techspec` com as entidades de UX/UI:
- Telas e componentes principais e seus tokens
- `_Atualizado por: /designer v1.0 — [data]_`
- `> Decisões: DDR-[NNN], ...`

Salvar o canvas após a atualização.

### Fase 7 — Handoff para o agente prototipador (obrigatório, não sob demanda)

Perguntar: "Deseja acionar o agente prototipador agora para gerar screen-map, tokens e protótipo HTML? [Sim/Não]"

- **Se sim:** acionar o agente autônomo `designer` (`.agents/agents/designer.md`) pelo mecanismo nativo do ambiente (ferramenta `Agent`/`Task` no Claude Code referenciando `.claude/agents/designer.md`; mecanismo equivalente de subagente em outras plataformas; sem suporte a subagentes, executar o protocolo do agente na mesma sessão). O agente lê o Design Brief e o PRD e gera, sem fazer novas perguntas: `docs/design/[feature]/screen-map.md`, `docs/design/[feature]/design-tokens.json` e `docs/design/[feature]/prototypes/*.html`.
- **Se não:** encerrar informando que o brief está salvo e o próximo passo é `/techspec`; deixar claro que tokens/protótipo ficam pendentes e devem ser gerados antes do `/techspec` avançar para decisões de UI.
- **Nunca pular esta fase silenciosamente** — é a causa mais comum de o design ficar incompleto (brief sem tokens/protótipo).

### Fase 8 — Validação e handoff final

1. Validar o Design Brief (a validação também checa a existência de `screen-map.md`, `design-tokens.json` e ao menos um protótipo `.html` gerados pelo agente prototipador):
   ```
   python .agents/scripts/validate.py --mode output \
     --rules .agents/skills/designer/validate-rules.json \
     --artifact docs/design/[feature]-design-brief.md
   ```
2. Atualizar `memory/state.md`:
   - Artifact Registry: `docs/design/[feature]-design-brief.md | 1.0 | ok`
3. Sugerir próximo passo: `/techspec` (o `screen-map.md` pode ser usado como referência de telas e rotas)

## Artefatos

**Entrada:**
- `docs/prd/[feature]-prd.md` (obrigatório)
- `memory/state.md` (obrigatório)
- `docs/spdd/[feature]-canvas.md` (opcional — se já existir)
- `systems/[sistema]/guidelines/design.md` ou `DESIGN.md` (opcional — design system existente)

**Saída:**
- `docs/design/[feature]-design-brief.md`
- `docs/design/[feature]/screen-map.md` (gerado pelo agente prototipador na Fase 7)
- `docs/design/[feature]/design-tokens.json` (gerado pelo agente prototipador na Fase 7)
- `docs/design/[feature]/prototypes/*.html` (gerado pelo agente prototipador na Fase 7; publicação como Artifact é complemento opcional quando a ferramenta estiver disponível — nunca substitui o arquivo local)
- `docs/decisions/ddr-[NNN]-[slug].md` (um por decisão de design system)

## Canvas

Esta skill atualiza:
- **E — Entities:** entidades de UX/UI (telas, componentes, tokens), complementando as entidades de domínio
- Referências a DDRs criadas nesta fase: `> Decisões: DDR-001, ...` (ou `> Decisões: —` se nenhuma)

## Handoff

Ao concluir, registrar em `memory/state.md` (seção da feature ativa):

```markdown
- **Etapa concluída:** /designer (v1.0) — [data]
- **Artefato:** docs/design/[feature]-design-brief.md
- **Screen map:** docs/design/[feature]/screen-map.md
- **Design tokens:** docs/design/[feature]/design-tokens.json
- **Protótipos:** docs/design/[feature]/prototypes/*.html [+ URLs de Artifact, se publicado]
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

Gerar os guidelines de um sistema, capturando decisões de stack, arquitetura e padrões de engenharia através de entrevista interativa — adaptada ao **cenário** do sistema (greenfield, brownfield ou migração). Cada decisão técnica significativa gera um ADR. Os guidelines são a fonte de verdade para todas as skills subsequentes do pipeline, incluindo o design system lido pelo `/designer`.

## Argumentos recebidos

- (sem argumento) — descobre os sistemas pendentes em `memory/state.md` e conduz o processo para eles
- `[sistema]` (ex: `api-auth`) — foca no sistema informado
- `[sistema] [arquivo]` (ex: `api-auth security`) — foca em um arquivo específico de um sistema já configurado

## Pré-condições

- `memory/state.md` deve existir com o sistema registrado na tabela de Sistemas
- `systems/[sistema]/` deve existir (ou será criado) — é o **repositório do sistema**: além de `guidelines/`, é onde `/tasks`, `/implement`, `/tdd` e `/code-review` criam e editam todo o código (ex: `systems/[sistema]/backend/`, `systems/[sistema]/frontend/`). Nunca na raiz do workspace, mesmo em projetos de sistema único.
- Se guidelines já existem: perguntar se deseja atualizar (bump de versão) ou substituir

## Workflow

### Fase 0 — Verificação e detecção de cenário

1. Identificar o sistema-alvo: ler o argumento ou perguntar
2. Verificar se `systems/[sistema]/guidelines/` já existe
   - Se sim: listar os arquivos existentes e perguntar "Atualizar guidelines existentes?"
3. Criar diretório `systems/[sistema]/guidelines/` se não existir
4. **Determinar o cenário** (gravado na tabela Sistemas de `memory/state.md`, ou perguntar se ausente):

| Cenário | Fonte primária das respostas | Comportamento da entrevista |
|---|---|---|
| **Greenfield** (novo, sem código) | Entrevista completa | Perguntas prescritivas — define os padrões do zero |
| **Brownfield** (código existente adotando SSPDD) | Inventário do código (Fase 0.5) | Módulos viram confirmação: "Detectei X — confirma?"; só pergunta o que não pode inferir |
| **Migração** (legado trocando de tecnologia) | Inventário do legado (as-is) + entrevista do alvo (to-be) | Duas passadas: inventário automático gera `legacy-context.md`; entrevista prescritiva define os guidelines do alvo + Módulo de Migração |

### Fase 0.5 — Inventário do código (obrigatório para Brownfield e Migração)

> Em Greenfield, pular esta fase.

Antes de perguntar qualquer coisa, montar o inventário — a resposta já está no repositório:

1. **Stack e dependências**: manifests (`package.json`, `pyproject.toml`, `pom.xml`, `go.mod`, `*.csproj`) — linguagens, frameworks, versões exatas
2. **Estrutura**: árvore de `src/` (ou equivalente) — padrão arquitetural em prática
3. **Ferramentas**: configs presentes (`.eslintrc*`, `.prettierrc*`, `ruff.toml`, configs de teste, pipelines em `.github/workflows/`) — lint, testes, CI
4. **Convenções git**: amostra do histórico (`git log --oneline -20` dentro de `systems/[sistema]/`) — formato de commits e branches em uso
5. Registrar cada item como inferência com evidência (ex: "Arquitetura: por camada — `src/controllers`, `src/services`") — na Fase 1 esses itens são **confirmados em bloco**, não perguntados do zero

**Adicionalmente para Migração**: identificar também integrações externas, pontos de entrada (rotas, jobs, listeners) e débitos/riscos evidentes (dependências EOL, áreas sem teste) — alimenta `legacy-context.md` (Fase 3).

Apresentar o diagnóstico em texto antes de iniciar a entrevista: o que foi inferido, o que será confirmado, o que precisa ser perguntado do zero.

### Fase 1 — Entrevista de stack (uma pergunta por vez)

**Regra de arquivos extras**: se `systems/[sistema]/guidelines/` já contém um arquivo fora do padrão (ex: `GUIDELINE_ARQUITETURA.md`) cobrindo total ou parcialmente um módulo abaixo, pular as perguntas já respondidas por ele — na Fase 3, o arquivo padrão correspondente só registra uma referência ao arquivo extra + eventuais gaps, sem duplicar conteúdo.

**Brownfield**: onde o inventário da Fase 0.5 já inferiu a resposta, confirmar em bloco ("Detectei: TypeScript 5.4, NestJS 10, PostgreSQL via Prisma — confirma?") e perguntar apenas os gaps. Se o usuário quiser um padrão diferente do que o código pratica, registrar as duas colunas no guideline: *estado atual* e *alvo*.

**Migração**: conduzir como greenfield para o sistema **alvo** (to-be); o inventário do legado é só referência. Ao final, conduzir também o Módulo de Migração.

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

**Módulo I — Design e UI/UX** *(pular se o sistema for puramente backend/API ou CLI sem interface gráfica)*

Antes de perguntar: verificar, na ordem, `DESIGN.md` na raiz do sistema, `systems/[sistema]/guidelines/design.md` com conteúdo real, `docs/design/*/design-tokens.json` de feature anterior, ou diretório de tema no código (`src/theme/`, `styles/tokens/`). Se algo for encontrado, extrair e **perguntar só os gaps**.

- "Já existem tokens de cor definidos (hex de primária/acento/fundo/erro/sucesso) ou preciso levantá-los no `/designer`?"
- "Qual a estratégia de componentes? (100% customizado / biblioteca headless + customização / biblioteca opinionada completa)"
- "Qual o padrão de iconografia?"
- "Quais os breakpoints e a estratégia de responsividade (mobile-first / desktop-first / ambos)?"
- "Qual o nível mínimo de acessibilidade exigido? (WCAG AA / AAA / sem requisito formal)"

Se nada for encontrado e o sistema tiver UI: conduzir o módulo completo — a resposta popula `guidelines/design.md` (Fase 3), que o `/designer` e o agente prototipador leem como fonte de verdade.

**Módulo de Migração** *(somente cenário Migração — respostas geram `legacy-context.md` e um ADR inicial)*
- "Qual abordagem de migração? (Strangler Fig / Big Bang / convivência paralela / a definir)"
- "Qual o nível de paridade exigido com o legado? (total / funcional / parcial — descrever)"
- "Como os dados do legado serão tratados? (migração completa / incremental / sistema novo começa vazio)"
- "Qual o critério de corte do legado? (ex: 100% do tráfego no novo por 30 dias sem incidente)"

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
10. `systems/[sistema]/guidelines/design.md` — **somente se o Módulo I foi conduzido** (sistema com UI). Tokens visuais (hex exatos), inventário de componentes, breakpoints, acessibilidade — é a fonte que `/designer` e o agente prototipador leem antes de gerar qualquer protótipo. Se um design system externo (`DESIGN.md`) já é a fonte primária, este arquivo só referencia + registra gaps complementares
11. `legacy-context.md` — **somente cenário Migração**. Stack legada, pontos de entrada, integrações externas, comportamentos a preservar, débitos conhecidos, e a estratégia de migração decidida no Módulo de Migração. Registrar a decisão também como ADR inicial em `memory/constitution.md`

Executar validação ao final:
```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/guidelines/validate-rules.json \
  --artifact systems/[sistema]/guidelines/stack.md \
  --system [sistema]
```

### Fase 3.5 — Comitê de Análise Assíncrono (opcional)

Antes de finalizar, oferecer revisão cruzada:

> "Os guidelines foram estruturados. Deseja que eu submeta este planejamento aos agents especialistas (Architect, Security, DevOps) em background para revisar antes de fechar? [Sim/Não]"

- **Se sim**: invocar os agents `.agents/agents/architect.md`, `.agents/agents/security.md`, `.agents/agents/devops.md` via ferramenta `Agent`, instruindo cada um a **ler os arquivos recém-salvos em disco** (não colar o conteúdo no prompt) e avaliar riscos, gaps e trade-offs não considerados. Apresentar o feedback consolidado (1-3 pontos por agent) e perguntar se aceita incorporar antes de fechar. Se aceitar, atualizar os arquivos e re-executar a validação.
- **Se não**: seguir direto para a Fase 4.

### Fase 4 — Handoff

Atualizar `memory/state.md`:
- Tabela de Sistemas: marcar Guidelines como `ok` para o sistema
- Artifact Registry: adicionar cada arquivo de guidelines com status `ok`
- Se restarem sistemas pendentes na fila da Fase 0: retornar à Fase 0.5/1 para o próximo antes de encerrar

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)

**Saída:**
- `systems/{{SYSTEM}}/guidelines/` — 9 arquivos padrão + `design.md` (se sistema com UI) + `legacy-context.md` (se Migração)
- `docs/decisions/ADR-[NNN]-*.md` — um ou mais ADRs de decisões de stack (inclui ADR de estratégia de migração, se aplicável)

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

## Argumentos recebidos

- `TASK-2.1` — implementa a task pelo ID (com decisão TDD automática)
- `"Título da task"` — implementa pela descrição
- (sem argumento) — lista as tasks disponíveis e pergunta qual executar
- `TASK-2.1 --no-tdd` — implementação direta, sem ciclo TDD mesmo se os critérios da Fase 1 indicariam TDD

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

**Regra fundamental de localização — todo trabalho desta skill acontece dentro de `systems/[sistema]/`:** criação/edição de arquivos de código, execução de testes e comandos git rodam nesse diretório, no repositório daquele sistema — nunca na raiz do workspace. Resolver `[sistema]` pelo campo `Sistema:` da task (ou o único da tabela Sistemas de `memory/state.md`, se houver apenas um). Os caminhos do "Guia técnico de implementação" da task são sempre relativos a `systems/[sistema]/`.

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

Se TDD aplicável (e `--no-tdd` não foi passado): seguir ciclo Red → Green → Refactor antes de implementar.
Se TDD não aplicável ou `--no-tdd`: implementar diretamente com verificação manual.

### Fase 1.5 — Caminho rápido vs. completo

Antes de codificar, decidir o nível de confirmação com base no tamanho da task:

- **Caminho rápido (task `[P]` — até 4h)**: se os requisitos estão claros e sem ambiguidade, pular a confirmação de plano — implementar direto, mencionando em uma linha o que será feito antes de começar.
- **Caminho completo (task `[M]`/`[G]` ou com ambiguidade)**: apresentar um plano de implementação em bullets (arquivo a criar/modificar → o que será feito) e aguardar confirmação antes de prosseguir.

Se houver ambiguidade ou informação ausente na task que impeça implementar com segurança, perguntar ao usuário antes — em qualquer um dos dois caminhos. Não assumir.

### Fase 2 — Implementação

2.1. **Verificar se arquivo-alvo já existe (dentro de `systems/[sistema]/`):**
   - Se sim: ler conteúdo antes de modificar (nunca sobrescrever cegamente)
   - Se não: criar novo dentro de `systems/[sistema]/`, seguindo os padrões de N — nunca na raiz do workspace

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

### Fase 3.5 — Handoff de code review

Task concluída — perguntar:

> "Deseja submeter os arquivos a um code review antes de prosseguir? (a) Sim — agent QA revisa em contexto fresco [recomendado para tasks M/G] / (b) Inline — review rápido no contexto atual / (c) Não — seguir direto para o relatório"

- **(a) Agent QA**: invocar `.agents/agents/qa.md` via ferramenta `Agent`, passando a lista de arquivos criados/modificados e a referência à task + critérios de aceite, instruindo-o explicitamente a **ler `.agents/skills/code-review/SKILL.md` e seguir o workflow integralmente** (não apenas "aplicar as dimensões") — incluindo o gate de testes, a taxonomia de severidade, o formato de finding, a Fase 1.6 de auto-fix limitado (ADR-015, com a fronteira de nunca editar `docs/prd/`, `docs/techspec/`, `docs/spdd/*-canvas.md`, `docs/tasks/`), a atualização da dimensão S do canvas e o artefato de saída em `docs/checklists/[feature]-[task-id]-review.md`. Apresentar o relatório recebido ao usuário.
- **(b) Inline**: seguir o workflow completo de `.agents/skills/code-review/SKILL.md` no contexto atual (mesmas categorias, gate de testes, Fase 1.6 de auto-fix e atualização do canvas).
- **(c)**: seguir direto para a Fase 4.

Se houver findings 🟡/🔴 em (a) ou (b): aguardar decisão do usuário — (i) corrigir agora, (ii) criar task de bug-fix separada, (iii) ignorar e concluir mesmo assim — antes de avançar.

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
4. Sugerir próximos passos: `/code-review` (se não feito na Fase 3.5) ou próxima task; se a implementação introduziu entidade, dependência ou padrão de arquitetura não previstos no canvas, sugerir também `/spdd-sync` para verificar divergência

**Atualizar status em todos os locais que rastreiam a task (nenhum é opcional — os três precisam ficar consistentes):**
1. Marcar `Status: Concluída` no arquivo individual `docs/tasks/[feature]/TASK-[EPIC].[SEQ]-[slug].md` da task.
2. Marcar o checkbox correspondente no documento consolidado `docs/tasks/[feature]-tasks.md` (`[ ] TASK-01.1` → `[x] TASK-01.1`) — é a lista que outras skills e o próprio usuário consultam para saber o que falta; deixar sem marcar aqui produz sugestões de "próxima atividade" incorretas mesmo com a task já concluída.
3. Atualizar `memory/state.md` **a cada task concluída, não apenas na última do Epic** — adicionar/atualizar linha `- **Task implementada:** TASK-[EPIC].[SEQ] — [data]` na seção da feature ativa. Ao concluir a última task de um Epic, adicionalmente marcar o Epic como concluído no status da feature.

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
- Antes de levantar a primeira funcionalidade: "Além do teste relacionado ao requisito em si (1x1, caminho feliz), quantas variações de comportamento você deseja incluir por RF? Ex.: 5 cenários BDD — usuário acerta a senha, usuário erra a senha, conta bloqueada, campo vazio, timeout de sessão."
  - Registrar a resposta como `N_VARIACOES` (inteiro ≥ 1; se o usuário não souber, sugerir 3 como padrão — feliz + 1 negativo + 1 edge case)
  - Piso de cenários Gherkin por RF nesta fase e na Fase 2.2 — usuário pode declarar mais variações para um RF específico, nunca menos
- "Liste as funcionalidades que o sistema DEVE ter (Must Have)."
- Para cada funcionalidade: "Como um usuário saberia que isso funciona? Descreva o comportamento esperado."
- Para cada funcionalidade, elicitar `N_VARIACOES` variações de comportamento (1 caminho feliz + `N_VARIACOES - 1` variações — negativas, edge cases, exceções): "Além do caminho feliz, quais são as [N-1] variações desse comportamento? (ex: entrada inválida, permissão negada, limite excedido, estado inesperado)". Se o usuário não conseguir listar todas de imediato, sugerir variações plausíveis com base no domínio e confirmar cada uma antes de registrar.
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
     - Adicionar `N_VARIACOES` cenários Gherkin (**Cenário 1 — caminho feliz**, **Cenário 2..N — variações** negativas/edge coletadas no Módulo B), cada um com **Dado que** / **Quando** / **Então**
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

### Fase 3.5 — Comitê de Análise Assíncrono (opcional)

Com o PRD salvo, oferecer revisão cruzada antes de liberar para `/techspec`:

> "O PRD foi gerado e salvo. Deseja que eu submeta os requisitos aos agents especialistas (Architect, Security, QA) em background para uma crítica antes de avançar? [Sim/Não]"

- **Se sim**: invocar `.agents/agents/architect.md`, `.agents/agents/security.md`, `.agents/agents/qa.md` via ferramenta `Agent`, instruindo cada um a **ler o PRD salvo em disco** (não colar o conteúdo no prompt) e apontar riscos, gaps de escopo ou requisitos não testáveis. Apresentar o feedback consolidado (1-3 pontos por agent) e perguntar se aceita que o PRD salvo seja atualizado. Se aceitar, aplicar e re-validar.
- **Se não**: seguir para a Fase 4.

### Fase 4 — Handoff

Atualizar `memory/state.md`:
- Seção Features Ativas: adicionar/atualizar entry com PRD v1.0 e status
- Artifact Registry: adicionar entrada `docs/prd/[feature]-prd.md | 1.0 | ok`
- Se canvas foi criado/atualizado: `docs/spdd/[feature]-canvas.md | — | draft`

**Detecção de interface visual:** verificar se algum sistema afetado tem front-end (ler `systems/[sistema]/guidelines/stack.md` procurando frameworks de UI — React, Next.js, Vue, Angular, Flutter, SwiftUI etc. — ou se o PRD descreve telas/jornadas de usuário final). Se sim, incluir `/designer` na lista de próximos passos, **antes** de `/techspec`.

Informar ao usuário:
- Caminho do PRD gerado
- Resultado da validação
- Próximo passo sugerido: `/clarify` (se houver questões em aberto) → `/designer` (se detectada interface visual) → `/techspec [feature] --system [sistema]`

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

**Nota:** dimensão R refinada em relação ao /discovery — RFs têm IDs formais e `N_VARIACOES` cenários Gherkin cada (parametrizado no início do Módulo B).

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
### [FEATURE_NAME]
- **Etapa concluída:** /prd (v1.0) — [data]
- **Artefato:** docs/prd/[feature]-prd.md
- **RFs Must Have:** RF-001, RF-002, ...
- **Questões em aberto:** [listar ou "nenhuma"]
- **Interface visual detectada:** [sim/não — se sim, recomendar /designer antes do /techspec]
- **Próximo comando:** /designer [feature] (se interface visual) ou /techspec [feature] --system [sistema]
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

## Argumentos recebidos

- (sem argumento) — gera tasks para a feature ativa em `memory/state.md`
- `"nome-da-feature"` — gera para uma feature específica
- `update` — modo revisão: lê o documento de tasks existente e pergunta o que adicionar/remover/repriorizar, preservando IDs já atribuídos

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
- **Gate `/analyze --pre-tasks`:** se `docs/design/[feature]/screen-map.md` existir (feature passou por `/designer`), `docs/analyze/[feature]-analysis.md` deve existir, ter sido gerado em modo `--pre-tasks` e ter veredicto ✅ ou ⚠️ (sem 🔴)
  - Se `docs/analyze/[feature]-analysis.md` não existir ou estiver desatualizado em relação ao PRD/TechSpec/screen-map atuais: **bloquear** e instruir o usuário a rodar `/analyze --pre-tasks` antes de prosseguir
  - Se existir com veredicto ❌ (algum 🔴 aberto): **bloquear** — o gap deve ser resolvido no PRD ou na TechSpec antes de gerar Tasks, não compensado depois em Tasks/BDD
  - Sem `screen-map.md` (feature sem UI/`/designer`): gate não se aplica; `/analyze` continua opcional como hoje

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
   - **Guia técnico:** arquivo a criar/modificar, padrão a seguir — **todo caminho de arquivo é relativo a `systems/[sistema]/`** (ex: `backend/src/main/...`, nunca `systems/[sistema]/backend/src/main/...` nem caminho a partir da raiz do workspace). `/implement` e `/tdd` resolvem `[sistema]` pelo campo Sistema da task e operam dentro desse diretório
   - **Critérios de aceite:** mensuráveis e verificáveis

2.3. **Gerar arquivo individual por task (obrigatório):** para cada Task, salvar também `docs/tasks/[feature]/TASK-[EPIC].[SEQ]-[slug].md` contendo o conteúdo completo da task (todos os campos da 2.2) de forma autocontida — é o arquivo que `/implement TASK-X.Y` consome diretamente, sem precisar abrir o documento consolidado. Não é opcional nem gerado apenas sob pedido do usuário.

2.4. Salvar após cada Epic completo (documento consolidado + arquivos individuais das tasks do Epic)

2.5. Gerar tabela de Sumário de Epics e o **Grafo de Dependências** no início, em ASCII (não apenas lista) — a representação visual explícita das cadeias de dependência, marcando `⚡` nas tasks do mesmo nível que podem rodar em paralelo:
```
TASK-1.1 (Setup BD)
  └── TASK-1.2 (Migration)
        └── TASK-2.1 (Repositório)
              ├── TASK-2.2 [P] (Use Case A)  ⚡ paralelo com TASK-2.3
              └── TASK-2.3 [P] (Use Case B)  ⚡ paralelo com TASK-2.2
```

2.6. **Se a feature afeta 2+ sistemas**, gerar a seção "Plano Git Multi-Sistema": branch da feature em cada repositório afetado (`feature/[nome]`, conforme o `git-workflow.md` de cada sistema) e a ordem de merge entre repositórios derivada das dependências (tabela: ordem / sistema / o que entrega / pré-requisito / compatibilidade retroativa). Em workspace de sistema único, omitir esta seção.

2.7. Gerar seção "Backlog Priorizado" ao final com ordem de início recomendada

2.8. Validação ao concluir:
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

### Fase 3.5 — Comitê de Análise Assíncrono (opcional)

Com as tasks salvas (documento consolidado + arquivos individuais), oferecer revisão cruzada antes de liberar para `/implement`:

> "As tasks foram geradas e salvas. Deseja que eu submeta o planejamento aos agents especialistas (Architect, QA) em background para revisão crítica? [Sim/Não]"

- **Se sim**: invocar `.agents/agents/architect.md` e `.agents/agents/qa.md` via ferramenta `Agent`, cada um lendo os arquivos recém-salvos em `docs/tasks/` (não colar conteúdo no prompt), apontando critérios de aceite vagos, tasks que misturam responsabilidades, ou dependências mal sequenciadas. Apresentar o feedback consolidado e perguntar se aceita atualizar os arquivos salvos.
- **Se não**: seguir para a Fase 3.6.

### Fase 3.6 — Opção: criar GitHub Issues

Perguntar: "Deseja que eu crie as tasks como GitHub Issues? Se sim, informe o repositório (`owner/repo`)."

Se confirmado, usar o CLI `gh` (`gh issue create` por task): labels a partir das tags da task (`backend`, `frontend`, `infra`, `test`); corpo com contexto, o que fazer e critérios de aceite; "Depende de #N" referenciando issues de dependência; agrupar por milestone se um sprint foi especificado.

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
- `docs/design/[feature]/screen-map.md` (se existir) + `docs/analyze/[feature]-analysis.md` gerado em `--pre-tasks` com veredicto ✅/⚠️ (gate — ver Pré-condições)

**Saída:**
- `docs/tasks/[feature]-tasks.md` — plano completo de tasks
- `docs/tasks/[feature]/TASK-[EPIC].[SEQ]-[slug].md` — um arquivo autocontido por task (obrigatório)
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

**Regra fundamental de localização — todo o ciclo acontece dentro de `systems/[sistema]/`:** testes, implementação e comandos git rodam nesse diretório, no repositório daquele sistema — nunca na raiz do workspace. Resolver `[sistema]` pelo campo `Sistema:` da task.

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

1.4. Executar os testes e classificar o resultado:

| Resultado | O que fazer |
|---|---|
| Todos falhando (RED confirmado) | Prosseguir para a Fase 2 |
| Alguns passando | Revisar — teste que passa sem implementação não testa nada real (mock com valor default, asserção vazia). Corrigir o teste antes de prosseguir |
| Erro de compilação/import (módulo não existe) | Normal em TDD estrito — criar os arquivos com stubs vazios (função que lança `NotImplementedError`/equivalente) apenas para viabilizar a execução dos testes, sem implementar lógica ainda |

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

**Atualizar status nos três locais que rastreiam a task:**
1. Marcar `Status: Concluída` no arquivo individual `docs/tasks/[feature]/TASK-[EPIC].[SEQ]-[slug].md`.
2. Marcar o checkbox correspondente no documento consolidado `docs/tasks/[feature]-tasks.md` (`[ ]` → `[x]`).
3. Atualizar `memory/state.md` com progresso da task (a cada task, não só na última do Epic).

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

### Fase 0.5 — Pesquisa de incertezas técnicas (condicional)

> Só executa quando existem incertezas técnicas reais. Se tudo já é conhecido pelos guidelines/PRD, informar "Nenhuma incerteza técnica identificada — prosseguindo para a Fase 1." e seguir.

1. Identificar incertezas que, se não resolvidas agora, geram decisões erradas na TechSpec: integração externa sem documentação clara, escolha de biblioteca com trade-off não óbvio, padrão de modelagem não coberto pelos guidelines, estratégia de auth para caso específico do PRD, comportamento de concorrência/consistência eventual.
2. Para cada incerteza: documentar o que é desconhecido, pesquisar (guidelines ou web), registrar a decisão tomada com justificativa.
3. **Se houver 2+ incertezas**: gerar `docs/techspec/[feature]-research.md` com, por incerteza — contexto (requisito do PRD que a origina), opções avaliadas com pros/contras, decisão, justificativa, impacto na TechSpec. Fechar com tabela de incertezas não resolvidas (questão / impacto / bloqueante?).
4. **Incertezas bloqueantes**: apresentar ao usuário e aguardar resposta antes de prosseguir. Não bloqueantes vão para a Seção 10 da TechSpec ("Questões em Aberto").

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

**Princípio de fonte única de verdade**: modelagem de dados e contratos de API são volumosos demais para viver dentro do documento principal — vivem em artefatos granulares que o TechSpec resume e referencia. Nunca gerar o mesmo conteúdo em dois lugares.

2.1. Criar `docs/techspec/[feature]-techspec.md` usando template techspec
   - Seção 1 (Visão Geral Técnica) → salvar
   - Seção 2 (Decisões Arquiteturais) com referências aos ADRs criados → salvar
   - Seção 3 (Modelo de Dados) — **resumo + link**; criar `docs/techspec/[feature]/data-model.md` como fonte de verdade (diagrama ER, entidades com campos/índices, ciclo de vida de estados, estratégia de migrations) → salvar ambos
   - Seção 4 (Contratos de API/Interface) — **índice de endpoints + link**; criar um arquivo por recurso/área funcional em `docs/techspec/[feature]/contracts/[recurso].md` (request, response, tabela de erros, RF atendido) → salvar cada um. Se a feature não tiver API, omitir e registrar "Nenhuma interface de API identificada — contratos não gerados."
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

2.3. **Gerar `docs/techspec/[feature]/quickstart.md` (obrigatório, não sob demanda)**: stack, estrutura de pastas, setup mínimo, cenários principais por RF (Dado/Quando/Então + exemplo executável), pontos de atenção e cenários de teste críticos. É o guia rápido que `/implement` e `/tdd` consultam antes de codificar.

2.4. Validação ao final:
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

### Fase 3.5 — Comitê de Análise Assíncrono (opcional)

Com a TechSpec e os artefatos granulares salvos, oferecer revisão cruzada:

> "A TechSpec foi gerada e salva. Deseja que eu submeta este planejamento aos agents especialistas (Architect, Security, Database, DevOps, QA) em background para revisão crítica antes de avançar para `/tasks`? [Sim/Não]"

- **Se sim**: invocar os agents relevantes via ferramenta `Agent`, cada um lendo os arquivos salvos em disco (não colar conteúdo no prompt), avaliando gargalos de performance, falhas de modelagem, riscos de segurança e testabilidade dos contratos. Apresentar feedback consolidado (1-3 pontos por agent) e perguntar se aceita atualizar os arquivos salvos. Se aceitar, aplicar e re-validar.
- **Se não**: seguir para a Fase 4.

### Fase 4 — Handoff

Atualizar `memory/state.md`:
- Artifact Registry: `docs/techspec/[feature]-techspec.md | 1.0 | ok`
- Se PRD foi mantido sem alteração: TechSpec status = `ok`
- Marcar feature como "Em especificação técnica → pronto para /tasks"

**Rede de segurança — detecção de interface visual:** se `docs/design/[feature]-design-brief.md` **não existir** e algum `systems/[sistema]/guidelines/stack.md` envolvido indicar framework de UI (React, Next.js, Vue, Angular, Flutter, SwiftUI etc.), alertar o usuário e recomendar rodar `/designer` antes do `/tasks` — mesmo que o `/prd` já devesse ter sugerido. Nunca assumir silenciosamente que a feature é backend-only.

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)
- `docs/prd/[feature]-prd.md` (obrigatório — deve estar `ok`)
- `systems/[sistema]/guidelines/*.md` (obrigatório — todos os 9)

**Saída:**
- `docs/techspec/[feature]-techspec.md` — TechSpec principal
- `docs/techspec/[feature]/data-model.md` — modelo de dados detalhado (fonte de verdade)
- `docs/techspec/[feature]/contracts/[recurso].md` — um arquivo por recurso de API (fonte de verdade), se aplicável
- `docs/techspec/[feature]/quickstart.md` — guia rápido de implementação (obrigatório)
- `docs/techspec/[feature]-research.md` — se 2+ incertezas técnicas foram resolvidas na Fase 0.5
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

### Fase 1.5 — Plano de testes (confirmação antes de gerar)

Antes de escrever qualquer arquivo de teste, apresentar o plano e aguardar confirmação:

```markdown
## Plano de Testes — [Task/RF]

### Escopo
- Tipo: [unitário/integração/e2e] | Ferramenta: [conforme testing.md] | Arquivo(s): [caminho esperado]

### Cenários
- Happy path: [N cenários]
- Casos de borda: [N cenários]
- Fluxos de erro: [N cenários]

### Dependências a mockar
- [dependência] — [motivo]
```

Perguntar: "O plano cobre o necessário ou há cenário adicional antes de gerar o código?" Só prosseguir para a Fase 2 após confirmação — evita gerar suítes grandes que precisam ser refeitas por escopo mal calibrado.

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

