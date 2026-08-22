---
name: implement
description: Executa uma task de implementação com precisão seguindo TechSpec, guidelines e canvas do projeto. Lê dimensões N (Norms) e S (Safeguards) do canvas como contexto. Use para implementar uma task específica do documento de tasks, produzindo código rastreável pronto para code review.
canvas-dimensions: []
input-artifacts:
  - memory/state.md
  - docs/tasks/{{FEATURE}}-tasks.md
  - docs/spdd/{{FEATURE}}-canvas.md
  - docs/techspec/{{FEATURE}}-techspec.md
output-artifacts: []
---

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
