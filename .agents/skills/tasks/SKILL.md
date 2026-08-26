---
name: tasks
description: Transforma PRD e TechSpec em tasks de implementação completas e auto-contidas, ordenadas por dependência, com oportunidades de paralelismo identificadas. Atualiza dimensão O do canvas. Use após aprovar a TechSpec.
canvas-dimensions: [O]
input-artifacts:
  - memory/state.md
  - docs/prd/{{FEATURE}}-prd.md
  - docs/techspec/{{FEATURE}}-techspec.md
  - docs/design/{{FEATURE}}/screen-map.md
  - docs/analyze/{{FEATURE}}-analysis.md
output-artifacts:
  - docs/tasks/{{FEATURE}}-tasks.md
  - docs/tasks/{{FEATURE}}/TASK-*.md
  - docs/spdd/{{FEATURE}}-canvas.md
---

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
