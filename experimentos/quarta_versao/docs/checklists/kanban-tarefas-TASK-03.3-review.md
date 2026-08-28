# Code Review — TASK-03.3: CRUD Raia (Swimlanes)

**Data:** 2026-08-28  
**Sistema:** CRUDAO (`systems/CRUDAO/backend`)  
**Task:** `TASK-03.3-crud-raia.md`  
**Status do Review:** APROVADO  

---

## Gate de Testes

- **Comando:** `docker run --rm -v ".../backend:/app" -w /app maven:3.9-eclipse-temurin-25 mvn test`
- **Resultado:** ✅ PASSED (35 testes executados, 0 falhas, 0 erros)

---

## Critérios de Aceite

| # | Critério de Aceite | Verificado? | Evidência |
|---|---------------------|-------------|-----------|
| 1 | Raia default global existe após seed e é usada quando o projeto não tem raia própria (RN-CB-005) | ✅ | `V4__raia.sql:10` (seed global) e `RaiaService.java:31-36` (fallback para `global: true`) |
| 2 | Contrato REST de raias respeitado (id, nome, ordem, global) | ✅ | `RaiaResponse.java` e `RaiaController.java` |
| 3 | Stub de RN-005 responde "sem tarefas ativas" | ✅ | `RaiaService.java:97` (`temTarefasAtivasNaRaia` retornando `false`) |

---

## 🔴 Crítico

Nenhum.

---

## 🟡 Importante

Nenhum.

---

## 🔵 Sugestão

1. **Reordenação de Raias**: Ao adicionar um parâmetro `ordem` via `CriarRaiaRequest` ou `AtualizarRaiaRequest`, considerar validar se a `ordem` é um valor maior que zero.
   - Arquivo: `CriarRaiaRequest.java:14` / `AtualizarRaiaRequest.java:14`
   - Não bloqueia o merge desta task.

---

## ✅ Pontos Positivos

- Implementação limpa da lógica de fallback para a raia default global (`projeto_id = NULL`).
- Proteção contra modificação ou deleção inadvertida da raia global por chamadas administrativas escopadas por projeto (lançando HTTP 403).
- Validação adequada de permissões via `permissaoGuard.membro` para leitura e `permissaoGuard.exigir` + `exigirProjetoAtivo` para mutações.

---

## Segurança

- **Input Validation:** Validação de payload via `@Valid` e Bean Validation (`@NotBlank`, `@NotNull`).
- **Autorização:** Verificação rigorosa do estado do projeto e da permissão `workflow:administrar`.
- **Proteção de Dados Globais:** Impedimento explícito de alteração da raia global (`raia.getProjeto() == null`).

---

## Conformidade com TechSpec

- Estrutura de dados totalmente compatível com `docs/techspec/kanban-tarefas/data-model.md` e contrato REST `docs/techspec/kanban-tarefas/contracts/raias.md`.

---

## Resultado

**APROVADO**

