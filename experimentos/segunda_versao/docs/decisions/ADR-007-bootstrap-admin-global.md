# ADR-007 — Bootstrap do primeiro admin via flag `adminGlobal` + e-mail configurado

_Status: Aceito | Data: 2026-08-25 | Feature: kanban-tarefas_

## Contexto

`POST /api/projetos` exige `projeto:administrar`, mas essa permissão é resolvida por
`UsuarioProjetoPapel` — associação escopada a um `Projeto` já existente (`projeto_id NOT NULL`,
V2). No momento da criação do primeiro projeto, nenhum projeto existe ainda para escopar a
checagem, e o papel `admin` global (`projeto_id NULL`, protegido, RN-006) não pode ser vinculado a
nenhum usuário via essa tabela — a coluna é `NOT NULL`, incompatível com um vínculo "sem projeto".
Faltava um caminho para autorizar quem cria o primeiro projeto e configura os papéis/usuários
iniciais.

## Decisão

Adicionar `Usuario.adminGlobal` (boolean, default `false`, migration V9). O primeiro login (JIT
provisioning, `UsuarioProvisioningService`) cujo e-mail bate com a property
`kanban.bootstrap.admin-email` marca esse usuário como `adminGlobal=true`; o flag não é
reaplicado em logins seguintes. Em ambiente dev, a property aponta para `admin.teste@crudao.local`
— já cadastrado no realm Keycloak (`keycloak/realm-export.json`, TASK-01.1); em produção deve ser
setada via `KANBAN_BOOTSTRAP_ADMIN_EMAIL`.

`PermissaoGuard.permitido`/`membro` retornam `true` incondicionalmente para `adminGlobal=true` —
bypass universal de RBAC escopado, inclusive para operações fora de qualquer projeto (`POST
/api/projetos`, checado diretamente em `ProjetoService.criar`). RN-015 (projeto finalizado é
somente leitura) continua valendo para o admin global — `exigirProjetoAtivo` não tem bypass.

## Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **`adminGlobal` + bootstrap por e-mail (escolhida)** | Sem seed SQL de `usuario` (o `keycloak_sub` real só existe após login OIDC); reaproveita o fluxo JIT já existente; simples de auditar | Novo conceito de autorização (flag) paralelo ao RBAC via papel — dois mecanismos de "é admin" no sistema |
| Tornar `usuario_projeto_papel.projeto_id` nullable | Reaproveita 100% o modelo de papel/permissão existente para o admin global | Coluna nullable quebra a PK composta (Postgres não aceita NULL em coluna de PK); exigiria redesenhar a chave/constraints da tabela só para este caso |
| Seed SQL do usuário admin na migration | Não depende de configuração de aplicação | `keycloak_sub` do admin não é previsível antes do primeiro login real — o registro semeado nunca bateria com o usuário que de fato loga |

## Consequências

- Nova property obrigatória em produção: `KANBAN_BOOTSTRAP_ADMIN_EMAIL` — sem ela, ninguém consegue
  criar o primeiro projeto nem configurar papéis/usuários (documentar no runbook de deploy).
- `adminGlobal` nunca é setado por nenhum endpoint de escrita (sem `PUT /api/usuarios/{id}` que o
  exponha) — só via bootstrap no provisioning. Promover outro usuário exige alteração direta no
  banco (aceito como decisão operacional deliberada, fora do escopo desta feature).
- `PermissaoGuard` passa a depender de `ProjetoRepository` (antes dependia só de
  `PermissaoService`) para resolver `exigirProjetoAtivo`.

## Referências

RF-008, RN-015, [projetos.md](../techspec/kanban-tarefas/contracts/projetos.md), TASK-03.1.
