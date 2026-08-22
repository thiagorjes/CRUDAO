---
name: code-review
description: Realiza code review integrado contra guidelines do projeto, TechSpec e critérios de aceite da task, com análise de segurança obrigatória. Ao final, extrai guardrails para atualizar dimensão S (Safeguards) do canvas. Use após implementar uma task antes de mergar.
canvas-dimensions: [S]
input-artifacts:
  - memory/state.md
  - docs/tasks/{{FEATURE}}-tasks.md
  - docs/techspec/{{FEATURE}}-techspec.md
  - docs/spdd/{{FEATURE}}-canvas.md
output-artifacts:
  - docs/checklists/{{FEATURE}}-review.md
  - docs/spdd/{{FEATURE}}-canvas.md
---

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
