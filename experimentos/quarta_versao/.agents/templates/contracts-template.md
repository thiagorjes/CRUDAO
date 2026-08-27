# Contrato de Integração — {{SISTEMA_A}} ↔ {{SISTEMA_B}}
_Versão: 1.0 | Status: ok | Data: {{DATE}} | Responsável: {{RESPONSAVEL}}_

---

## Identificação

| Campo | Valor |
|-------|-------|
| Interface | {{NOME_DA_INTERFACE}} |
| Direção | {{SISTEMA_A}} → {{SISTEMA_B}} |
| Protocolo | REST \| gRPC \| Event \| File \| CLI |
| Responsável | {{SISTEMA_RESPONSAVEL}} |
| Versão | 1.0 |

---

## Descrição

{{DESCRICAO_DA_INTERFACE}}

---

## Contrato de Dados

### Entrada

```
{{SCHEMA_ENTRADA}}
```

**Campos obrigatórios:**
| Campo | Tipo | Descrição | Validação |
|-------|------|-----------|-----------|
| {{CAMPO_1}} | {{TIPO_1}} | {{DESCRICAO_1}} | {{VALIDACAO_1}} |

### Saída

```
{{SCHEMA_SAIDA}}
```

**Campos de resposta:**
| Campo | Tipo | Descrição | Quando presente |
|-------|------|-----------|----------------|
| {{CAMPO_1}} | {{TIPO_1}} | {{DESCRICAO_1}} | Sempre |

---

## Erros e Códigos

| Código | Significado | Ação esperada |
|--------|------------|---------------|
| {{CODIGO_1}} | {{SIGNIFICADO_1}} | {{ACAO_1}} |

---

## SLAs

- **Latência p95:** {{LATENCIA_P95}}
- **Disponibilidade:** {{DISPONIBILIDADE}}
- **Retry policy:** {{POLITICA_RETRY}}

---

## Exemplos

### Requisição de exemplo

```
{{EXEMPLO_REQUISICAO}}
```

### Resposta de exemplo

```
{{EXEMPLO_RESPOSTA}}
```

---

## Histórico de Revisões

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | {{DATE}} | {{RESPONSAVEL}} | Versão inicial |
