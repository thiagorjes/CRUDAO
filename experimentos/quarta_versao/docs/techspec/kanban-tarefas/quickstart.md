# Quickstart — Kanban de Tarefas

_Versão: 1.0 | Data: 2026-08-25_

> Guia rápido para `/implement`/`/tdd` antes de codificar. Não repete o que já está em `data-model.md`/`contracts/`.

---

## Stack e estrutura de pastas

```
backend/
  src/main/java/.../kanban/
    controller/    # REST + STOMP handlers
    service/       # regras de negócio (TarefaService, WorkflowService, PermissaoService...)
    repository/    # Spring Data JPA
    dto/           # request/response
    mapper/        # MapStruct
    entity/        # JPA entities (ver data-model.md)
    event/         # listener LISTEN/NOTIFY + publisher STOMP
  src/main/resources/db/migration/   # Flyway V1..V7
  src/test/java/...                  # JUnit 5 + Testcontainers
frontend/
  app/ (Next.js) — rotas conforme design-brief.md (TL-01..TL-10)
  components/
  lib/ (client REST + STOMP)
```

## Setup mínimo

**Via Docker (ADR-008, único modo suportado):**
1. `docker compose up -d` (sobe `postgres`, `keycloak`, `backend`, `frontend` — Flyway aplica migrations automaticamente no boot do backend).
2. Acessar `http://localhost:3000`.

Keycloak: realm/client pré-configurado com redirect URI `http://localhost:3000/login/oauth2/code/keycloak`.

Admin global (ADR-007): logue com `admin.teste@crudao.local` / `admin123` (já cadastrado no realm dev) — vira `adminGlobal=true` no primeiro login (property `kanban.bootstrap.admin-email`, já setada em `application-dev.yml`), única forma de criar o primeiro projeto e configurar papéis/usuários.

Não executar backend, frontend ou Keycloak diretamente no host. `mvnw` e `npm run dev` não são caminhos de validação desta especificação.

## Cenários principais por RF (Dado/Quando/Então)

### RF-002 — Transição bloqueada
**Dado** workflow com etapas A→B configuradas (sem B→C)
**Quando** `POST /api/tarefas/{id}/mover` com card em B e `etapaDestinoId=C`
**Então** `409`, card permanece em B, nenhum `TarefaAuditoria`/evento é gerado

```java
// exemplo de teste de integração (TarefaServiceIT)
assertThrows(TransicaoNaoPermitidaException.class,
    () -> tarefaService.mover(tarefaId, etapaCId, usuarioDev));
```

### RF-003 — Congelamento pós-início
**Dado** card com `iniciada=true`
**Quando** `PUT /api/tarefas/{id}` envia `titulo` alterado
**Então** `409`, `titulo` original preservado

### RF-004/RN-002 — Impedimento acumulado
**Dado** card marcado impedido às 10h, desmarcado às 11h, marcado de novo às 14h, desmarcado às 14h30
**Quando** `GET /api/tarefas/{id}`
**Então** `tempoImpedimentoTotalSegundos = 90min` (soma dos 2 ciclos fechados)

### RF-018/RN-CB-004/005 — Criação com defaults
**Dado** projeto sem raia própria (usa raia default global)
**Quando** `POST /api/projetos/{id}/tarefas` sem `responsavelId`/`raiaId`
**Então** card criado com `responsavelId=null`, `raiaId=<raia default global>`, `etapaAtualId=<etapa de menor ordem>`

### RF-019/RN-CB-002 — Exclusão condicionada a toggle
**Dado** usuário papel `dev`, projeto com `devPodeExcluirTarefa=false`
**Quando** `DELETE /api/tarefas/{id}`
**Então** `403`

**Dado** mesmo cenário com `devPodeExcluirTarefa=true`
**Então** `204`, evento `TAREFA_EXCLUIDA` publicado

### RNF-001 — Propagação em tempo real
**Dado** 2 clientes STOMP conectados a `/topic/board/{projetoId}`
**Quando** um cliente cria um card
**Então** o outro cliente recebe `TAREFA_CRIADA` em até 2s (teste assíncrono com `Awaitility`, timeout 2s)

### RNF-002 — Propagação cross-pod (multi-instância)
**Dado** 2 instâncias Spring Boot (pod A e pod B) compartilhando o mesmo PostgreSQL Testcontainer, cliente STOMP conectado ao pod B
**Quando** uma requisição de movimentação de card é processada pelo pod A
**Então** o `NOTIFY` disparado pelo pod A é recebido pelo listener do pod B e retransmitido ao cliente conectado nele, em até 2s — valida que não há estado não compartilhado impedindo o broadcast entre instâncias

### ADR-004 — Reconexão do listener LISTEN/NOTIFY
**Dado** listener JDBC em `LISTEN` no canal `board_events`
**Quando** a conexão é derrubada (kill da conexão via Testcontainers/admin do Postgres)
**Então** o backend reconecta com retry/backoff em até N tentativas, loga `WARN`→`ERROR` progressivo, e o readiness probe do pod reflete "não pronto" enquanto desconectado; após reconexão, o próximo `NOTIFY` disparado volta a propagar normalmente

### ADR-004 — Resincronização client-side por gap de `seq`
**Dado** cliente com último `seq` recebido = 5 para um board
**Quando** um evento chega com `seq=8` (gap) ou o WebSocket reconecta após queda
**Então** o cliente descarta o estado incremental e refaz `GET /api/projetos/{projetoId}/board`, garantindo consistência mesmo sob perda de evento durante reconexão do listener

### RF-016/RN-017 — Autoconcessão de permissão bloqueada
**Dado** usuário com papel `project_admin` no projeto, tentando alterar `PapelPermissao` do próprio papel `project_admin`
**Quando** `PUT /api/papeis/{id}/permissoes/{chave}`
**Então** `403` — apenas outro usuário com `papel:administrar` pode alterar permissões do papel que o usuário autenticado possui

## Pontos de atenção

- **Nunca** usar `ddl-auto=update` — schema só via Flyway (ADR-005).
- **Nomenclatura de boolean:** seguir `coding-standards.md` — evitar `isXFinal` com duas maiúsculas seguidas (usar `etapaFinal`, não `eFinal`).
- **RNF-003:** toda checagem de permissão do frontend é só UX — replicar sempre no service backend, nunca confiar em "se o botão não aparece, a ação está bloqueada".
- **RN-011/RN-012:** lógica de atribuição/finalização é a mais sensível a bug silencioso — cobrir com teste unitário por combinação de papel × ação antes de integrar ao controller.
- **ADR-004:** listener LISTEN/NOTIFY deve reconectar com backoff — sem isso, um pod "surdo" após queda de rede quebra RNF-001 silenciosamente (sem erro visível ao usuário).
- **Testes de integração exigem Keycloak rodando (achado TASK-02.3):** `spring-boot-starter-oauth2-client` resolve o issuer OIDC eagerly na subida do `ApplicationContext` — qualquer teste `@SpringBootTest`/`@WebMvcTest` que suba o `SecurityConfig` real falha com `ConnectException` sem `docker compose up -d keycloak postgres` rodando antes de `mvn test` (ver `testing.md`). Testes unitários com Mockito (sem contexto Spring) não são afetados.

## Cenários de teste críticos (resumo)

Ver Seção 7 da TechSpec principal para a lista completa mapeada por RF.
