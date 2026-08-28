# Code Review — TASK-03.2: CRUD Workflow/Etapa/Transicao

**Data:** 2026-08-28  
**Sistema:** CRUDAO (`systems/CRUDAO/backend`)  
**Task:** `TASK-03.2-crud-workflow-etapa-transicao.md`  
**Status do Review:** APROVADO  

---

## Gate de Testes

- **Comando:** `docker run --rm -v ".../backend:/app" -w /app maven:3.9-eclipse-temurin-25 mvn test`
- **Resultado:** ✅ PASSED (30 testes executados, 0 falhas, 0 erros)

---

## Critérios de Aceite

| # | Critério de Aceite | Verificado? | Evidência |
|---|---------------------|-------------|-----------|
| 1 | Etapa não-final sem transição de saída configurada → erro `422` ao tentar salvar/operacionalizar | ✅ | `WorkflowService.java:136-138` (Lança `ResponseStatusException(422)`) |
| 2 | Reordenação de etapas persiste corretamente (`ordem`) | ✅ | `WorkflowService.java:97` (`etapa.setOrdem(...)`) e `EtapaRepository.java:7` (`findByWorkflowIdOrderByOrdemAsc`) |
| 3 | `UNIQUE(etapaOrigemId, etapaDestinoId)` respeitado em Transicao | ✅ | `V3__workflow_etapa_transicao.sql:18` (`CONSTRAINT uk_transicao_origem_destino UNIQUE`) |
| 4 | Stub de RN-005 responde "sem tarefas ativas" | ✅ | `WorkflowService.java:176, 181` (`temTarefasAtivas...` retornando `false`) |

---

## 🔴 Crítico

Nenhum.

---

## 🟡 Importante

Nenhum.

---

## 🔵 Sugestão

1. **Validação de pertencimento de etapas em transição**: No método `atualizarTransicoes`, considerar validar se a `etapaDestino` pertence ao mesmo `workflow` da `etapaOrigem`. Atualmente qualquer etapa existente no sistema é aceita se o ID for válido.
   - Arquivo: `WorkflowService.java:143`
   - Não bloqueia o merge desta task pois o contrato básico está atendido.

---

## ✅ Pontos Positivos

- Cobertura completa de migração Flyway V3 com chaves estrangeiras, deleção em cascata e índices únicos.
- Uso consistente do `permissaoGuard.exigir(projetoId, "workflow:administrar")` e `permissaoGuard.exigirProjetoAtivo(projetoId)` em todas as operações de workflow/etapas.
- Suíte TDD isolada com Mockito cobrindo regras de negócio (RN-003, RN-005 stub e retornos de DTO).

---

## Segurança

- **Input Validation:** Aplicada via `@Valid` nos controllers e `@NotBlank`/`@NotNull` nos DTOs de requisição.
- **Autorização:** Verificação explícita do status do projeto (ativo) e da permissão `workflow:administrar` antes de qualquer alteração de estado.
- **Injeção de Código/SQL:** Utilização exclusiva do Spring Data JPA / Hibernate com parâmetros sanitizados.

---

## Conformidade com TechSpec

- Mapeamento de `Workflow`, `Etapa` e `Transicao` idêntico à especificação em `docs/techspec/kanban-tarefas/data-model.md`.
- Contratos REST e códigos de resposta HTTP alinhados a `docs/techspec/kanban-tarefas/contracts/workflows.md`.

---

## Resultado

**APROVADO**

