# Quickstart — Kanban Configurável
_Versão: 1.0 | Data: 2026-08-23_
_Cobre principalmente a revisão de RBAC por projeto (PRD v1.2, BDR-001, ADR-006) — o restante do sistema já está implementado e documentado em `memory/state.md`._

---

## Stack

Backend: Java 25 + Spring Boot 3.5 (Lombok, MapStruct, Bean Validation, AOP), PostgreSQL, WebSocket/STOMP.
Frontend: Next.js 16, Vitest.
Auth: Keycloak (OIDC) — ver `docs/contracts/CRUDAO-keycloak-contract.md`.

## Estrutura de pastas relevante

```
systems/CRUDAO/backend/src/main/java/com/crudao/kanban/
  domain/rbac/          Usuario, Papel, Permissao, UsuarioProjetoPapel (novo), PapelService, ...
  domain/projeto/        Projeto (+ data_finalizacao), ConfiguracaoProjeto (novo)
  domain/tarefa/         Tarefa, AuditoriaTarefa (novo)
  security/               ExigePermissao, PermissaoAspect, AutorizacaoProjetoService (novo), UsuarioContexto
```

## Setup mínimo

Sem novidade de infra — `docker compose up -d --build` já sobe Keycloak, PostgreSQL, backend e frontend (ver TASK-00.1/00.2). A revisão de RBAC exige apenas migração de schema (nova tabela `usuario_projeto_papel`, `configuracao_projeto`, `auditoria_tarefa`, colunas `usuario.admin` e `projeto.data_finalizacao`) e um script de migração dos dados de `usuario.papel_id` existentes (Q-006).

## Cenários principais por RF (novos/revisados nesta versão)

### RF-013/RF-015 — RBAC por projeto

**Dado que** um usuário tem o papel `dev` no Projeto A e `project_admin` no Projeto B
**Quando** ele tenta `PUT /api/workflows/{id}` de um workflow do Projeto A
**Então** recebe 403 (não tem `workflow:gerenciar` no Projeto A); o mesmo request contra um workflow do Projeto B é permitido.

Exemplo de checagem no Service (substitui `@ExigePermissao` para ações escopadas):
```java
Usuario usuario = usuarioContexto.usuarioAtual();
Workflow workflow = buscarEntidade(id); // já resolve o projetoId
autorizacaoProjetoService.exigirPermissao(usuario, workflow.getProjetoId(), "workflow:gerenciar");
```

### RF-016 — Toggles por projeto

**Dado que** `ConfiguracaoProjeto.devPodeExcluirTarefa = false` (default) no Projeto A
**Quando** um `dev` tenta `DELETE /api/tarefas/{id}` de uma tarefa do Projeto A
**Então** recebe 403; se o `project_admin` do Projeto A ligar o toggle (`PUT /api/projetos/{id}/configuracao`), o mesmo `dev` passa a poder excluir.

### RF-017 — Auditoria de tarefa

**Dado que** um `product_owner` reatribui uma tarefa de PEDRO para JOAO
**Quando** a operação é confirmada
**Então** uma linha é gravada em `AuditoriaTarefa` (`campo=RESPONSAVEL`, `valorAnterior=PEDRO`, `valorNovo=JOAO`) na mesma transação da troca de responsável; `GET /api/tarefas/{id}/historico` reflete a mudança imediatamente.

### RF-012 (revisado) — `tarefa:finalizar`

**Dado que** uma tarefa está na última etapa não-final de um workflow, e o `dev` autenticado não tem a permissão `tarefa:finalizar`
**Quando** ele tenta mover a tarefa para a etapa marcada `etapaFinal=true`
**Então** recebe 403; o mesmo movimento por um `product_owner` (que tem a permissão) é aceito. "Desfinalizar" segue a mesma regra na volta.

### RF-008 (revisado) — Projeto finalizado

**Dado que** `Projeto.data_finalizacao` está preenchida
**Quando** qualquer usuário (inclusive admin/project_admin) tenta qualquer escrita nesse projeto (tarefa, workflow, etapa, raia, membro, toggle)
**Então** recebe 403/409; só `PUT`/`DELETE /api/projetos/{id}/finalizar` (reabrir) é aceito, e exige `projeto:gerenciar`.

## Pontos de atenção

- **`admin` nunca passa por `UsuarioProjetoPapel`** — checar `Usuario.admin` primeiro em `AutorizacaoProjetoService`, antes de qualquer consulta de papel por projeto (evita N+1 desnecessário para o caso mais comum de admin acessando tudo).
- **`projetoId` nunca vem do payload do cliente** para fins de autorização — sempre da entidade carregada pelo Service (RNF-003, ADR-006).
- **`project_admin` não pode conceder `admin`** — o endpoint de membros (`PUT /api/projetos/{id}/membros/{usuarioId}`) rejeita explicitamente o papel `admin` na lista.
- **`AuditoriaTarefa` é gravada na mesma transação** da alteração — nunca em job assíncrono separado (evita perder registro em falha parcial).
- Reaproveitar o padrão de "revalidar no ponto de uso" já usado no frontend (`return-to.ts`, TASK-05.0) — o gating de UI via `GET /api/usuarios/me` é só UX, nunca a fonte de autorização.

## Cenários de teste críticos

- Acúmulo de permissões: usuário com 2 papéis no mesmo projeto tem a união das permissões de ambos.
- Isolamento entre projetos: papel/permissão em um projeto não vaza para outro.
- `admin` global ignora todo escopo de projeto.
- Projeto finalizado bloqueia toda escrita, inclusive para quem tem `projeto:gerenciar`.
- Autoatribuição de tarefa já atribuída a outro dev, sem aprovação, com auditoria gravada.
