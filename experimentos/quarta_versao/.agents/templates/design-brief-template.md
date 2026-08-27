# Design Brief — {{FEATURE_NAME}}
_Versão: 1.0 | Data: {{DATE}} | Designer: {{AUTHOR}}_
_PRD: docs/prd/{{FEATURE_NAME}}-prd.md_

---

## 1. Contexto e Objetivo

**Feature:** {{FEATURE_NAME}}
**Objetivo de UX:** {{OBJETIVO_UX}}
**Público-alvo:** {{PUBLICO_ALVO}}

---

## 2. Tokens de Cor

| Token | Valor | Uso |
|-------|-------|-----|
| `--color-primary` | `{{COR_PRIMARIA}}` | Ações principais, CTAs |
| `--color-secondary` | `{{COR_SECUNDARIA}}` | Ações secundárias |
| `--color-background` | `{{COR_FUNDO}}` | Fundo de tela |
| `--color-surface` | `{{COR_SUPERFICIE}}` | Cards, modais |
| `--color-error` | `{{COR_ERRO}}` | Estados de erro |
| `--color-success` | `{{COR_SUCESSO}}` | Estados de sucesso |
| `--color-text-primary` | `{{COR_TEXTO_PRINCIPAL}}` | Texto principal |
| `--color-text-secondary` | `{{COR_TEXTO_SECUNDARIO}}` | Texto auxiliar |

---

## 3. Tipografia

| Token | Fonte | Peso | Tamanho | Uso |
|-------|-------|------|---------|-----|
| `--font-heading-1` | {{FONTE_HEADING}} | Bold (700) | {{TAMANHO_H1}} | Títulos de página |
| `--font-heading-2` | {{FONTE_HEADING}} | SemiBold (600) | {{TAMANHO_H2}} | Subtítulos de seção |
| `--font-body` | {{FONTE_BODY}} | Regular (400) | {{TAMANHO_BODY}} | Texto corrido |
| `--font-caption` | {{FONTE_BODY}} | Regular (400) | {{TAMANHO_CAPTION}} | Legendas, metadados |
| `--font-code` | {{FONTE_MONO}} | Regular (400) | {{TAMANHO_CODE}} | Código inline |

---

## 4. Espaçamento e Grid

- **Grid:** {{TIPO_GRID}} (ex: 12 colunas, gutter {{GUTTER}})
- **Breakpoints:** Mobile {{BREAKPOINT_MOBILE}} | Tablet {{BREAKPOINT_TABLET}} | Desktop {{BREAKPOINT_DESKTOP}}
- **Escala de espaçamento:** base {{BASE_ESPACAMENTO}} (4px ou 8px recomendado)

| Token | Valor | Uso |
|-------|-------|-----|
| `--spacing-xs` | {{SPACING_XS}} | Elementos internos compactos |
| `--spacing-sm` | {{SPACING_SM}} | Padding de componentes |
| `--spacing-md` | {{SPACING_MD}} | Espaçamento entre componentes |
| `--spacing-lg` | {{SPACING_LG}} | Seções da página |
| `--spacing-xl` | {{SPACING_XL}} | Margens de layout |

---

## 5. Componentes

### {{COMPONENTE_1}}

**Quando usar:** {{QUANDO_USAR_1}}

**Variantes:**
- {{VARIANTE_1}}: {{DESCRICAO_VARIANTE_1}}
- {{VARIANTE_2}}: {{DESCRICAO_VARIANTE_2}}

**Estados:** default | hover | active | disabled | loading | error

**Exemplo:**
```
{{EXEMPLO_COMPONENTE_1}}
```

### {{COMPONENTE_2}}

**Quando usar:** {{QUANDO_USAR_2}}

**Variantes:**
- {{VARIANTE_1}}: {{DESCRICAO_VARIANTE_1}}

---

## 6. Padrões de Interação

| Padrão | Descrição | Animação |
|--------|-----------|----------|
| {{PADRAO_1}} | {{DESCRICAO_PADRAO_1}} | {{ANIMACAO_1}} |
| {{PADRAO_2}} | {{DESCRICAO_PADRAO_2}} | {{ANIMACAO_2}} |

**Feedback visual:**
- Sucesso: {{FEEDBACK_SUCESSO}}
- Erro: {{FEEDBACK_ERRO}}
- Loading: {{FEEDBACK_LOADING}}

---

## 7. Acessibilidade

- **Contraste mínimo:** WCAG AA (4.5:1 texto normal, 3:1 texto grande)
- **Foco visível:** {{ESTILO_FOCO}}
- **Leitores de tela:** {{CONSIDERACOES_ARIA}}
- **Tamanho mínimo de toque:** 44×44px (mobile)

---

## 8. Decision Records de Design (DDR)

| DDR | Decisão |
|-----|---------|
| DDR-{{NNN}} | {{DECISAO_DESIGN_1}} |

---

## 9. Referências

- Design System base: {{LINK_DESIGN_SYSTEM}}
- Figma/Protótipo: {{LINK_PROTOTIPO}}
- Inspirações: {{REFERENCIAS_VISUAIS}}
