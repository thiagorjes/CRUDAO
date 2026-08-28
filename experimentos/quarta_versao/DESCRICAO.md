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
- 


