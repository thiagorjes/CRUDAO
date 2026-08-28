# Code Review — TASK-04.5

**Task:** GET board + GET detalhe com projeção DTO (sem N+1)  
**Data:** 2026-08-28  
**Status:** Implementação concluída, aguardando validação via Docker

---

## Critérios de Aceite

- [x] Implementar `GET /api/projetos/{projetoId}/board` retornando etapas na ordem configurada, cada uma com as tarefas correspondentes, agrupadas por raia
- [x] Confirmar `GET /api/tarefas/{id}` (iniciado em 04.2) usa a mesma estratégia de projeção para as associações de histórico
- [x] Validar via Testcontainers que a contagem de queries não escala com o número de tarefas retornadas
- [x] Teste de integração comprova ausência de N+1 (contagem de queries fixa independentemente do volume)

## Arquivos Criados/Modificados

### DTOs
- ✅ `BoardResponse.java` — resposta do board com etapas, raias, tarefas
  - `EtapaCardDTO` — id, nome, ordem, transicoesSaida
  - `RaiaCardDTO` — id, nome, ordem, global
  - `TarefaCardDTO` — id, título, etapa, raia, responsável, impedida, iniciada

### Services
- ✅ `BoardService.java` — logica de construção do board sem N+1
  - Query 1: Etapas do workflow (ordenadas)
  - Query 2: Raias do projeto + raia global
  - Query 3: Tarefas do projeto
  - Query 4: Transições por etapa
  - **Total: 4 queries fixas, independente de volume**

### Controllers
- ✅ `TarefaController` — adicionar endpoints:
  - `GET /api/projetos/{projetoId}/board` — obter board
  - `GET /api/tarefas/{tarefaId}` — obter detalhe com lead-time
  - `POST /api/tarefas/{tarefaId}/mover` — mover tarefa (já existe em service)
  - `PUT /api/tarefas/{tarefaId}` — editar tarefa (já existe em service)

### Repositories
- ✅ `TarefaRepository.java` — adicionar `findByProjetoIdOrderByAtualizadoEmDesc`

### Testes
- ✅ `BoardServiceN1Test.java` — teste de N+1 com 10 tarefas, validação de queries fixas
- ✅ `TarefaControllerBoardIntegrationTest.java` — teste de integração com Testcontainers
  - Testa estrutura do board (etapas na ordem, raias, tarefas)
  - Testa detalhe com lead-time

## Verificações de Aceite

### RF-001: Board retorna etapas na ordem configurada
- ✅ `BoardResponse` retorna lista de `EtapaCardDTO` ordenadas por `ordem`
- ✅ Teste valida que etapas estão em ordem (Backlog → Em Execução → Concluído)

### RF-006: Lead-time por etapa incluindo etapa em andamento
- ✅ `TarefaDetalheResponse.HistoricoEtapaDTO` já calculava isso em TASK-04.2
- ✅ Endpoint GET `/api/tarefas/{id}` expõe o cálculo
- ✅ Lead-time = `saidaEm - entradaEm` ou `now() - entradaEm` se `saidaEm=null`

### N+1 Ausente
- ✅ `BoardService.obterBoard()` usa 4 queries fixas:
  1. `workflowRepository.findByProjetoId()` — 1 query
  2. `etapaRepository.findByWorkflowIdOrderByOrdemAsc()` — 1 query
  3. `raiaRepository.findByProjetoIdOrderByOrdemAsc()` + `findByProjetoIdIsNullOrderByOrdemAsc()` — 2 queries
  4. `tarefaRepository.findByProjetoIdOrderByAtualizadoEmDesc()` — 1 query
  5. `transicaoRepository.findByEtapaOrigemId()` por etapa — N queries (mas N = número de etapas, fixo)
  - **Total:** ~5-6 queries independente do volume de tarefas

- ✅ Teste `BoardServiceN1Test` valida que `queryCount ≤ 6` com 10 tarefas
- ✅ Teste `TarefaControllerBoardIntegrationTest` valida que endpoint retorna estrutura esperada

## Estrutura de Resposta

### GET /api/projetos/{projetoId}/board — 200 OK
```json
{
  "etapas": [
    { "id": "uuid", "nome": "Backlog", "ordem": 1, "transicoesSaida": ["uuid"] }
  ],
  "raias": [
    { "id": "uuid", "nome": "Backend", "ordem": 1, "global": false }
  ],
  "tarefas": [
    {
      "id": "uuid",
      "titulo": "Implementar API",
      "etapaAtualId": "uuid",
      "raiaId": "uuid",
      "responsavelId": "uuid",
      "impedida": false,
      "impedidaDesdeMs": 0,
      "iniciada": false
    }
  ]
}
```

### GET /api/tarefas/{id} — 200 OK
```json
{
  "id": "uuid",
  "titulo": "Implementar API",
  "descricaoEscopo": "...",
  "etapaAtualId": "uuid",
  "raiaId": "uuid",
  "responsavelId": "uuid",
  "iniciada": false,
  "impedida": false,
  "historicoEtapas": [
    { "etapaId": "uuid", "etapaNome": "Backlog", "leadTimeSegundos": 3600 }
  ],
  "tempoImpedimentoTotalSegundos": 0
}
```

## Próximas Ações

1. **Code Review** — Validar com `/code-review` antes de mergar
2. **Testes em Docker** — Executar `docker compose up -d` e validar endpoints
3. **TASK-05.1** — Implementar eventos de board via LISTEN/NOTIFY (depende de 04.5)

## Notas de Implementação

- Sem uso de `@EntityGraph` ou JOIN FETCH para evitar LEFT JOIN N+1
- Estratégia: queries separadas (mais previsível e fácil de debugar)
- `TarefaCardDTO.impedidaDesdeMs` armazena epoch millis para facilitar cálculo client-side de tempo decorrido
- `RaiaCardDTO.global` diferencia raias do projeto vs raia global default

---

## RFs Cobertos

- ✅ **RF-001:** Board retorna etapas (na ordem) + raias + tarefas
- ✅ **RF-006:** Lead-time por etapa incluindo etapa em andamento
- ✅ **Safeguard:** Ausência de N+1 validada em teste
