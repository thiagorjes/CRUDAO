# Contrato — Papéis, Permissões e Usuários do Projeto

_RF-013, RF-015, RF-016, RN-006, RN-011, RN-012, RN-013, RN-CB-001, RN-CB-002_

Todos os endpoints requerem `papel:administrar` (exceto GETs de leitura, que requerem apenas vínculo ao projeto).

## GET /api/projetos/{projetoId}/papeis

`[{ "id", "chave", "nome", "protegido", "permissoes": [{ "chave", "habilitada" }] }]`

## POST /api/projetos/{projetoId}/papeis

Cria papel custom. `{ "chave": "string", "nome": "string" }`. `chave=admin` é reservada — `422` se tentado.

## PUT /api/papeis/{id}

Edita nome. `403` se `protegido=true` (papel `admin` — RN-006).

## DELETE /api/papeis/{id}

`403` se `protegido=true`. `409` se houver usuários vinculados ao papel.

## PUT /api/papeis/{id}/permissoes/{permissaoChave}

Toggle de permissão (RF-016). `{ "habilitada": "boolean" }`.

**RN-017 (novo — achado do Comitê de Análise, Security):** um usuário não pode alterar `PapelPermissao` de nenhum papel que ele próprio possui no projeto — previne autoconcessão de privilégio. `403` se o papel-alvo estiver entre os papéis do usuário autenticado no projeto; outro usuário com `papel:administrar` precisa executar a alteração. Toda alteração gera registro em `PapelPermissaoAuditoria` (autor, valor anterior/novo, data/hora).

Toggle especial `devPodeExcluirTarefa` é modelado como a permissão `tarefa:gerenciar` do papel `dev` combinada com uma flag de escopo — ver nota de implementação: representado como entrada `PapelPermissao` própria (`papel=dev`, `permissao=tarefa:gerenciar`, contexto=exclusão) para não exigir uma segunda dimensão de dados; detalhar decisão final na migration V2 se a modelagem 1:1 permissão↔ação se mostrar insuficiente (ver Seção 10 da TechSpec — Questão em Aberto).

## GET /api/projetos/{projetoId}/usuarios

Lista usuários associados ao projeto com seus papéis (RF-015, TL-10).

## POST /api/projetos/{projetoId}/usuarios

Associa usuário ao projeto com um papel (RF-015). `{ "usuarioId": "uuid", "papelId": "uuid" }`.

## DELETE /api/projetos/{projetoId}/usuarios/{usuarioId}

Remove associação (não exclui o `Usuario`, apenas o vínculo `UsuarioProjetoPapel`).

## GET /api/projetos/{projetoId}/usuarios/buscar?q= (novo — TASK-07.5)

Autocomplete de usuários **ainda não associados** ao projeto, para a UI de associação (RF-015). Mesma permissão de `POST /usuarios` (`papel:administrar`) — nunca uma listagem global de usuários do sistema (achado do Comitê de Análise, Architect+Security). `q` com menos de 3 caracteres retorna `[]` sem consultar o banco. Resultado limitado a 20, ordenado por nome, exclui usuários inativos (`ativo=false`) e já vinculados ao projeto.

`[{ "id": "uuid", "nome": "string", "email": "string" }]` — nunca expõe `keycloakSub`/`adminGlobal`/`ativo`.

## Erros

| Código | Situação |
|---|---|
| 403 | Sem `papel:administrar`; tentativa de editar/excluir papel `admin` (RN-006); tentativa de alterar permissão do próprio papel (RN-017) |
| 409 | Exclusão de papel com usuários vinculados |
| 422 | `chave=admin` em criação de papel |

RFs atendidos: RF-013, RF-015, RF-016.
