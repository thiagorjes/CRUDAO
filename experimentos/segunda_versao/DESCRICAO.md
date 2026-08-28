Segunda versão:

Intenção:
- Validar melhorias nas skills/agents do fluxo SSPDD

Modelo:
-Claude Sonnet

Harness:
- Claude Code no VSCODE

Forma de Licença:
- Conta PRO particular

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
12- foi necessário solicitar a criação do docker de back e front depois de finalizadas todas as tasks
13- problema real de login ao testar o resultado.
```

o Keycloak emite o iss do token sempre com a URL pública (http://localhost:8080/...), mas a validação do id_token no frontend compara contra o issuer interno (http://keycloak:8080/...) — mismatch, jwtVerify lança, exceção é engolida silenciosamente e cai no erro genérico. Confirmei trocando o código manualmente de dentro do container (token exchange funcionou, iss veio como http://localhost:8080/realms/kanban-dev).

Vou corrigir em duas frentes: fixar o hostname do Keycloak (comportamento hoje é "adivinhado" pela primeira request — frágil) e corrigir a checagem no frontend.

Atenção: isso pode quebrar a resolução OIDC do client registration do backend (Spring exige que o issuer-uri configurado bata exatamente com o campo issuer do discovery document). Vou testar com tudo de pé antes de dar como resolvido

Confirmado o efeito colateral: fixar KC_HOSTNAME quebrou a resolução OIDC do client registration do backend (que exige que o discovery document bata exatamente com o issuer-uri interno usado para alcançá-lo). O comportamento padrão do Keycloak já era o correto — o iss do token de um fluxo de autorização iniciado pelo browser (localhost:8080) fica travado nesse host durante todo aquele fluxo, mesmo que a troca de código seja feita internamente. Só a checagem do frontend estava errada. Revertendo o KC_HOSTNAME:



Resumo do que foi corrigido nesta investigação (além do escopo original de dockerização):

iss do id_token vs. issuer checado — verificarIdToken comparava contra o issuer interno; corrigido para usar o público (lib/oidc.ts).
Cookie de sessão > 4KB — descartado silenciosamente pela maioria dos browsers; removido idToken do cookie persistido (só é necessário no momento do login).
KC_HOSTNAME fixo no Keycloak — sem isso, introspection rejeitava tokens emitidos pelo fluxo do browser quando consultados via nome interno de rede.
Client-registration OIDC morto removido (SecurityConfig.java) — bloqueava o passo 3 (exigia discovery-issuer batendo com URL interna) e já era código sem uso real desde TASK-07.1.
Bug crítico de ordenação de filtro (AtivoUsuarioFilter rodava antes do BearerTokenAuthenticationFilter) — nenhuma chamada autenticada real jamais funcionou antes, mascarado porque nenhum teste exercitava esse caminho ponta a ponta.


Login OIDC + chamada autenticada real funcionando de ponta a ponta dentro do Docker
6 bugs pré-existentes corrigidos (o mais crítico: AtivoUsuarioFilter rodando antes da autenticação — nenhuma chamada autenticada real jamais teria funcionado, em nenhum ambiente)
165/165 testes unitários verdes, sem regressão
5 falhas de IT identificadas como pré-existentes e não relacionadas (confirmado via git stash)



```

14- não tinha botão de "novo projeto" nem rota no frontend.
15- não usou os protótipos como referencia para garantir que criou tudo.
16- o que faltou: criar fluxo mais flexível, foi feito somente saida e não retorno (n x n)

Fluxo ok: 
- funcionando melhor que antes
- maior profundidade
- exige maior atenção do techleader e do PO para não faltar nenhum requisito.


resultado na pasta segunda versao