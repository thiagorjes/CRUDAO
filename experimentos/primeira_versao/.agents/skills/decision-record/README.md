# Helper de Decision Records (DR)

Este diretório não é uma skill de pipeline — reúne o schema e as ferramentas
compartilhadas de criação de DRs, usadas pelas skills core (`guidelines`, `prd`,
`techspec` e demais) sempre que registram uma decisão (ADR/BDR/SDR/DDR).

## Como criar uma nova DR

1. **Determinar o tipo:** `ADR` (técnico), `BDR` (negócio), `SDR` (segurança) ou `DDR` (design) — ver ADR-012.
2. **Verificar o próximo NNN:** ler `memory/constitution.md`, seção `## Decision Records` → tabela do tipo escolhido. O NNN é o maior ID já listado + 1 (contadores independentes por tipo, começando em 001).
3. **Criar o arquivo:** copiar `.agents/templates/{{LANG}}/decision-record-template.md` para `docs/decisions/[TIPO]-[NNN]-[titulo-curto].md`, preenchendo os placeholders.
4. **Atualizar o índice:** adicionar uma linha na tabela do tipo correspondente em `memory/constitution.md`, com a coluna ID como link relativo: `[ADR-001](../docs/decisions/ADR-001-titulo-curto.md)`.
5. **Validar:**
   ```
   python .agents/scripts/validate.py --mode output \
     --rules .agents/skills/decision-record/validate-rules.json \
     --artifact docs/decisions/[TIPO]-[NNN]-[titulo-curto].md
   ```
   `python .agents/scripts/validate_skills.py .agents/skills` também verifica a integridade do índice (link quebrado → `AVISO`).

## Schema

Ver `validate-rules.json` — frontmatter obrigatório (`id`, `type`, `status`, `date`), 4 seções (`## Decisão`, `## Motivação`, `## Consequências`, `## Alternativas Consideradas`) e `status` restrito a `accepted | superseded | deprecated` (checado por `scripts/check_dr_status.py`).
