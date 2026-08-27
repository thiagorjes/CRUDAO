**Status:** Concluída — 2026-08-24

# TASK-04.2 — RBAC por projeto: retrabalho do modelo e enforcement [G]

**Epic:** EPIC-04 — RBAC e Autenticação | **User Story:** US-04.2 — Controle de acesso escopado por projeto
**Sistema:** CRUDAO | **RF:** RF-013, RF-015, RF-003 (autorização de Tarefa) | **Dependências:** TASK-04.1 (retrabalha), TASK-00.2

---

## Contexto

Retrabalho da [TASK-04.1](TASK-04.1-rbac-keycloak.md), motivado por um gap real encontrado ao implementar TASK-05.3: o modelo de papel único global por usuário não suportava "usuário com permissão restrita edita apenas o projeto de origem" — não havia como enforçar isso no backend (bypassável via chamada direta à API). PRD v1.3 introduziu RBAC escopado por projeto: papel `admin` continua global; os demais papéis (`project_admin`, `product_owner`, `dev`, `gestor`, `user` legado) são atribuídos por par (usuário, projeto), acumulando permissões. Ver [BDR-001](../../decisions/BDR-001-rbac-por-projeto.md) e [ADR-006](../../decisions/ADR-006-rbac-por-projeto-enforcement.md).

Esta task é pré-requisito de TASK-05.3, TASK-01.3 e TASK-02.3.

## O que deve ser feito

- [ ] Migração de schema: adicionar `Usuario.admin` (boolean), `Projeto.data_finalizacao` (timestamp nullable), criar tabelas `usuario_projeto_papel` (PK composta `usuario_id, projeto_id, papel_id` + índice `idx_upp_projeto (projeto_id)`) e `configuracao_projeto` (ver TASK-01.3 para uso). Não dropar `usuario.papel_id` na mesma migration da criação — script separado, depois da migração de dados
- [ ] Script de migração de dados (idempotente, com log): `papel.nome = 'admin'` → `Usuario.admin = true`, demais papéis → nenhuma linha gerada em `UsuarioProjetoPapel` (reatribuição manual pós-migração, RF-015 — não herdar escopo implícito, ambiente ainda em dev/homolog)
- [ ] Seed do catálogo de papéis padrão: `admin` (protegido, sem `UsuarioProjetoPapel`), `project_admin`/`product_owner`/`dev`/`gestor` com as permissões default da tabela do PRD (RF-013) — `project_admin` recebe todas as chaves exceto `papel:gerenciar`; `user` (legado) sem permissões
- [ ] Reescrever `UsuarioContexto`/auto-provisionamento: usuário sem role do Keycloak mapeada para papel configurado continua sem `admin` e sem `UsuarioProjetoPapel` (equivalente ao antigo `user`, RN-014)
- [ ] Implementar `AutorizacaoProjetoService.exigirPermissao(usuario, projetoId, permissao)`: (1) `Usuario.admin` → autorizado, sem consultar `UsuarioProjetoPapel` (RN-008 — papel `admin` é global, nunca atribuído via papel de projeto); (2) projeto com `data_finalizacao` preenchida e operação de escrita → 403/409 (RN-015, checagem única, não duplicada por Service); (3) agrega permissões de `UsuarioProjetoPapel` do usuário naquele projeto
- [ ] `papel:gerenciar` nunca atribuível via `UsuarioProjetoPapel` — checado só contra `Usuario.admin` em `PapelController` (fecha vetor de escalação, RN-006 superseded em parte)
- [ ] **Migrar TODOS os Services de CRUD escopado a projeto** de `@ExigePermissao` para chamada explícita a `AutorizacaoProjetoService.exigirPermissao`, resolvendo `projetoId` da entidade já carregada — inclui Workflow, Etapa, Transição, Raia **e Tarefa** (achado do `/analyze`, finding G1: os 7 endpoints de `TarefaController` — `criar`, `editar`, `excluir`, `mover`, `moverParaProjeto`, `marcarImpedimento`, `desmarcarImpedimento` — ficaram de fora do escopo original desta task e precisam da mesma migração)
- [ ] `PATCH /api/tarefas/{id}/mover-projeto` exige `tarefa:gerenciar` **nos dois projetos** (origem e destino) — duas chamadas a `exigirPermissao`, uma por projeto (achado do `/analyze`, finding G2: regra documentada em `data-model.md` desde v1.0, nunca migrada para o novo mecanismo — ver contrato explicitado na TechSpec v1.3)
- [ ] Seed de permissões: confirmar que `impedimento:marcar` não é concedida por padrão a `gestor` (RN-013) — coberto pelo seed padrão da tabela de papéis do PRD (RF-013), mas validar com teste explícito, já que RN-013 não tinha task associada até esta revisão
- [ ] Contrato explícito de `PapelController` (`GET/POST/PUT/DELETE /api/papeis`) — `@ExigePermissao("papel:gerenciar")` continua, mas `PermissaoAspect` passa a checar só `Usuario.admin` para essa chave
- [ ] Novo endpoint `GET /api/usuarios/me` — `{ id, nome, admin, projetos: [{ projetoId, papeis, permissoes }] }`, resolvendo `UsuarioProjetoPapel → Papel → Permissao` em uma única query (evitar N+1)
- [ ] Novos endpoints de associação (RF-015): `GET /api/projetos/{projetoId}/membros` (403 se o usuário não tem nenhum vínculo com o projeto) e `PUT /api/projetos/{projetoId}/membros/{usuarioId}` `{ papeis: [papelId...] }` — rejeita papel `admin` ou qualquer papel com `papel:gerenciar` (422); acessível por `admin` global (qualquer projeto) ou `project_admin` (só seu(s) projeto(s))
- [ ] Teste estrutural de CI (guardrail G-RBAC-06): verifica que todo método público de `@Service` de domínio que grava entidade com `projetoId` contém chamada a `AutorizacaoProjetoService.exigirPermissao`
- [ ] Nota de acompanhamento (não bloqueante, G-RT-01 do canvas): registrar como débito técnico a extensão futura de `StompAuthChannelInterceptor` para restringir subscription STOMP a membros do projeto, agora que `UsuarioProjetoPapel` existe

## Guia técnico

- Pacote: `security/`, `domain/rbac`, `domain/workflow`, `domain/raia`, `domain/tarefa` (migração de `@ExigePermissao`)
- Referência: [ADR-006](../../decisions/ADR-006-rbac-por-projeto-enforcement.md), [BDR-001](../../decisions/BDR-001-rbac-por-projeto.md), `docs/techspec/kanban-configuravel-techspec.md` (v1.3, contrato de `mover-projeto`), `docs/techspec/kanban-configuravel/data-model.md` (v1.2), `docs/techspec/kanban-configuravel/quickstart.md`

## Critérios de aceite

- Usuário com papel só no Projeto A recebe 403 ao agir no Projeto B; o mesmo usuário com papel no Projeto B é autorizado lá (teste de integração)
- Usuário com 2 papéis no mesmo projeto acumula as permissões de ambos
- Admin global autorizado em qualquer projeto, sem consultar `UsuarioProjetoPapel`
- `project_admin` não consegue conceder `admin` nem `papel:gerenciar` via `PUT /membros/{usuarioId}` (422)
- Projeto com `data_finalizacao` preenchida bloqueia toda escrita, inclusive para admin/project_admin (403/409)
- Teste estrutural de CI passa e falha propositalmente se a chamada de autorização for removida de um Service (validação do próprio mecanismo) — cobrindo também `TarefaService` (finding G1)
- Usuário com `tarefa:gerenciar` só no projeto de origem recebe 403 ao chamar `mover-projeto` para um destino onde não tem essa permissão, e vice-versa (finding G2)
- Seed de papéis não concede `impedimento:marcar` a `gestor` (RN-013, finding S2)

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
