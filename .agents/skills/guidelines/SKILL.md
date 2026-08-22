---
name: guidelines
description: Conduz entrevista de stack e arquitetura para gerar os arquivos de guidelines do sistema (stack, architecture, coding-standards, testing, security, observability, git-workflow, skill-conventions, spdd-integration). Cria ADRs para decisões técnicas significativas. Use no início de cada novo sistema antes de /techspec.
canvas-dimensions: [N]
input-artifacts:
  - memory/state.md
output-artifacts:
  - systems/{{SYSTEM}}/guidelines/stack.md
  - systems/{{SYSTEM}}/guidelines/architecture.md
  - systems/{{SYSTEM}}/guidelines/coding-standards.md
  - systems/{{SYSTEM}}/guidelines/testing.md
  - systems/{{SYSTEM}}/guidelines/security.md
  - systems/{{SYSTEM}}/guidelines/observability.md
  - systems/{{SYSTEM}}/guidelines/git-workflow.md
  - systems/{{SYSTEM}}/guidelines/skill-conventions.md
  - systems/{{SYSTEM}}/guidelines/spdd-integration.md
---

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
