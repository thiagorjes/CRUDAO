# Contrato MOCK — {{SISTEMA_A}} ↔ {{SISTEMA_B}}
_Versão: 0.1 | Status: **PENDENTE DE VALIDAÇÃO** | Data: {{DATE}}_

> ⚠️ **Este é um contrato mock gerado automaticamente pelo /techspec.**
> Deve ser substituído pelo contrato real antes da integração em produção.
> Task de substituição: **{{TASK_ID_SUBSTITUICAO}}**

---

## Identificação

| Campo | Valor |
|-------|-------|
| Interface | {{NOME_DA_INTERFACE}} |
| Direção | {{SISTEMA_A}} → {{SISTEMA_B}} |
| Protocolo | {{PROTOCOLO}} |
| Responsável (estimado) | {{SISTEMA_RESPONSAVEL}} |
| Versão mock | 0.1 |

---

## Descrição (estimada)

{{DESCRICAO_ESTIMADA_DA_INTERFACE}}

---

## Contrato de Dados (ESTIMADO — não validado com o sistema {{SISTEMA_B}})

### Entrada estimada

```
{{SCHEMA_ENTRADA_ESTIMADO}}
```

### Saída estimada

```
{{SCHEMA_SAIDA_ESTIMADO}}
```

---

## Erros conhecidos (estimados)

| Código | Significado estimado |
|--------|---------------------|
| {{CODIGO_1}} | {{SIGNIFICADO_ESTIMADO_1}} |

---

## Checklist de validação (a fazer antes de remover este aviso)

- [ ] Contrato validado com responsável do sistema {{SISTEMA_B}}
- [ ] Schema de entrada/saída confirmado
- [ ] SLAs definidos
- [ ] Estratégia de retry acordada
- [ ] Arquivo renomeado para `contracts/{{SISTEMA_A}}-{{SISTEMA_B}}-contract.md`
- [ ] Status atualizado de `PENDENTE DE VALIDAÇÃO` para `ok`

---

## Histórico

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 0.1 | {{DATE}} | /techspec (gerado) | Mock inicial — PENDENTE DE VALIDAÇÃO |
