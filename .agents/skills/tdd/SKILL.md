---
name: tdd
description: Executa o ciclo TDD completo (Red → Green → Refactor → Review integrado) para implementar uma task com cobertura de testes desde o início. Lê dimensões N e S do canvas como contexto. Use como alternativa ao /implement quando testes são obrigatórios ou quando a lógica é complexa o suficiente para TDD ser mais seguro.
canvas-dimensions: []
input-artifacts:
  - memory/state.md
  - docs/tasks/{{FEATURE}}-tasks.md
  - docs/spdd/{{FEATURE}}-canvas.md
  - docs/techspec/{{FEATURE}}-techspec.md
output-artifacts: []
---

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
