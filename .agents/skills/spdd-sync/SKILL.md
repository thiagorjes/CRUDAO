---
name: spdd-sync
description: Detecta divergências entre o REASONS Canvas e o código implementado e oferece resolução bidirecional (corrigir canvas ou reverter código), registrando cada desvio em deviations.md. Use após /implement ou /code-review quando o código evoluiu de forma diferente do que o canvas descreve.
canvas-dimensions: []
input-artifacts:
  - memory/state.md
  - docs/spdd/{{FEATURE}}-canvas.md
output-artifacts:
  - docs/spdd/{{FEATURE}}-deviations.md
---

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
