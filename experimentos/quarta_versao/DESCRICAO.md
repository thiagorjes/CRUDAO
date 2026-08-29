Vou copiar os artefatos abaixo gerados na segunda rodada e continuar o fluxo de tasks com outro modelo, 
simulando a ideia de escolher modelos diferentes para cada etapa.

Artefatos:
- docs/discovery
- docs/prd
- docs/design
- docs/techspec
- docs/decisions
- systems/CRUDAO/guidelines

Intenção:
- Validar resultados com outro harness/modelo na mesma versão do fluxo SSPDD

Modelo:
- Gemini-3.6-flash para "/implement" e "/code-review" até a task 4.1
- Gemini-3.7-flash para "/tasks" e /implement,/code-review a partir da task 4.2.
- Interrompi a implementação com gemini+copilot pois estava falhando muito e travando.
- resolvi continuar com claude haiku. tudo fluindo melhor com haiku.

Harness:
- Iniciei no copilot no VSCODE
- Migrei para o antigravity no VSCODE, pois durante a etapa de implementação o copilot se mostrou mais "burro" (enquanto harness) que o claude, então resolvi testar a extensão do antigravity (harness para vscode).
- acabaram os tokens do antigravity e retornei ao copilot com o gemini da AUMO

Forma de Licença:
- Conta PRO particular

Vou solicitar que a memoria, canvas etc sejam recriados.
precisei recriar:
- memory/state 
- memory/constitution
- ADRs 1, 2 e 3
- reasons-canvas como docs/spdd/kanban-tarefas-canvas.md
- iniciou as implementações muito bem
- ficou "enrolando" para executar os testes (TDD/BDD)
- tem deixado muitas tasks com o status desatualizado, ora na task, ora no canvas/state, gerando problemas no fluxo e exigindo nova validação e update de status.

Problemas:
- criar uma validação (script python) para garantir que os testes (tdd/bdd) estão criados para as tarefas que exigem isso.
- criar uma validação (script python) para garantir que os testes serão executados.
- criar uma validação (script python) para garantir que os testes foram executados.
- garantir que o code-review seja executado ao final do implement/tdd.
- garantir que todos os cenários (tdd e bdd) foram mapeados, implementados e associados a cada task para serem validados ao final da implementação. sem isso a etapa de verificação não fica confiável.
- está sendo necessário rodar o /tests tasks-XX.X para validar se todos os testes estão mapeados, implementados e com sucesso.
- ajustar a pipeline para:
1. criar a tarefa com os comportamentos que precisam ser testados (critérios de aceite)
2. executar o /implement
3. o /implement chama o /tests para validar se todos os testes estão criados e coerentes.
4. o /tests avalia apenas o escopo da task em si confrontando com o techspec, mas sem alterar outras tarefas/testes.
5. o /implement executa os testes através do /tdd (que deve mudar de nome, pois vai validar tanto testes unitários, quanto os comportamentos e integrações)
6. a etapa de testes só finaliza depois de tudo verde (seguindo REG->GREEN), se as alterações demorarem mais de X turnos (valor de X configurável no techspec) na mesma task, gera alerta e aguarda usuário avaliar.
7. garantir que todos os testes serão mapeados, implementados e testados com sucesso. testes com sucesso obrigatórios: Acceptance Criteria Tests, Unitary Tests , Integration Tests.
8. fluxo deve ser: Criar Banco e schema, criar modelos/services/controllers/repositories etc, rodar flyway, criar testes, rodar testes. Assim conseguiremos rodar as etapas anteriores sem falhas.



