# Skill Conventions — CRUDAO
_Versão: 1.0 | Data: 2026-08-22_

## Pipeline SSPDD aplicado ao sistema CRUDAO

```
/guidelines → /prd → [/clarify] → [/checklist] → /techspec → /spdd-canvas → /tasks → [/analyze] → /implement → [/spdd-sync] → /code-review
```

## Convenções específicas

- Features deste sistema seguem o padrão de nomenclatura kebab-case (ex.: `kanban-configuravel`).
- TDD/BDD obrigatórios sempre que aplicáveis (ver `testing.md`) — usar `/tdd` como alternativa ao `/implement` para lógica complexa (ex.: cálculo de lead-time, engine de transições de workflow).
- Toda decisão técnica com trade-off relevante gera ADR, registrado em `memory/constitution.md`.
- Idioma de todos os artefatos: pt_BR.
