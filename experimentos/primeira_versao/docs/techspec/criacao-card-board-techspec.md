# TechSpec — Criação e gerenciamento de cards no board
_Versão: 1.0 | Status: Draft | Data: 2026-08-24 | Sistema: CRUDAO_
_PRD: docs/prd/criacao-card-board-prd.md (v1.0) | Design Brief: docs/design/criacao-card-board-design-brief.md (v1.0)_

---

## 1. Visão Geral Técnica

Feature majoritariamente **frontend**, com dois ajustes pontuais de backend descobertos durante a análise técnica (comitê architect/security/database, 2026-08-24):

1. **`TAREFA_EXCLUIDA` (broadcast em tempo real):** hoje existe STOMP para `TAREFA_CRIADA`/`TAREFA_MOVIDA`, mas não para exclusão (lacuna do PRD §7) — decisão confirmada com o usuário: criar o evento.
2. **`TarefaService.excluir` precisa migrar de hard-delete para soft-delete:** o comitê encontrou que `excluir()` hoje só limpa `Observador` antes de `tarefaRepository.delete(tarefa)` — `RegistroEtapa`, `Impedimento` e `AuditoriaTarefa` referenciam `tarefa_id` como `@ManyToOne(optional = false)`, sem `ON DELETE CASCADE`. Qualquer tarefa com histórico (toda tarefa, pois a criação já abre `RegistroEtapa`) quebraria o `DELETE` com violação de FK — bug pré-existente, mas que RF-002 (Must Have) expõe na prática pela primeira vez. Decisão do usuário: **soft-delete** — a tarefa passa a ter `excluidaEm` (marca de exclusão), some do board, mas a linha e todo o histórico relacionado (`RegistroEtapa`, `Impedimento`, `AuditoriaTarefa`, `Observador`) permanecem intactos no banco. Isso resolve o problema de FK sem precisar de cascade nem de migração de nulabilidade, e preserva a auditoria (RN-016) — melhor dos dois mundos frente às opções apresentadas ao usuário.

`POST /api/tarefas` continua sem alteração de contrato. `DELETE /api/tarefas/{id}` mantém o mesmo contrato HTTP (204, mesmas validações de RBAC/toggle/projeto finalizado) — só a persistência interna muda.

Todo o restante — botão "Novo card", modal de criação, ícone de lixeira, modal de confirmação de exclusão — é composição de componentes de UI já existentes (`ModalErro`, toast, tokens do design system), sem novo padrão visual.

---

## 2. Decisões Arquiteturais

### D-01 — Sem novo endpoint de backend; `excluir()` migra de hard-delete para soft-delete

**Decisão:** reutilizar `POST /api/tarefas` (`TarefaRequest`) e `DELETE /api/tarefas/{id}` como estão do ponto de vista de contrato HTTP — mesma URL, mesmo verbo, mesmo 204, mesmas validações de RBAC/toggle/projeto finalizado (RN-001/RN-002/RN-003, já implementadas desde TASK-02.1/02.3/04.2).

**Mudança interna (achado do comitê, decisão do usuário):** `TarefaService.excluir` deixa de chamar `tarefaRepository.delete(tarefa)` e passa a fazer soft-delete — seta `tarefa.excluidaEm = Instant.now()` e salva. Motivo: `RegistroEtapa`/`Impedimento`/`AuditoriaTarefa` referenciam `tarefa_id` como `@ManyToOne(optional = false)` sem cascade; toda tarefa tem `RegistroEtapa` desde a criação, então o hard-delete sempre quebraria com violação de FK — bug pré-existente, exposto agora porque RF-002 é a primeira feature a exercitar `DELETE /api/tarefas/{id}` de ponta a ponta pela UI. Soft-delete evita a violação de FK sem cascade e sem migração de nulabilidade, e preserva o histórico de auditoria (RN-016) — a tarefa "some do board", mas nada é apagado do banco.

**Novo campo:** `Tarefa.excluidaEm` (`Instant`, nullable, default `null`). Ver `data-model.md`.

**Efeito em leitura:** `TarefaRepository.findByProjetoIdOrderByCriadoEmAsc` (usado por `listarPorProjeto`, consumido por `GET /api/tarefas?projetoId=`) passa a filtrar `excluidaEm IS NULL` — novo método `findByProjetoIdAndExcluidaEmIsNullOrderByCriadoEmAsc`. `buscar(id)` (`GET /api/tarefas/{id}`) passa a lançar `RecursoNaoEncontradoException` (404) se `excluidaEm != null` — API trata a tarefa como inexistente para qualquer consumidor externo, mesmo com a linha ainda no banco.

**Escopo consciente (fora desta feature):** restaurar tarefa excluída, expurgo definitivo (hard-delete físico após retenção) e inclusão/exclusão de tarefas soft-deleted em relatórios (dashboard de lead-time) não são pedidos pelo PRD — registrados em Questões em Aberto (§10).

### D-02 — Evento `TAREFA_EXCLUIDA` via LISTEN/NOTIFY existente (ADR-004)

**Decisão:** estender o mecanismo já existente (`TipoEventoBoard`, `EventoBoardPublisher`, `PostgresNotificationListener`) com um novo tipo de evento, em vez de criar um canal/tópico separado.
**Justificativa:** consistente com ADR-004 (broadcast multi-pod via `pg_notify`), reaproveita 100% da infraestrutura de `TAREFA_CRIADA`/`TAREFA_MOVIDA`. Nenhum ADR novo — extensão direta de um padrão já decidido.

**Simplificação trazida pelo soft-delete (D-01):** como a linha da `Tarefa` **continua existindo** no banco após a exclusão (só `excluidaEm` é preenchido), `PostgresNotificationListener.montarEvento` **não precisa de nenhum caso especial** — `tarefaRepository.findById(tarefaId)` encontra a tarefa normalmente para `TAREFA_EXCLUIDA` também, e o `EventoBoardDTO` é montado do mesmo jeito que para `CRIADA`/`MOVIDA`/`IMPEDIMENTO_ALTERADO`, sem campos nullable novos. Isso descarta a complexidade que uma versão anterior desta TechSpec havia introduzido (DTO com campos condicionalmente inválidos, apontada como debt pelo comitê de arquitetura) — o `EventoBoardDTO` permanece com a mesma forma para os 4 tipos de evento.

**Nota de segurança herdada (achado do comitê):** o tópico `/topic/projetos/{id}/board` já não valida, no CONNECT/SUBSCRIBE, se o usuário autenticado é membro daquele projeto (débito G-RT-01, registrado desde TASK-04.2/05.1) — qualquer usuário autenticado no sistema pode se inscrever em qualquer projeto e observar os eventos, incluindo agora `TAREFA_EXCLUIDA`. Esta feature **não introduz** essa lacuna, mas amplia o número de tipos de evento afetados por ela; o payload continua minimizado (`tarefaId`/`projetoId`/`etapaAtualId`, sem título/descrição), então o vazamento é de "existência/atividade", não de conteúdo. G-RT-01 permanece como débito técnico não bloqueante — não corrigido nesta feature.

### D-04 — Etapa/raia padrão calculadas no frontend, não no backend

**Decisão:** "coluna 0" (etapa de menor `ordem`) e "primeira raia" (RN-005 do PRD) são resolvidas no **frontend**, no momento de montar o `TarefaRequest`, reaproveitando os arrays `etapas`/`raias` que o `BoardApp` já carrega e já ordena por `ordem` (`estado.etapas` já vem `sort((a,b) => a.ordem - b.ordem)`; `raias` precisa do mesmo tratamento, hoje consumido só via `agruparPorRaiaEEtapa`).
**Justificativa:** `TarefaRequest` já aceita `etapaInicialId`/`raiaId` explícitos — não há necessidade de o backend inferir default algum (ele já tem o comportamento de fallback para raia default global via `RaiaResolver`, mas isso é escopo de outro fluxo). Evita modificar `TarefaService.criar` ou criar um novo endpoint só para resolver defaults. `raiaId` pode ser enviado `null` quando o projeto não tiver raia própria — o backend já trata (`buscarRaiaOuNula`), e o board já cai no grupo "Tarefas" (`RAIA_SEM_RAIA_ID`) nesse caso via `agruparPorRaiaEEtapa`.
**Trade-off aceito:** se o workflow ativo não tiver etapas (`estado.etapas.length === 0`), não há "coluna 0" para default — o botão "Novo card" fica desabilitado nesse estado (board já exibe "workflow ainda não tem etapas configuradas" nesse caso, RF-001 não se aplica).

### D-05 — Nenhum RBAC novo

**Decisão:** gating do botão "Novo card" e do ícone de lixeira usa o mesmo padrão já estabelecido em `AdminApp`/`MembrosAba`: buscar `GET /api/usuarios/me` uma vez, calcular `permissoesProjeto` (Set) por projeto selecionado, checar `permissoesProjeto.has('tarefa:gerenciar')`. `BoardApp` ainda não fazia essa chamada (board hoje não esconde nenhuma ação) — passa a fazer, só para esses dois elementos novos.
**Justificativa:** RN-001 do PRD; reaproveita 100% do padrão de gating existente (`ProjetoPapeis`, `UsuarioMe`). Gating é só estético — backend já revalida (RNF-003/ADR-006), sem mudança nenhuma nesta feature.
**Nota:** o toggle `devPodeExcluirTarefa` (RN-002) já é exposto via `GET /projetos/{id}/configuracao` (`ConfiguracaoProjeto`, consumido hoje por `TogglesAba`) — `BoardApp` passa a buscar essa configuração também, só para decidir se mostra a lixeira a um usuário `dev`-tier. "Dev-tier" no frontend já tem heurística definida (TASK-05.4, `ehDevTier`: tem `tarefa:gerenciar` mas não `tarefa:atribuir`) — reaproveitada aqui sem alteração.

---

## 3. Modelo de Dados

Nenhuma entidade nova. Uma coluna nova (`Tarefa.excluidaEm`, soft-delete — D-01) e o enum `TipoEventoBoard` (+ `TAREFA_EXCLUIDA`), sem mudança de estrutura no DTO de evento (D-02).

Ver `docs/techspec/criacao-card-board/data-model.md` para o detalhamento (curto, dado o escopo).

---

## 4. Contratos de API / Interface

Nenhum endpoint REST novo. `POST /api/tarefas` e `DELETE /api/tarefas/{id}` reutilizados sem alteração de contrato HTTP — `DELETE` muda de comportamento interno (soft-delete, D-01) mas mantém request/response/erros idênticos (204, mesmos 403/404/422 já documentados na TASK-02.1/02.3). `GET /api/tarefas?projetoId=` e `GET /api/tarefas/{id}` passam a excluir tarefas soft-deleted do resultado (404 no caso do `{id}`) — mudança de comportamento, não de forma do contrato.

O único contrato novo é o **evento STOMP `TAREFA_EXCLUIDA`**, documentado em `docs/techspec/criacao-card-board/contracts/evento-tarefa-excluida.md`.

Índice:

| Contrato | Tipo | Arquivo |
|---|---|---|
| Evento `TAREFA_EXCLUIDA` (`/topic/projetos/{id}/board`) | STOMP (existente, tipo novo) | `criacao-card-board/contracts/evento-tarefa-excluida.md` |

---

## 5. Arquitetura e Fluxo

**Criação (RF-001):**
1. `BoardApp` calcula etapa/raia padrão (D-04) a partir do estado já carregado.
2. Modal "Novo card" coleta título/descrição/tipo/responsável (opcional).
3. `POST /api/tarefas` com `{ projetoId, etapaInicialId, raiaId, tipo, titulo, descricao, responsavelId: null }`.
4. Backend cria, publica `TAREFA_CRIADA` (já existente) via `afterCommit` → `pg_notify` → `PostgresNotificationListener` de cada pod → STOMP `/topic/projetos/{id}/board`.
5. Frontend que criou já tem o card na resposta HTTP (adiciona localmente, sem esperar o próprio evento STOMP — mesmo padrão de `mover()` em `BoardApp`, atualização otimista/direta); demais clientes conectados recebem via STOMP e buscam o estado completo (`atualizarTarefaLocal`, fluxo já existente para "tarefa não encontrada localmente").

**Exclusão (RF-002):**
1. Usuário confirma no modal → `DELETE /api/tarefas/{id}`.
2. Backend valida RBAC/toggle/projeto finalizado (já implementado), marca `excluidaEm` (soft-delete, D-01) em vez de apagar a linha, publica `TAREFA_EXCLUIDA` (novo) via o mesmo pipeline `afterCommit`/`pg_notify`.
3. `PostgresNotificationListener.montarEvento` monta o DTO normalmente (a tarefa ainda existe no banco — D-02), sem nenhum caso especial.
4. Frontend que excluiu remove o card localmente na resposta 204 (direto, sem esperar o evento); demais clientes recebem `TAREFA_EXCLUIDA` via STOMP e removem o card do estado local por `tarefaId` (novo branch em `atualizarTarefaLocal`, análogo ao branch de "tarefa já existe" hoje).

Nenhuma mudança em `WebSocketConfig`/`StompAuthChannelInterceptor` — mesmo tópico, mesma autenticação (RF-005/TASK-05.1).

---

## 6. Dependências Inter-Sistemas

Nenhuma. Feature inteiramente interna ao sistema CRUDAO (backend + frontend do mesmo repositório). Nenhum mock contract necessário.

---

## 7. Estratégia de Testes

Segue `systems/CRUDAO/guidelines/testing.md` (TDD 80% lógica geral, BDD 100% cenários Gherkin do PRD).

**Backend (TDD):**
- `TarefaServiceTest`: caso `excluir` faz soft-delete (`excluidaEm` preenchido, linha continua no banco, `RegistroEtapa`/`Impedimento`/`AuditoriaTarefa`/`Observador` intactos — teste explícito para não regredir ao hard-delete que quebrava por FK); caso `excluir` publica `TAREFA_EXCLUIDA` (verificar chamada ao `EventoBoardPublisher` com o tipo certo).
- `TarefaServiceTest` (leitura): `listarPorProjeto` não retorna tarefa soft-deleted; `buscar(id)` de tarefa soft-deleted lança `RecursoNaoEncontradoException`.
- Integração (`RealtimeBoardIT`, já existente): estender com um cenário de exclusão — 2 clientes STOMP reais, um exclui, outro recebe `TAREFA_EXCLUIDA` em até 2s (RNF-001), mesmo padrão dos cenários de `TAREFA_CRIADA`/`TAREFA_MOVIDA` já cobertos.

**Frontend (sem TDD obrigatório para UI declarativa — mesma decisão de TASK-05.2/05.3/05.4, mas com cobertura de lógica pura):**
- Lógica pura testável isoladamente (Vitest): função que resolve etapa/raia padrão (D-04) — casos "workflow com etapas" / "projeto sem raia própria" / "workflow sem etapas" (botão desabilitado).
- `agrupar.ts`/`atualizarTarefaLocal` (se extraído para função pura testável): caso `TAREFA_EXCLUIDA` remove a tarefa do array por `id`.
- `tsc --noEmit`, `eslint`, `next build` limpos (barreira mínima já usada nas tasks de UI anteriores).

**BDD / Critérios de aceite do PRD (100% obrigatório):**
- RF-001: os 3 cenários Gherkin do PRD (criar com sucesso, botão oculto sem permissão, bloqueio por projeto finalizado).
- RF-002: os 3 cenários Gherkin do PRD (confirmação abre modal, exclusão remove card, ícone oculto sem permissão/toggle).
- Cobertura recomendada via E2E (Playwright, já configurado desde TASK-06.1) — estender `board.spec.ts` ou novo `criacao-exclusao-card.spec.ts` seguindo o padrão de fixtures existente (`e2e/fixtures/api.ts`).

---

## 8. Segurança e Observabilidade

**Segurança:** RBAC/toggle/projeto-finalizado já validados no backend (RNF-003/ADR-006) pelos endpoints reutilizados — esta feature não introduz nenhum caminho de escrita que os contorne. Gating de UI (D-05) é só estético, conforme padrão do sistema. Esta feature **herda e amplia o escopo** (não introduz) o débito G-RT-01 — subscription STOMP não valida vínculo do usuário ao projeto (ver D-02) — registrado como débito técnico não bloqueante, não corrigido aqui.

**Observabilidade:** segue `systems/CRUDAO/guidelines/observability.md` (log tradicional, sem telemetria nesta fase). Falha ao publicar `TAREFA_EXCLUIDA` já cai no mesmo `catch`/log de `EventoBoardPublisher.publicar` (não propaga, não desfaz a exclusão — mesmo comportamento aceito em ADR-004 para os demais tipos de evento).

---

## 9. Matriz de Rastreabilidade

| RF | Descrição | Componentes | Testes |
|----|-----------|-------------|--------|
| RF-001 | Criar card pelo board | `BoardApp` (botão + gating), novo `ModalNovoCard`, `POST /api/tarefas` (existente), D-04 (defaults) | BDD (3 cenários), teste unitário de resolução de defaults |
| RF-002 | Excluir card pelo board | `CardTarefa` (ícone + gating), novo `ModalConfirmarExclusao` (ou reuso de `ModalErro`/padrão de confirmação existente), `DELETE /api/tarefas/{id}` (soft-delete, D-01), evento `TAREFA_EXCLUIDA` (D-02) | BDD (3 cenários), `RealtimeBoardIT` (cenário de exclusão), teste de soft-delete em `TarefaServiceTest` |

Verificação automatizada:
```
python .agents/skills/techspec/scripts/check_rf_coverage.py \
  --prd docs/prd/criacao-card-board-prd.md \
  --techspec docs/techspec/criacao-card-board-techspec.md
```

---

## 10. Questões em Aberto

Nenhuma bloqueante. Registro não bloqueante:

| Questão | Impacto | Bloqueante? |
|---|---|---|
| Nome exato do componente de modal de confirmação de exclusão (novo `ModalConfirmarExclusao` genérico vs. inline no `CardTarefa`) | Baixo — decisão de implementação, não de contrato; ambos atendem RF-002 | Não |
| Extrair `atualizarTarefaLocal` para função pura testável (hoje é uma closure dentro de `BoardApp`) | Testabilidade um pouco melhor; não é pré-requisito para a feature funcionar | Não |
| Restaurar tarefa soft-deleted / expurgo definitivo após retenção — não pedido pelo PRD | Nenhum caminho de UI ou API para desfazer a exclusão nesta feature; dado fica retido indefinidamente no banco | Não |
| Dashboard de lead-time (RF-006/007) hoje não filtra tarefas soft-deleted em seus cálculos agregados (consulta via `RegistroEtapa`, não via `TarefaRepository.listarPorProjeto`) — decisão de incluir/excluir do cálculo não foi tomada | Baixo-médio — pode inflar médias com tarefas excluídas; não afeta RF-001/RF-002 | Não |
| G-RT-01 (subscription STOMP sem checagem de membro do projeto) segue sem correção — apenas com escopo ampliado por esta feature | Vazamento de existência/atividade de tarefas entre projetos para qualquer usuário autenticado; sem vazamento de conteúdo | Não |

---

## 11. Histórico de Revisões

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-08-24 | Thiago Goncalves Cavalcante | Versão inicial |
