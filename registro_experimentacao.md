Primeia versão do SSPDD
- criei um projeto com a primeira versão do SSPDD
o que pedi:
-- dashboar para acompanhar as tarefas dos times em projetos, pois antes era feito via mensagens e emails.
qual a dor:
-- acompanhar por email/msg/notificacao é ruim, pois as mensagens podem se perder em meio aos spams e chegams a ficar 5 dias parados pois uma mensagem não foi lida, e isso poderia ter sido resolvido com um "sim ou não" se tivesse lido a mensagem a tempo.
o que foi gerado:
- um sistema de gestão de atividades
- gestão de projetos, com usuários associados, workflow personalizavel e gestão de papeis (quem faz o que)
- pasta primeira_versão

problemas identificados:
- o guideline negligenciava a sessão de design
- o prd não exigia etapa de design mesmo sabendo que seria um sistema web
- o canvas não era atualizado automaticamente em todas as etapas
- skills não estavam no formato canonico
- as implementações de sistema não ficavam na pasta systems (precisamos definir isso também)
- readme desatualizado em relação ao SSPDD - puxava o fluxo SDD
- AGENTS desatualizado em relação ao SSPDD - puxava o fluxo SDD
- init.py desatualizado em relação ao SSPDD - puxava algumas coisas do fluxo SDD
- não levantou todos os requisito, foi necessário testes com o produto para solicitar ajustes essenciais.

versão final utilizável.
regras atendidas, mas cabia evolução futura.
resultado na pasta "primeira_versao".

Teste com segunda versão ajustados os problemas detectados na V1.
- usei o conhecimento (artefatos de designer/prd/reasons-canvas das 2 atividades da V1 para responder ao questionário do SSPDD na v2)
- fluxo adotado:
1- preparei uma sessão do claude sonnet, dentro da pasta "primeira_versao" e usei o seguinte prompt:
"vou fazer uma série de perguntas e espero que você responda com base nos artefatos de docs, priorizando os arquivos relacionados ao "kanban-configuravel", depois ao criacao-card-board, primeiro pelo discovery, depois pelo prd e por ultimo nos reasons-canvas. ok?"

2- em outra sessão iniciei a v2, ela fazia a pergunta e eu copiava e envia na sessão da v1.
3- respondi ao discovery, prd e obtive boa cobertura.
4- o designer foi chamado automaticamente (recomendado no prompt,pois as etapas não iniciam sem autorização do usuário) e gerou mais telas e melhores que na V1, sem usar as referencias anteriores (peguei apenas as cores como referencia);
5- os protótipos ficaram muito superiores (pasta docs/design/prootypes/);
6- o techspech seguiu suave pois reutilizei os guidelines já levantados na etapa da V1, e precisei responder o básico, sem necessidade de usar a V1 como referencia, pois ela estava inferindo melhor.
7- os ponto de dúvida eu recorria a V1.
8- solicitei a escrita de histórias.
9- observei que depois do techspec o contexto ficou em torno de 170k tokens, antes não passava de 90k.
10- sequencia de perguntas do tasks se manteve igual e iniciou escrita.
11- implementação encontrando alguns erros, que estão sendo corrigidos ao final antes de seguir para a próxima.
12- 