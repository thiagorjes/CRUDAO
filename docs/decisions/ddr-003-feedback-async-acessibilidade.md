---
id: DDR-003
type: DDR
status: accepted
date: 2026-08-22
supersedes: —
superseded-by: —
---

# DDR-003 — Padrões de feedback, loading assíncrono e nível de acessibilidade

## Decisão

Feedback de erro/atenção usa modal de confirmação; feedback de sucesso/baixa relevância usa toast/snackbar. Loading de operações longas (ex.: job assíncrono do dashboard, ADR-005) usa skeleton screen; operações síncronas rápidas usam spinner simples como fallback. Acessibilidade formal (WCAG AA, foco visível, tamanho mínimo de toque) não é obrigatória nesta versão — sistema interno/validação de conceito.

## Motivação

Diferenciar a severidade da interrupção pelo tipo de feedback evita fadiga de modais para eventos triviais, e reserva a atenção do usuário para erros reais. O skeleton screen comunica melhor tempo de espera variável do job assíncrono do dashboard do que um spinner indefinido.

**Problema que resolve:**
Definir um padrão único e prático de feedback visual e loading antes da implementação, evitando inconsistência entre telas.

**Restrições consideradas:**
- ADR-005: dashboard calculado de forma assíncrona, com resultado entregue via WebSocket/polling — precisa de um padrão de loading que comunique espera variável.
- Sistema de uso interno, sem requisito de compliance de acessibilidade (confirmado na entrevista).

## Consequências

**Positivas:**
- Hierarquia clara de feedback (modal = atenção obrigatória; toast = informativo).
- Skeleton screen melhora percepção de performance em operações longas.

**Negativas / trade-offs:**
- Acessibilidade não tratada nesta fase é uma dívida técnica caso o sistema seja usado por público mais amplo no futuro — revisar se o escopo mudar.

**Downstream afetado:**
- Frontend: componentes de Modal, Toast/Snackbar, Skeleton e Spinner.

## Alternativas Consideradas

### Alternativa 1 — Toast único para todo tipo de feedback
**Descartada porque:** usuário quer maior destaque (modal) para situações que exigem atenção, como erros.

### Alternativa 2 — Acessibilidade WCAG AA obrigatória desde já
**Descartada porque:** usuário classificou como não obrigatório nesta fase (sistema interno/POC).
