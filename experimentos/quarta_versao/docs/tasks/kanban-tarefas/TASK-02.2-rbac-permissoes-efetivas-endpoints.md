# TASK-02.2 — RBAC: motor de permissões efetivas + guard reutilizável

**Status:** Concluída — 2026-08-28

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-013 (suporte)
**Dependências:** TASK-02.1
**Paralelismo:** [P] com TASK-03.1, TASK-03.2, TASK-03.3 (após esta task concluída, essas três podem rodar em paralelo)

## Contexto

Motor central de autorização — todo endpoint de escrita das demais epics depende deste serviço para revalidação backend (RNF-003). Separada de TASK-02.3 (CRUD de papéis/permissões) por decisão do Comitê de Análise (Architect): era uma task única misturando 4 subsistemas e bloqueando 3 tasks paralelas (03.1/03.2/03.3) por escopo maior do que o estritamente necessário — aqui fica só o motor + guard, que é o que 03.x de fato precisa. **TDD obrigatório** (via `/tdd`), conforme `skill-conventions.md` — resolução de permissões é lógica de alto risco.

## O que deve ser feito

- [x] Implementar serviço de resolução de permissões efetivas do usuário por projeto (papel(is) + toggles `PapelPermissao` habilitados + `Usuario.ativo`).
- [x] Implementar checagem reutilizável (`@PreAuthorize` custom ou service guard) usada por todos os controllers de escrita subsequentes (03.x, 04.x, 05.x).

### Validação

- `mvn -q -Dtest=PermissaoServiceTest,PermissaoGuardTest,PermissaoGuardEndpointIT test` — não executado: Maven não conseguiu resolver o parent Spring Boot `3.5.16` porque o mirror Nexus configurado está indisponível (`nexus3-cicd-tools.cloud.sfb`).
- A análise de problemas do editor não encontrou erros nos cinco arquivos Java novos/alterados.

## Guia técnico

- `backend/src/main/java/.../rbac/` — serviço de permissões efetivas, guard reutilizável.
- `docs/techspec/kanban-tarefas/data-model.md` — seções Papel, PapelPermissao, UsuarioProjetoPapel.
- Usar `/tdd` para esta task (lógica de maior risco, TDD obrigatório conforme TechSpec Seção 7).

## Critérios de aceite

- Endpoint sensível sem permissão exigida retorna `403` (teste por endpoint, não só por regra — RNF-003).
- Toggle desabilitado bloqueia ação mesmo com papel normalmente permitido.
- `UsuarioProjetoPapel` existente reflete corretamente nas permissões efetivas.
- Usuário com `ativo=false` nunca passa na checagem, independentemente do vínculo.
