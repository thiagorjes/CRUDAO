# Code Review — TASK-02.1 — CRUD de Tarefa e movimentação entre etapas

_Revisor: agent qa | Data: 2026-08-22_

## Gate de testes

`mvn test` (via docker maven:3.9-eclipse-temurin-25) — **PASSOU**, sem falhas reportadas.

## Critérios de Aceite

| # | Critério de Aceite | Verificado? | Evidência |
|---|---|---|---|
| 1 | Movimentação só ocorre se transição permitida pelo workflow (teste unitário) | ✅ | `TarefaServiceTest.deveMoverQuandoTransicaoPermitidaPeloWorkflow` / `naoDeveMoverQuandoTransicaoNaoPermitidaPeloWorkflow` |
| 2 | Marcar/desmarcar impedimento funciona independente da posição no workflow | ✅ | `TarefaServiceTest.deveMarcarImpedimentoIndependenteDaEtapaAtual` / `deveDesmarcarImpedimentoSemAlterarEtapaAtual` |
| 3 | Cenários Gherkin do PRD (RF-002, RF-004, RF-012) cobertos a 100% (BDD) | ⚠️ | Não há testes BDD nomeados/rastreáveis a cenários Gherkin específicos, nem cenário dedicado para REABERTURA (RF-012) via `TarefaService.mover` — a lógica de tipo de transição fica inteiramente na `TransicaoEngine` (já testada na TASK-01.1), mas não há teste aqui exercitando explicitamente uma transição do tipo REABERTURA "desfinalizando" uma tarefa via `TarefaService` |

## 🔴 Crítico

Nenhum.

## 🟡 Importante

#### I1 — `moverParaProjeto` sem cobertura de teste
Arquivo: `backend/src/main/java/com/crudao/kanban/domain/tarefa/TarefaService.java:112-124`
Problema: item explícito do escopo da task ("suporte a mover tarefa entre projetos") não tem nenhum teste unitário (herança de workflow do destino, etapa pertencente ao workflow destino). `TarefaServiceTest` cobre apenas `mover` e impedimento.
Guideline violado: `testing.md` — TDD 80% cobertura mínima; funcionalidade nova sem teste é lacuna de cobertura, não apenas estilo.

#### I2 — Cenário REABERTURA (RF-012) não exercitado no nível de serviço
Arquivo: `backend/src/test/java/com/crudao/kanban/domain/tarefa/TarefaServiceTest.java`
Problema: a task pede explicitamente suporte à transição REABERTURA para "desfinalizar" tarefa. O teste de `mover` usa uma `Transicao` genérica sem setar `tipo`, não comprovando que uma transição REABERTURA de fato reabre uma tarefa finalizada via o fluxo do serviço.
Guideline violado: `testing.md` — BDD 100% dos cenários Gherkin do PRD (RF-012).

#### I3 — `motivo` do impedimento é descartado silenciosamente
Arquivo: `backend/src/main/java/com/crudao/kanban/domain/tarefa/TarefaService.java:126-133`
Problema: `TarefaImpedimentoRequest.motivo` é recebido pelo controller/service mas nunca usado — o parâmetro `request` de `marcarImpedimento` não é lido. Aceitável dado que o histórico (Impedimento/RegistroEtapa) é escopo da TASK-03.1, mas o parâmetro deveria ao menos ser documentado como "aceito e descartado por ora" no controller, ou omitido do endpoint até a TASK-03.1 existir, para não sugerir que o motivo é persistido.
Como corrigir:
  Atual:   parâmetro presente e ignorado sem nota no controller
  Correto: manter (é decisão de escopo já registrada no Javadoc do record), mas adicionar comentário no `TarefaService.marcarImpedimento` reforçando "motivo intencionalmente não persistido nesta task — ver TASK-03.1"
Guideline violado: não coberto explicitamente — recomendo nota de manutenibilidade.

## 🔵 Sugestão

#### S1 — `excluir` sem soft-delete/guarda
Arquivo: `TarefaService.java:79-81`
Exclusão de tarefa é física (hard delete), sem qualquer guarda de negócio (ex.: tarefa em etapa final, tarefa com histórico). Fora do escopo do PRD nesta task, mas vale registrar como possível gap futuro se RF-003 vier a exigir auditoria de exclusão.

#### S2 — `java.time.Instant.now()` chamado inline por FQN
Arquivo: `TarefaService.java:74,101,122,131,139`
Uso de `java.time.Instant.now()` totalmente qualificado repetidamente em vez de import estático/import de classe (já importado `java.time.Instant` na entidade, mas não no service). Puramente estético, sem violação de guideline formal.

#### S3 — Duplicação em `buscarWorkflowAtivo`/`buscarEtapaDoWorkflow`
Padrão "buscar ou lançar exceção" se repete de forma consistente com o restante do domínio (`workflow`, `projeto`) — nenhuma ação necessária, apenas observação positiva de consistência.

## ✅ Pontos Positivos

- Separação clara de responsabilidades: `mover` (workflow) não se mistura com `moverParaProjeto` (troca de projeto+workflow+etapa) nem com impedimento — reflete bem a regra RF-004 "impedimento não bloqueia/libera movimentação".
- `TarefaService.mover` reaproveita `TransicaoEngine` já testada na TASK-01.1, evitando duplicar regra de negócio do workflow.
- `TarefaImpedimentoRequest` documenta explicitamente, via Javadoc, por que o histórico foi deixado de fora — boa prática de rastreabilidade de decisão de escopo.
- `VerificadorDeTarefasAtivas` migrado corretamente de porta vazia para implementação real (`VerificadorDeTarefasAtivasImpl`), fechando a nota técnica pendente da TASK-01.1 (RN-005).
- Uso consistente de `MapStruct` para DTO (`TarefaMapper`), `Bean Validation` (`@NotNull`/`@NotBlank`) e `RegraDeNegocioException`/`RecursoNaoEncontradoException` já estabelecidas no domínio — aderente a `coding-standards.md`.
- Nomenclatura de campos booleanos (`impedida`) evita a armadilha JavaBeans documentada em `coding-standards.md` (achado da TASK-01.1 aplicado corretamente aqui).

## Segurança

- `SecurityConfig` (`backend/src/main/java/com/crudao/kanban/config/SecurityConfig.java`) libera `/api/**` via `permitAll()` de forma consciente e documentada (TODO TASK-04.1, ADR-003), aplicada desde a TASK-00.2 — **todos** os controllers de domínio (`ProjetoController`, `TarefaController`, etc.) estão igualmente sem controle de acesso hoje. O TODO no `moverParaProjeto` (`TarefaService.java:109-110`) é consistente com esse estágio do projeto e não introduz uma exposição nova ou desproporcional em relação ao resto da API — não bloqueante.
- Input validation: presente via Bean Validation (`@NotNull`, `@NotBlank`) em todos os requests de entrada externa (`TarefaRequest`, `TarefaMoverRequest`, `TarefaMoverProjetoRequest`). `TarefaImpedimentoRequest` não tem validação, mas seu único campo (`motivo`) é opcional e de baixo risco.
- Sem SQL injection: uso exclusivo de Spring Data JPA (query methods/JPQL derivado), sem SQL nativo concatenado.
- Sem secrets hardcoded nos arquivos revisados.
- Sem logging de dados sensíveis nos arquivos revisados (não há logging explícito ainda — ver Observabilidade abaixo).

## Conformidade com TechSpec

- Estrutura de pacote `domain/tarefa` conforme especificado no guia técnico da task.
- Contratos REST (`PATCH /mover`, `POST`/`DELETE /impedimento`) seguem o padrão de verbos HTTP esperado; `PATCH /mover-projeto` adicional é aderente ao item extra do escopo ("mover entre projetos").
- Campo `TipoTarefa` implementado como enum simples (`FEATURE`, `BUG`, `CHORE`) resolvendo a Q-004 da techspec — sem contradição encontrada com a techspec revisada.
- `RegistroEtapa`/`Impedimento` (histórico) e notificação a observadores (RF-005/STOMP) corretamente fora de escopo, conforme já alinhado.

## Observabilidade

- Nenhum log estruturado nos pontos críticos (criação, movimentação, exclusão de tarefa) — `observability.md` define logs em arquivo local com rotação, mas não há evidência de logging aplicado nesta task nem nas anteriores revisadas. Não é regressão desta task especificamente (padrão ainda não estabelecido no código-base), mas fica como lacuna a considerar antes de produção.

## Resultado

**APROVADO COM RESSALVAS**

Nenhum item crítico. Os dois "Importantes" (I1, I2) são lacunas de cobertura de teste em funcionalidades que a própria task lista como escopo (mover entre projetos, REABERTURA) — recomendo fechar antes do merge para não abrir precedente de "AC 100% BDD" sem evidência real. I3 é cosmético.
