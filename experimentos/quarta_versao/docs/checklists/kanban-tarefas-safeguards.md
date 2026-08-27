# Inventario de Safeguards — Kanban de Tarefas

_Versao: 1.0 | Status: Pendente de validacao por `/code-review` | Data: 2026-08-27_

> Este inventario consolida restricoes encontradas nos guidelines, PRD, TechSpec, data model, contratos e tasks. Ele define o que a implementacao deve proteger; nao prova que o codigo ja cumpre essas regras.

## Fonte e classificacao

| Categoria | Fontes |
|---|---|
| Seguranca e autorizacao | `guidelines/security.md`, RNF-003, ADR-003, ADR-006, ADR-007, contratos |
| Integridade de dominio | PRD, `data-model.md`, regras RN-001 a RN-016 e RN-CB |
| Operacao e resiliencia | RNF-001, RNF-002, RNF-004, ADR-004, ADR-005, ADR-008 |
| Qualidade e testes | `guidelines/testing.md`, `skill-conventions.md`, TechSpec Secao 7 |

## Guardrails normativos

### Seguranca e autorizacao

- Toda escrita administrativa ou sensivel deve revalidar a permissao no backend; esconder ou desabilitar uma acao na UI nunca e suficiente (RNF-003).
- A autorizacao escopada deve usar o `projetoId` resolvido no backend a partir do path ou da entidade carregada, nunca confiar apenas em um valor enviado no payload.
- Usuario com `ativo=false` nao pode passar por nenhuma checagem de permissao.
- Subscricoes STOMP de board devem validar vinculo ativo do usuario ao projeto.
- Subscricoes STOMP de notificacoes devem aceitar somente o proprio `usuarioId`.
- O papel global `admin` e protegido: nao pode ser criado, editado, excluido, ter toggles alterados ou ser associado por um administrador local.
- `adminGlobal` pode bypassar RBAC escopado, mas nao pode bypassar a regra de projeto finalizado somente leitura.
- Um usuario nao pode alterar `PapelPermissao` de um papel que ele proprio possui no projeto.
- O catalogo de permissoes e fixo em runtime; alteracoes de toggles devem gerar auditoria.
- Nao implementar fallback de senha local quando Keycloak estiver indisponivel.
- DTOs de entrada devem usar validacao Bean Validation; persistencia deve usar queries parametrizadas.
- Logs nao devem expor tokens, credenciais ou segredos.

### Integridade de dominio

- Projeto finalizado e somente leitura para todos, inclusive `admin` e `adminGlobal`, ate ser reaberto.
- Nao excluir projeto, workflow, etapa, raia ou outro recurso protegido quando houver tarefas ativas vinculadas.
- Toda etapa nao-final deve possuir ao menos uma transicao de saida; etapa final nao possui saida padrao.
- Mover para etapa final ou desfinalizar exige `tarefa:finalizar`.
- Tarefa iniciada congela `titulo` e `descricaoEscopo`; somente campos explicitamente editaveis podem mudar.
- Dev pode autoatribuir a si mesmo, mas nao atribuir a tarefa a terceiros; papeis administrativos podem reatribuir.
- Marcar e desmarcar impedimento deve respeitar `tarefa:impedimento` e registrar historico.
- Alteracoes relevantes de tarefa devem registrar autor, valor anterior, valor novo e data/hora.
- Criacao de card sem defaults explicitos usa a primeira etapa, a primeira raia do projeto ou a raia global; responsavel permanece nulo.
- Criar e excluir card exigem `tarefa:gerenciar`; exclusao por dev exige tambem `tarefa:excluir`/toggle equivalente.

### Operacao, consistencia e empacotamento

- Schema so pode mudar por migrations Flyway; Hibernate deve permanecer em `ddl-auto=validate`.
- Eventos de board e notificacoes devem ser publicados apos commit, nunca antes de uma transacao confirmada.
- Payloads de `LISTEN/NOTIFY` devem permanecer abaixo do limite de 8 KB e conter somente dados necessarios para ressincronizacao.
- Perda de evento ou reconexao do WebSocket/listener deve provocar ressincronizacao via REST; nao assumir entrega garantida de `NOTIFY`.
- Um pod com listener desconectado nao deve ser considerado pronto sob RNF-002.
- Nenhum estado de negocio ou sessao pode depender de memoria local de um pod.
- Backend e frontend devem rodar em imagens Docker sem carregar toolchain de build no runtime.
- URLs e credenciais dependentes de ambiente nao podem ser hardcoded nas imagens.

### Qualidade e testes

- Logica de transicoes, lead-time e resolucao de permissoes deve seguir TDD.
- Toda RF Must Have deve ter cenario de teste correspondente.
- Testes de integracao que sobem a configuracao real de seguranca exigem Keycloak e PostgreSQL disponiveis.
- Testes multi-pod devem verificar propagacao cross-pod, reconexao e ressincronizacao.

## O que nao fazer

- Nao confiar somente em guards client-side, roles do browser ou dados de permissao enviados pelo cliente.
- Nao usar `ddl-auto=update` ou criar schema implicitamente pelo Hibernate.
- Nao publicar notificacao antes do commit da alteracao de negocio.
- Nao usar broadcast local em memoria como mecanismo multi-pod.
- Nao introduzir Redis, Kafka ou outro broker para contornar ADR-002 sem nova decisao arquitetural.
- Nao usar fallback de autenticacao local.
- Nao registrar segredos em logs, arquivos de configuracao versionados ou camadas Docker.
- Nao usar nomes booleanos Java com introspeccao ambigua, como `eFinal`.

## Handoff

- **Status:** candidatos extraidos; validacao de implementacao pendente.
- **Responsavel pela confirmacao:** `/code-review`.
- **Canvas:** a dimensao S permanece DRAFT ate uma revisao de codigo real.
