# Checklist de Qualidade — {{FEATURE_NAME}} ({{TIPO_ARTEFATO}})
_Data: {{DATE}} | Revisado por: {{AUTHOR}}_
_Artefato: docs/{{TIPO_ARTEFATO}}/{{FEATURE_NAME}}-{{TIPO_ARTEFATO}}.md_

---

## Instruções

- ✅ = Atende completamente
- ⚠️ = Atende parcialmente (detalhes em observações)
- ❌ = Não atende (bloqueante para próxima etapa)
- N/A = Não se aplica a esta feature

**Itens marcados com 🔴 são bloqueantes — impedem avançar para a próxima etapa do pipeline.**

---

## Seção 1 — Clareza e Completude

| # | Critério | Status | Observações |
|---|---------|--------|-------------|
| 1.1 | 🔴 Todos os requisitos têm ID único (RF-NNN / RNF-NNN) | {{STATUS}} | {{OBS}} |
| 1.2 | 🔴 Cada RF tem critérios de aceite Gherkin (Dado/Quando/Então) | {{STATUS}} | {{OBS}} |
| 1.3 | 🔴 RNFs têm métricas mensuráveis (não "ser rápido", mas "< 200ms p95") | {{STATUS}} | {{OBS}} |
| 1.4 | Stakeholders identificados com responsabilidades claras | {{STATUS}} | {{OBS}} |
| 1.5 | Público-alvo definido com personas | {{STATUS}} | {{OBS}} |

---

## Seção 2 — Consistência Interna

| # | Critério | Status | Observações |
|---|---------|--------|-------------|
| 2.1 | 🔴 Sem contradições entre requisitos | {{STATUS}} | {{OBS}} |
| 2.2 | Casos de uso cobrem todos os RFs | {{STATUS}} | {{OBS}} |
| 2.3 | Regras de negócio referenciadas pelos RFs que as aplicam | {{STATUS}} | {{OBS}} |
| 2.4 | Prioridades (Must/Should/Could/Won't) atribuídas a todos os RFs | {{STATUS}} | {{OBS}} |

---

## Seção 3 — Escopo e Rastreabilidade

| # | Critério | Status | Observações |
|---|---------|--------|-------------|
| 3.1 | 🔴 Seção "Fora do Escopo" explícita | {{STATUS}} | {{OBS}} |
| 3.2 | Dependências externas listadas | {{STATUS}} | {{OBS}} |
| 3.3 | KPIs de sucesso definidos e mensuráveis | {{STATUS}} | {{OBS}} |
| 3.4 | Restrições técnicas ou de negócio documentadas | {{STATUS}} | {{OBS}} |

---

## Seção 4 — Prontidão para Próxima Etapa

| # | Critério | Status | Observações |
|---|---------|--------|-------------|
| 4.1 | 🔴 Artefato aprovado pelo Product Owner | {{STATUS}} | {{OBS}} |
| 4.2 | Questões em aberto resolvidas ou documentadas | {{STATUS}} | {{OBS}} |
| 4.3 | Versão e data atualizadas no cabeçalho | {{STATUS}} | {{OBS}} |
| 4.4 | Nenhum placeholder {{SCREAMING_SNAKE_CASE}} remanescente | {{STATUS}} | {{OBS}} |

---

## Resultado

| Bloqueantes (🔴) | Melhorias (⚠️) | Aprovados (✅) |
|-----------------|--------------|--------------|
| {{N_BLOQUEANTES}} | {{N_MELHORIAS}} | {{N_APROVADOS}} |

**Decisão:** ✅ Aprovado para próxima etapa | ⚠️ Aprovado com ressalvas | ❌ Reprovado — corrigir bloqueantes

**Observações gerais:**
{{OBSERVACOES_GERAIS}}
