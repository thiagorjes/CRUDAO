---
name: tests
description: Gera e executa suíte de testes completa a partir dos critérios de aceite da task e da estratégia de testes da TechSpec, suportando modo TDD e modo audit. Use para criar cobertura de testes expressiva e alinhada às guidelines de testing do sistema.
canvas-dimensions: []
input-artifacts:
  - memory/state.md
  - docs/tasks/{{FEATURE}}-tasks.md
  - docs/techspec/{{FEATURE}}-techspec.md
output-artifacts: []
---

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
