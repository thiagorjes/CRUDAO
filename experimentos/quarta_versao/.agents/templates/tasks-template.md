# Tasks — {{FEATURE_NAME}}
_Versão: 1.0 | Data: {{DATE}} | Autor: {{AUTHOR}}_
_PRD: docs/prd/{{FEATURE_NAME}}-prd.md v{{PRD_VERSION}}_
_TechSpec: docs/techspec/{{FEATURE_NAME}}-techspec.md v{{TECHSPEC_VERSION}}_

---

## Sumário de Epics

| ID | Epic | Tasks | Estimativa total | Pode iniciar |
|----|------|-------|-----------------|-------------|
| EPIC-01 | {{NOME_EPIC_01}} | {{N_TASKS}} | {{ESTIMATIVA}} | Imediatamente |

**Legenda:** P ≤ 4h | M 4–8h | G 1–2 dias

---

## Grafo de Dependências

```
EPIC-01
  └── TASK-01.1 → TASK-01.2
```

---

## EPIC-01 — {{NOME_EPIC_01}}

### US-01.1 — {{NOME_USER_STORY}}

#### TASK-01.1 — {{TITULO_TASK}} [P|M|G]
**Sistema:** {{SISTEMA}} | **RF:** {{RF_ORIGEM}} | **Dependências:** nenhuma

**Contexto:**
{{CONTEXTO_DA_TASK}}

**O que deve ser feito:**
- [ ] {{ACAO_1}}
- [ ] {{ACAO_2}}

**Guia técnico:**
- Arquivo: `{{CAMINHO_ARQUIVO}}`
- {{DETALHE_TECNICO_1}}

**Critérios de aceite:**
- {{CRITERIO_1}}
- {{CRITERIO_2}}

---

#### TASK-01.2 — {{TITULO_TASK}} [P|M|G]
**Sistema:** {{SISTEMA}} | **RF:** {{RF_ORIGEM}} | **Dependências:** TASK-01.1 | **[P] com TASK-01.3**

**Contexto:**
{{CONTEXTO_DA_TASK}}

**O que deve ser feito:**
- [ ] {{ACAO_1}}
- [ ] {{ACAO_2}}

**Critérios de aceite:**
- {{CRITERIO_1}}

---

## Backlog Priorizado (Ordem de Início)

| Prioridade | Task | Motivo |
|-----------|------|--------|
| 1 | TASK-01.1 | {{MOTIVO_PRIORIDADE}} |
| 2 | TASK-01.2 | Depende de TASK-01.1 |

## Fora do Escopo (Backlog Futuro)

- {{ITEM_FORA_ESCOPO_1}}
