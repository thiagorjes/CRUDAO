# TechSpec — {{FEATURE_NAME}}
_Versão: 1.0 | Status: Draft | Data: {{DATE}} | Autor: {{AUTHOR}}_
_PRD: docs/prd/{{FEATURE_NAME}}-prd.md v{{PRD_VERSION}}_

---

## 1. Visão Geral Técnica

{{DESCRICAO_TECNICA_DA_FEATURE}}

**Sistemas afetados:** {{SISTEMAS_AFETADOS}}

**Abordagem:** {{ABORDAGEM_TECNICA}}

---

## 2. Decisões Arquiteturais

> Decisões: {{LISTA_ADR}}

| ADR | Decisão | Impacto |
|-----|---------|---------|
| ADR-{{NNN}} | {{DECISAO}} | {{IMPACTO}} |

---

## 3. Modelo de Dados

→ Documento completo: [data-model.md]({{FEATURE_NAME}}/data-model.md)

**Entidades principais:**

| Entidade | Atributos-chave | Relacionamentos |
|----------|----------------|----------------|
| {{ENTIDADE_1}} | {{ATRIBUTOS_1}} | {{RELACOES_1}} |

---

## 4. Contratos de API / Interface

→ Documentos completos: [contracts/]({{FEATURE_NAME}}/contracts/)

### {{ENDPOINT_OU_INTERFACE_1}}

**Tipo:** REST | gRPC | Event | File | CLI

**Contrato:**
- Entrada: {{DESCRICAO_ENTRADA}}
- Saída: {{DESCRICAO_SAIDA}}
- Erros: {{CODIGOS_DE_ERRO}}

---

## 5. Arquitetura e Fluxo

```
{{DIAGRAMA_ASCII_OU_MERMAID}}
```

**Fluxo principal:**
1. {{PASSO_1}}
2. {{PASSO_2}}
3. {{PASSO_3}}

---

## 6. Dependências Inter-Sistemas

| Sistema | Interface | Status | Mock? |
|---------|-----------|--------|-------|
| {{SISTEMA_1}} | {{INTERFACE_1}} | {{STATUS_1}} | Não |

---

## 7. Estratégia de Testes

| Tipo | Ferramenta | Cobertura alvo |
|------|-----------|----------------|
| Unitário | {{FERRAMENTA_UNIT}} | {{COBERTURA_UNIT}} |
| Integração | {{FERRAMENTA_INT}} | {{COBERTURA_INT}} |
| E2E | {{FERRAMENTA_E2E}} | Fluxos principais |

---

## 8. Segurança e Observabilidade

**Segurança:**
- {{CONSIDERACAO_SEGURANCA_1}}

**Observabilidade:**
- Logs: {{ESTRATEGIA_LOGS}}
- Métricas: {{METRICAS_CHAVE}}

---

## 9. Matriz de Rastreabilidade

| RF/RNF | Implementado em | Validado por |
|--------|----------------|-------------|
| RF-001 | {{ARQUIVO_IMPLEMENTACAO}} | {{METODO_VALIDACAO}} |
| RNF-001 | {{ARQUIVO_IMPLEMENTACAO}} | {{METODO_VALIDACAO}} |

---

## 10. Questões em Aberto

| # | Questão | Responsável | Prazo |
|---|---------|------------|-------|
| Q-001 | {{QUESTAO}} | {{RESPONSAVEL}} | {{PRAZO}} |

---

## 11. Histórico de Revisões

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | {{DATE}} | {{AUTHOR}} | Versão inicial |
