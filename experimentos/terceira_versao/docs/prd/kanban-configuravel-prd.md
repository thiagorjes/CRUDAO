# PRD — kanban-configuravel
_Versão: 1.0 | Status: Draft | Data: 2026-08-27 | Autor: opencode_

---

## 1. Visão Geral

**Problema:** A equipe de desenvolvimento não tinha um jeito centralizado de acompanhar o andamento das tarefas — o status e os impedimentos eram reportados por email/chat e se perdiam no meio de outras mensagens, causando cobranças manuais e, em um caso concreto, uma demanda parada por 5 dias sem que ninguém visse o aviso de impedimento a tempo. O kanban-configuravel resolve isso com um board onde os próprios devs atualizam status/impedimentos em tempo real, com etapas configuráveis por projeto e lead-time visível — eliminando o cruzamento manual de reports, tanto para quem executa quanto para gestores que só acompanham.

**Solução proposta:** Um sistema de kanban com etapas configuráveis por projeto, onde os próprios desenvolvedores atualizam status e sinalizam impedimentos, com notificações automáticas de impedimento e lead-time visível por etapa (no board da tarefa) e agregado em dashboard.

**Público-alvo:** Equipe de Desenvolvimento (devs e liderança técnica) — desenvolvedores e líderes técnicos que executam e acompanham as atividades diariamente. Persona secundária: gestores de outros times que precisam de visibilidade de progresso, impedimentos e lead-time sem precisar atualizar dados.

---

## 2. Stakeholders

| Papel | Nome | Responsabilidade |
|-------|------|-----------------|
| Product Owner | — | Aprovação de requisitos |
| Tech Lead | — | Decisões técnicas |
| Usuário final | Equipe de Desenvolvimento | Validação de UX |

---

## 3. Requisitos Funcionais

### RF-001 — Board com colunas configuráveis por projeto

**Como** Desenvolvedor, **quero** visualizar o board do projeto com colunas na ordem configurada no workflow, **para** acompanhar o andamento das tarefas no fluxo real do meu projeto.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** um projeto possui um workflow com etapas definidas
**Quando** o usuário abre o board do projeto
**Então** o sistema exibe as colunas na ordem configurada, com as tarefas posicionadas na etapa atual

**Cenário 2 (Estado inesperado — projeto sem workflow/etapas definidas):**
**Dado que** um projeto não possui workflow configurado ou o workflow não tem etapas
**Quando** o usuário abre o board do projeto
**Então** o sistema exibe um estado vazio claro para o usuário, não um erro de sistema

**Cenário 3 (Permissão negada — usuário sem acesso ao projeto):**
**Dado que** um usuário não está associado ao projeto
**Quando** o usuário tenta abrir o board
**Então** o backend nega a consulta

**Cenário 4 (Configuração inconsistente do workflow):**
**Dado que** uma etapa fica sem nenhuma transição de saída configurada (fora da etapa final)
**Quando** o usuário abre o board
**Então** o board continua exibindo as colunas normalmente — a restrição afeta a movimentação da tarefa, não a exibição do board

**Cenário 5 (Projeto finalizado):**
**Dado que** o projeto foi marcado como finalizado
**Quando** o usuário abre o board
**Então** o board continua sendo exibido normalmente, com colunas e tarefas visíveis, mas em modo somente leitura — toda ação de escrita é bloqueada

**Prioridade:** Must Have

---

### RF-002 — Workflows com transições configuráveis entre etapas (n para n)

**Como** Desenvolvedor, **quero** mover tarefas apenas para etapas permitidas pelas transições do workflow, **para** respeitar o fluxo de trabalho definido no projeto.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** uma tarefa está em uma etapa com transições de saída configuradas
**Quando** o usuário inicia o movimento do card
**Então** o sistema destaca apenas as colunas de destino válidas conforme as transições permitidas

**Cenário 2 (Entrada inválida — movimento via API ignorando UI):**
**Dado que** um cliente tenta mover a tarefa para uma etapa sem transição permitida a partir da etapa atual
**Quando** a requisição chega ao backend
**Então** a operação é rejeitada com erro

**Cenário 3 (Permissão negada — usuário sem acesso de movimentação):**
**Dado que** um usuário sem vínculo ao projeto ou sem permissão para gerenciar tarefas tenta mover um card
**Quando** a ação é executada
**Então** a ação é negada pelo backend

**Cenário 4 (Estado inesperado — etapa sem transição de saída):**
**Dado que** uma etapa (não final) não possui nenhuma transição de saída configurada
**Quando** o usuário inicia o movimento da tarefa nessa etapa
**Então** nenhuma coluna fica destacada — a tarefa fica "presa" ali, e a interface deixa claro que nenhum destino está disponível

**Cenário 5 (Projeto finalizado):**
**Dado que** o projeto está em modo somente leitura
**Quando** o usuário tenta mover uma tarefa
**Então** o movimento é bloqueado — nenhuma coluna é destacada como destino possível

**Prioridade:** Must Have

---

### RF-003 — CRUD de tarefas

**Como** Desenvolvedor, **quero** criar, editar e excluir tarefas no board, **para** gerenciar meu trabalho diário.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** o usuário tem permissão no projeto
**Quando** cria, edita ou exclui uma tarefa no board
**Então** a alteração é refletida imediatamente no board para todos os usuários conectados

**Cenário 2 (Permissão negada — edição bloqueada por estado da tarefa):**
**Dado que** um dev tenta editar título/descrição/tipo de uma tarefa que já saiu da etapa inicial
**Quando** a ação é executada
**Então** a ação é bloqueada pelo backend, mesmo que o campo apareça editável na tela

**Cenário 3 (Entrada inválida — exclusão sem permissão):**
**Dado que** um usuário sem a permissão correspondente tenta excluir uma tarefa
**Quando** a ação é executada
**Então** o backend recusa a operação

**Cenário 4 (Estado inesperado — edição concorrente):**
**Dado que** dois usuários editam a mesma tarefa quase simultaneamente
**Quando** ambas as alterações chegam ao servidor
**Então** o sistema lida com isso sem quebrar ou perder atualização, refletindo a última alteração válida a todos conectados

**Cenário 5 (Projeto finalizado):**
**Dado que** o projeto foi marcado como finalizado
**Quando** qualquer usuário tenta criar, editar ou excluir tarefa
**Então** a ação é bloqueada para qualquer papel, até o projeto ser reaberto

**Prioridade:** Must Have

---

### RF-004 — Sinalização de impedimento

**Como** Desenvolvedor, **quero** marcar uma tarefa como impedida, **para** sinalizar que ela está parada e iniciar contagem de lead-time de impedimento.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** uma tarefa está em andamento
**Quando** o usuário marca a tarefa como impedida
**Então** o card exibe indicador visual de impedimento e o tempo em impedimento começa a ser contado separadamente

**Cenário 2 (Permissão negada — usuário sem permissão):**
**Dado que** um usuário sem permissão para marcar/desmarcar impedimento tenta realizar a ação
**Quando** a ação é executada
**Então** o backend recusa a operação

**Cenário 3 (Estado inesperado — tarefa já impedida):**
**Dado que** o usuário tenta marcar como impedida uma tarefa que já está impedida (ou desmarcar uma que já não está)
**Quando** a ação é executada
**Então** o sistema trata de forma idempotente/consistente, sem duplicar contagem de tempo

**Cenário 4 (Concorrência — dois usuários interagindo):**
**Dado que** um usuário marca a tarefa como impedida enquanto outro tenta desmarcar quase simultaneamente
**Quando** as ações são processadas
**Então** o resultado final reflete de forma consistente para todos conectados

**Cenário 5 (Projeto finalizado):**
**Dado que** o projeto está em modo somente leitura
**Quando** o usuário tenta marcar ou desmarcar impedimento
**Então** a ação é bloqueada para qualquer papel

**Prioridade:** Must Have

---

### RF-005 — Notificação de transições aos observadores

**Como** Observador de tarefa, **quero** receber notificação quando a tarefa transita de etapa, **para** acompanhar o andamento sem precisar olhar o board constantemente.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** um usuário está cadastrado como observador de uma tarefa
**Quando** a tarefa transita de uma etapa para outra
**Então** todos os observadores recebem uma notificação interna ao sistema

**Cenário 2 (Estado inesperado — tarefa sem observadores):**
**Dado que** uma tarefa transita de etapa sem nenhum observador cadastrado
**Quando** a transição ocorre
**Então** nenhuma notificação é gerada — não é um erro, apenas ausência de destinatário

**Cenário 3 (Entrada inválida — observador não cadastrado):**
**Dado que** alguém tenta adicionar como observador um usuário que não existe no sistema
**Quando** a associação é tentada
**Então** a operação é recusada

**Cenário 4 (Estado inesperado — observador removido durante movimentação):**
**Dado que** um usuário é removido da lista de observadores praticamente ao mesmo tempo em que a tarefa transita
**Quando** a transição é processada
**Então** o sistema decide de forma consistente se a notificação ainda é enviada

**Cenário 5 (Volume — múltiplos observadores simultâneos):**
**Dado que** uma tarefa tem vários observadores cadastrados
**Quando** a tarefa transita de etapa
**Então** todos recebem a notificação sem que a entrega para um dependa ou atrase a dos demais

**Prioridade:** Must Have

---

### RF-006 — Cálculo de lead-time por etapa e lead-time de impedimento

**Como** Gestor/Desenvolvedor, **quero** visualizar o tempo que a tarefa passou em cada etapa e em impedimento, **para** identificar gargalos no fluxo.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** uma tarefa passou por uma ou mais etapas com períodos de impedimento
**Quando** o usuário visualiza o detalhe da tarefa
**Então** o sistema exibe o tempo decorrido em cada etapa e separadamente o tempo em impedimento por etapa

**Cenário 2 (Estado inesperado — tarefa na primeira etapa):**
**Dado que** a tarefa nunca saiu da etapa inicial
**Quando** o usuário visualiza o detalhe
**Então** o sistema exibe o tempo decorrido nessa etapa até o momento atual, sem histórico anterior

**Cenário 3 (Estado inesperado — múltiplos períodos de impedimento):**
**Dado que** a tarefa foi marcada e desmarcada como impedida várias vezes na mesma etapa
**Quando** o usuário visualiza o detalhe
**Então** o tempo de impedimento exibido é a soma correta de todos os períodos

**Cenário 4 (Estado inesperado — tarefa retorna a etapa anterior):**
**Dado que** a tarefa passa novamente por uma etapa em que já esteve antes
**Quando** o usuário visualiza o detalhe
**Então** o sistema deixa claro se está somando ou tratando como registros separados

**Cenário 5 (Limite — impedimento ativo na consulta):**
**Dado que** o usuário visualiza a tarefa enquanto ela ainda está impedida
**Quando** o detalhe é exibido
**Então** o tempo de impedimento reflete o período parcial até o momento da consulta

**Prioridade:** Must Have

---

### RF-007 — Dashboard de gestão com lead-time médio, por etapa, impedido etc.

**Como** Gestor, **quero** visualizar lead-time médio por etapa e tempo médio em impedimento, **para** identificar gargalos sem abrir tarefa por tarefa.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** existem tarefas com histórico de lead-time
**Quando** o gestor acessa o dashboard e seleciona um período
**Então** o sistema exibe lead-time médio por etapa e tempo médio em impedimento, agregados por projeto

**Cenário 2 (Estado inesperado — nenhuma tarefa no período):**
**Dado que** o gestor escolhe um intervalo sem nenhuma tarefa com histórico
**Quando** o dashboard é carregado
**Então** exibe estado vazio claro por etapa, não erro nem números zerados sem explicação

**Cenário 3 (Entrada inválida — intervalo malformado):**
**Dado que** o usuário seleciona data final anterior à inicial
**Quando** a seleção é feita
**Então** o sistema rejeita/impede antes de disparar o cálculo

**Cenário 4 (Permissão negada — usuário sem acesso):**
**Dado que** usuário sem vínculo ao projeto tenta acessar o dashboard
**Quando** a consulta é feita
**Então** o backend nega a consulta

**Cenário 5 (Estado inesperado — cálculo assíncrono falha):**
**Dado que** o processamento não retorna a tempo ou falha
**Quando** o usuário aguarda o resultado
**Então** vê estado de espera/erro claro, sem travar a tela nem apresentar dado parcial

**Prioridade:** Must Have

---

### RF-008 — CRUD de projetos

**Como** Admin, **quero** criar, editar, finalizar e excluir projetos, **para** gerenciar o portfólio de projetos do sistema.

**Critérios de aceite:**

**Cenário 1 (caminho feliz — exclusão condicionada):**
**Dado que** um projeto não possui tarefas ativas
**Quando** o admin tenta excluí-lo
**Então** a exclusão é permitida

**Cenário 2 (Estado inesperado — exclusão com tarefas ativas):**
**Dado que** o admin tenta excluir projeto com tarefas ativas
**Quando** a exclusão é tentada
**Então** o sistema bloqueia e orienta migrar as tarefas antes

**Cenário 3 (Entrada inválida — campos obrigatórios ausentes):**
**Dado que** o usuário tenta salvar projeto sem nome
**Quando** o envio é feito
**Então** o sistema rejeita e sinaliza o campo pendente

**Cenário 4 (Estado inesperado — escrita em projeto finalizado):**
**Dado que** qualquer usuário tenta editar/excluir projeto finalizado
**Quando** a ação é executada
**Então** a ação é bloqueada até reabertura explícita

**Cenário 5 (Permissão negada — usuário sem papel admin):**
**Dado que** usuário sem papel admin tenta CRUD de projeto
**Quando** a ação é executada
**Então** o backend recusa

**Prioridade:** Must Have

---

### RF-009 — CRUD de workflows por projeto

**Como** Project Admin, **quero** criar, editar e excluir workflows do meu projeto, **para** definir o fluxo de trabalho adequado.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** o usuário tem permissão de gerenciar workflow no projeto
**Quando** cria, edita ou exclui um workflow
**Então** as mudanças são refletidas nas colunas e transições do board daquele projeto

**Cenário 2 (Permissão negada — usuário sem permissão):**
**Dado que** usuário sem permissão de gerenciar workflow tenta CRUD
**Quando** a ação é executada
**Então** o backend recusa

**Cenário 3 (Estado inesperado — exclusão de workflow em uso):**
**Dado que** o usuário tenta excluir workflow ativo com tarefas vinculadas
**Quando** a exclusão é tentada
**Então** o sistema bloqueia até que as tarefas sejam migradas

**Cenário 4 (Entrada inválida — workflow incompleto):**
**Dado que** o usuário tenta salvar workflow sem etapas ou com etapa não-final sem transição de saída
**Quando** o envio é feito
**Então** o sistema rejeita/sinaliza a inconsistência

**Cenário 5 (Estado inesperado — escrita em projeto finalizado):**
**Dado que** o usuário tenta CRUD de workflow em projeto finalizado
**Quando** a ação é executada
**Então** a ação é bloqueada até reabertura

**Prioridade:** Must Have

---

### RF-010 — CRUD de colunas (etapas) no board

**Como** Project Admin, **quero** criar, editar e excluir colunas do workflow, **para** representar as etapas reais do fluxo.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** o usuário tem permissão de gerenciar colunas
**Quando** cria, edita ou exclui uma coluna vazia
**Então** a mudança aparece no board imediatamente na ordem configurada

**Cenário 2 (Permissão negada — usuário sem permissão):**
**Dado que** usuário sem permissão tenta CRUD de coluna
**Quando** a ação é executada
**Então** o backend recusa

**Cenário 3 (Estado inesperado — exclusão de coluna com tarefas):**
**Dado que** o usuário tenta excluir coluna com tarefas posicionadas
**Quando** a exclusão é tentada
**Então** o sistema bloqueia e exige migração das tarefas

**Cenário 4 (Entrada inválida — edição que quebra transições):**
**Dado que** o usuário edita coluna deixando-a sem transição de saída (não sendo final)
**Quando** o envio é feito
**Então** o sistema sinaliza a inconsistência

**Cenário 5 (Estado inesperado — escrita em projeto finalizado):**
**Dado que** o usuário tenta CRUD de coluna em projeto finalizado
**Quando** a ação é executada
**Então** a ação é bloqueada até reabertura

**Prioridade:** Must Have

---

### RF-011 — CRUD de raias (swimlanes) no board

**Como** Project Admin, **quero** criar, editar e excluir raias do board, **para** organizar tarefas quando múltiplos devs atuam no mesmo contexto.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** o usuário tem permissão de gerenciar raias
**Quando** cria, edita ou exclui uma raia
**Então** a mudança reflete imediatamente na organização visual do board

**Cenário 2 (Permissão negada — usuário sem permissão):**
**Dado que** usuário sem permissão tenta CRUD de raia
**Quando** a ação é executada
**Então** o backend recusa

**Cenário 3 (Estado inesperado — exclusão de raia com tarefas):**
**Dado que** o usuário tenta excluir raia com tarefas posicionadas
**Quando** a exclusão é tentada
**Então** o sistema bloqueia e exige migração das tarefas

**Cenário 4 (Estado inesperado — exclusão de raia default global):**
**Dado que** o admin remove raia default global usada por múltiplos projetos
**Quando** a exclusão é feita
**Então** o sistema deixa claro o efeito nos demais projetos que dependem dela

**Cenário 5 (Estado inesperado — escrita em projeto finalizado):**
**Dado que** o usuário tenta CRUD de raia em projeto finalizado
**Quando** a ação é executada
**Então** a ação é bloqueada até reabertura

**Prioridade:** Must Have

---

### RF-012 — Etapa final com opção de reabertura

**Como** Product Owner, **quero** finalizar tarefas na etapa final e desfinalizá-las retornando a etapa anterior, **para** corrigir finalizações prematuras.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** o usuário tem permissão específica para finalizar/desfinalizar
**Quando** move tarefa para etapa final ou aciona desfinalizar escolhendo etapa de destino
**Então** a tarefa transita e a contagem de lead-time da etapa de retorno recomeça do zero

**Cenário 2 (Permissão negada — dev tentando finalizar/desfinalizar):**
**Dado que** um dev sem a permissão específica tenta mover para final ou desfinalizar
**Quando** a ação é executada
**Então** o backend recusa ambas as ações

**Cenário 3 (Entrada inválida — desfinalizar sem etapa de destino):**
**Dado que** o usuário aciona desfinalizar sem escolher etapa anterior
**Quando** a ação é tentada
**Então** o sistema exige a escolha antes de completar

**Cenário 4 (Entrada inválida — etapa de destino inexistente):**
**Dado que** o usuário seleciona etapa que não pertence mais ao workflow atual
**Quando** a desfinalização é confirmada
**Então** o sistema rejeita a seleção

**Cenário 5 (Estado inesperado — projeto finalizado):**
**Dado que** o projeto está em modo somente leitura
**Quando** o usuário tenta finalizar ou desfinalizar
**Então** a ação é bloqueada até reabertura do projeto

**Prioridade:** Must Have

---

### RF-013 — Controle de acesso por papéis configuráveis, escopados por projeto

**Como** Admin Global, **quero** criar e gerenciar papéis com permissões configuráveis por projeto, **para** controlar acesso granular sem permitir escalação de privilégios.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** o admin global cria papel e define permissões
**Quando** usuários são associados a projetos com esses papéis
**Então** o sistema aplica as permissões exatamente ao escopo do projeto

**Cenário 2 (Permissão negada — usuário delegado gerenciando papéis):**
**Dado que** project_admin tenta criar/editar/excluir papel ou alterar permissões do admin
**Quando** a ação é executada
**Então** o backend recusa, mesmo com acesso amplo no projeto

**Cenário 3 (Entrada inválida — papel duplicado ou sem permissões):**
**Dado que** admin global tenta criar papel com nome existente ou sem permissões
**Quando** o envio é feito
**Então** o sistema rejeita/sinaliza a inconsistência

**Cenário 4 (Estado inesperado — usuário sem papel no projeto):**
**Dado que** usuário autenticado sem vínculo no projeto tenta ação restrita
**Quando** a ação é executada
**Então** o backend nega, mesmo que tenha papel em outros projetos

**Cenário 5 (Limite — exclusão de papel em uso):**
**Dado que** admin global tenta excluir papel atribuído a usuários ativos
**Quando** a exclusão é tentada
**Então** o sistema aplica comportamento consistente (bloquear ou remover atribuição)

**Prioridade:** Must Have

---

### RF-014 — Associação de usuário a projeto(s) com papel(is)

**Como** Admin/Project Admin, **quero** associar usuários existentes a projetos com um ou mais papéis, **para** conceder acesso contextualizado.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** usuário existe no sistema (autoprovisionado no 1º login)
**Quando** admin global ou project_admin o associa a projeto com papel(is)
**Então** o usuário acumula permissões de todos os papéis naquele projeto

**Cenário 2 (Permissão negada — project_admin associando a outro projeto):**
**Dado que** project_admin tenta associar usuário a projeto onde não é project_admin
**Quando** a ação é executada
**Então** o backend recusa — escopo restrito aos seus projetos

**Cenário 3 (Entrada inválida — usuário/papel inexistente):**
**Dado que** admin tenta associar usuário que nunca fez login ou papel inexistente
**Quando** a associação é tentada
**Então** o sistema rejeita

**Cenário 4 (Estado inesperado — associação duplicada):**
**Dado que** admin atribui novamente papel que usuário já tem no projeto
**Quando** a ação é executada
**Então** o sistema trata de forma idempotente, sem duplicar

**Cenário 5 (Estado inesperado — remoção de último papel):**
**Dado que** admin remove o único papel do usuário no projeto
**Quando** a remoção é confirmada
**Então** o usuário perde completamente acesso àquele projeto

**Prioridade:** Must Have

---

### RF-015 — Configuração de permissões por projeto (toggles)

**Como** Project Admin, **quero** ligar/desligar toggles de permissão pré-definidos no meu projeto, **para** ajustar comportamento padrão de papéis sem criar novas permissões RBAC.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** o project_admin altera um toggle no seu projeto
**Quando** a configuração é salva
**Então** o comportamento default do papel muda imediatamente naquele projeto, isolado dos demais

**Cenário 2 (Permissão negada — usuário sem project_admin):**
**Dado que** usuário sem project_admin tenta alterar toggles
**Quando** a ação é executada
**Então** o backend recusa

**Cenário 3 (Entrada inválida — toggle inexistente):**
**Dado que** usuário tenta manipular chave fora do conjunto pré-definido
**Quando** a ação é tentada
**Então** o sistema rejeita — conjunto não é extensível

**Cenário 4 (Estado inesperado — mudança concorrente):**
**Dado que** toggle é desligado enquanto ação dependente dele está em processamento
**Quando** a operação chega ao servidor
**Então** o sistema decide de forma consistente qual valor vale para a ação

**Cenário 5 (Estado inesperado — projeto finalizado):**
**Dado que** project_admin tenta alterar toggle em projeto finalizado
**Quando** a ação é executada
**Então** a ação é bloqueada até reabertura

**Prioridade:** Must Have

---

### RF-016 — Histórico de auditoria da tarefa

**Como** Gestor/Desenvolvedor, **quero** consultar histórico de alterações relevantes da tarefa, **para** rastrear quem fez o quê e quando.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** uma tarefa sofreu alterações (responsável, título/descrição, etapa)
**Quando** o usuário consulta o histórico
**Então** exibe lista cronológica com autor, campo, valor anterior/novo e timestamp

**Cenário 2 (Estado inesperado — tarefa recém-criada):**
**Dado que** tarefa acabou de ser criada sem edições
**Quando** o histórico é consultado
**Então** exibe histórico vazio claro, não erro

**Cenário 3 (Permissão negada — usuário sem acesso):**
**Dado que** usuário sem vínculo ao projeto tenta consultar histórico
**Quando** a consulta é feita
**Então** o backend nega

**Cenário 4 (Volume — histórico extenso):**
**Dado que** tarefa antiga com muitas alterações ao longo do tempo
**Quando** o histórico é consultado
**Então** continua acessível com paginação/ordenação adequada

**Cenário 5 (Estado inesperado — alterações simultâneas):**
**Dado que** duas mudanças na mesma tarefa ocorrem em sequência rápida por usuários distintos
**Quando** o histórico é consultado
**Então** cada uma gera sua entrada com autor e timestamp corretos, sem mesclar

**Prioridade:** Must Have

---

### RF-017 — Criar card pelo board

**Como** Desenvolvedor, **quero** criar card direto no board com título obrigatório, **para** adicionar trabalho rapidamente sem sair da visualização do fluxo.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** usuário tem permissão tarefa:gerenciar no projeto
**Quando** clica em "Novo card", preenche título (obrigatório), descrição, tipo e salva
**Então** card aparece imediatamente no board na etapa/raia padrão, sem responsável, com feedback de sucesso

**Cenário 2 (Entrada inválida — título não preenchido):**
**Dado que** usuário tenta salvar sem preencher título
**Quando** o envio é feito
**Então** o sistema bloqueia e sinaliza o campo pendente

**Cenário 3 (Permissão negada — sem tarefa:gerenciar):**
**Dado que** usuário sem permissão tenta criar (ou ação via outra via)
**Quando** a ação é executada
**Então** o backend recusa

**Cenário 4 (Estado inesperado — projeto finalizado):**
**Dado que** usuário tenta criar card em projeto finalizado
**Quando** a ação é executada
**Então** bloqueado pelo backend, mesmo com permissão

**Cenário 5 (Estado inesperado — projeto sem workflow/raia):**
**Dado que** projeto não tem workflow com etapas ou nenhuma raia disponível
**Quando** usuário tenta criar card
**Então** sistema lida explicitamente com ausência de destino padrão

**Prioridade:** Must Have

---

### RF-018 — Excluir card pelo board

**Como** Desenvolvedor, **quero** excluir card pelo board com confirmação, **para** remover trabalho incorreto preservando histórico.

**Critérios de aceite:**

**Cenário 1 (caminho feliz):**
**Dado que** usuário tem permissão tarefa:gerenciar (e toggle se dev-tier)
**Quando** clica na lixeira, confirma no modal
**Então** card é removido do board, histórico preservado

**Cenário 2 (Permissão negada — dev com toggle desabilitado):**
**Dado que** dev-tier com toggle devPodeExcluirTarefa desabilitado
**Quando** tenta excluir (mesmo via outra via)
**Então** backend recusa

**Cenário 3 (Estado inesperado — card já excluído por outro):**
**Dado que** usuário abre modal mas outro já excluiu o card
**Quando** confirma a exclusão
**Então** sistema trata de forma segura, sem quebrar a tela

**Cenário 4 (Estado inesperado — projeto finalizado):**
**Dado que** usuário tenta excluir em projeto finalizado
**Quando** a ação é executada
**Então** bloqueado pelo backend

**Cenário 5 (Cancelamento — desiste no modal):**
**Dado que** usuário abre modal mas clica cancelar/fecha
**Quando** o modal é fechado
**Então** nenhuma alteração ocorre, card permanece intacto

**Prioridade:** Must Have

---

## 4. Requisitos Não-Funcionais

### RNF-001 — Atualização em tempo real

**Categoria:** Performance

**Métrica:** Tempo de propagação de alterações

**Critério:** Alterações (ex.: mover card) devem ser refletidas para todos os usuários conectados ao mesmo board em até 2 segundos, sem necessidade de refresh manual.

---

### RNF-002 — Escalabilidade horizontal sem inconsistência

**Categoria:** Performance

**Métrica:** Número de instâncias simultâneas / usuários concorrentes

**Critério:** Sistema deve operar corretamente com 1 pod e escalar horizontalmente para 2+ pods simultâneos, suportando dezenas a centenas de usuários simultâneos, sem gerar inconsistência de dados entre instâncias.

---

### RNF-003 — Autenticação e autorização

**Categoria:** Segurança

**Métrica:** Cobertura de endpoints protegidos / tempo de resposta de auth

**Critério:** 100% das ações de escrita sensíveis passam por checagem explícita de permissão no backend; canal WebSocket exige autenticação na conexão; fallback de login local para continuidade caso Keycloak indisponível.

---

### RNF-004 — Auditoria e rastreabilidade

**Categoria:** Segurança

**Métrica:** Cobertura de eventos auditados

**Critério:** Toda alteração relevante em tarefas (edição, atribuição, finalização, movimentação) gera registro de auditoria append-only com autor, campo, valor anterior/novo, timestamp.

---

### RNF-005 — Integridade de fluxo de negócio

**Categoria:** Confiabilidade

**Métrica:** Taxa de operações rejeitadas por violação de regra

**Critério:** Projetos finalizados bloqueiam escrita para todos os papéis; regras de movimentação, edição, atribuição e finalização validadas exclusivamente no servidor; admin global é único que pode gerenciar papéis/permissões.

---

## 5. Regras de Negócio

| ID | Regra | Origem |
|----|-------|--------|
| RN-001 | Tarefa só avança para etapas seguintes seguindo transições configuradas no workflow | RF-002 |
| RN-002 | Reabertura (mover para etapa anterior) é ação distinta com permissão própria | RF-012 |
| RN-003 | Tarefa impedida continua bloqueada independentemente da etapa — impedimento é condição própria | RF-004 |
| RN-004 | Mover tarefa entre projetos exige permissão de gerenciar tarefas em ambos (origem e destino) | RF-003 |
| RN-005 | Lead-time por etapa calculado automaticamente; múltiplos períodos de impedimento somados | RF-006 |
| RN-006 | Tempo médio por etapa (dashboard) processado assincronamente | RF-007 |
| RN-007 | Após tarefa sair da etapa inicial pela 1ª vez, título/descrição travados para edição por certos papéis — salvo se toggle do projeto liberar | RF-003 |
| RN-008 | Autoatribuição permitida amplamente; atribuir a terceiro exige permissão específica | RF-003 |
| RN-009 | Finalizar tarefa (mover para etapa final ou desfinalizar) exige permissão dedicada distinta de gerenciar tarefas | RF-012 |
| RN-010 | Projeto finalizado bloqueia qualquer escrita, exceto ação de reabertura com permissão elevada | RF-008 |
| RN-011 | Cada projeto tem toggles próprios que alteram comportamento de regras (ex.: dev exclui, dev edita iniciada, gestor vê board) | RF-015 |
| RN-012 | Raias podem ser específicas do projeto ou usar default global quando projeto não define as suas | RF-011 |
| RN-013 | Papéis acumuláveis por usuário e por projeto — mesma pessoa pode ter papéis diferentes em projetos diferentes | RF-013, RF-014 |
| RN-014 | Gerenciar papéis/permissões nunca concedido a papel de projeto — só admin global | RF-013 |
| RN-015 | Criação de card exige workflow com ao menos uma etapa; etapa/raia padrão determinadas automaticamente | RF-017 |
| RN-016 | Exclusão de card preserva histórico (lead-time, impedimentos, auditoria) — remove apenas da visualização do board | RF-018 |
| RN-017 | Permissão para excluir tarefa pode ser restringida por toggle do projeto dependendo do papel | RF-018 |

---

## 6. Casos de Uso

### UC-001 — Gerenciar board de projeto

**Ator:** Desenvolvedor / Project Admin
**Fluxo principal:**
1. Usuário acessa board do projeto
2. Visualiza colunas na ordem do workflow
3. Move cards entre etapas permitidas (drag ou ação)
4. Marca/desmarca impedimentos
5. Cria/exclui cards (se tiver permissão)
6. Alterações refletidas em tempo real para todos conectados

**Fluxo alternativo:** Projeto finalizado — board em modo somente leitura; ações de escrita bloqueadas.

---

### UC-002 — Configurar workflow do projeto

**Ator:** Project Admin
**Fluxo principal:**
1. Acessa configuração de workflow do projeto
2. Cria/edita/exclui workflow, etapas (colunas) e transições
3. Define etapa final
4. Mudanças refletidas imediatamente no board

**Fluxo alternativo:** Workflow em uso com tarefas — exclusão bloqueada até migração.

---

### UC-003 — Gerenciar projeto

**Ator:** Admin
**Fluxo principal:**
1. Cria projeto com nome obrigatório
2. Associa usuários com papéis
3. Configura toggles de permissão
4. Finaliza projeto (bloqueia escrita)
5. Reabre projeto (restaura escrita)

**Fluxo alternativo:** Exclusão de projeto com tarefas ativas bloqueada até migração.

---

### UC-004 — Visualizar dashboard de gestão

**Ator:** Gestor
**Fluxo principal:**
1. Acessa dashboard do projeto
2. Seleciona intervalo de datas
3. Visualiza lead-time médio por etapa e tempo médio em impedimento
4. Identifica gargalos sem abrir tarefas individuais

**Fluxo alternativo:** Intervalo sem dados — estado vazio claro; cálculo assíncrono falha — estado de erro claro.

---

### UC-005 — Consultar histórico de auditoria

**Ator:** Gestor / Desenvolvedor
**Fluxo principal:**
1. Acessa detalhe da tarefa
2. Abre aba de histórico
3. Visualiza lista cronológica de alterações com autor, campo, antes/depois, timestamp

**Fluxo alternativo:** Tarefa sem histórico — estado vazio claro; histórico extenso — paginação.

---

## 7. Restrições e Premissas

**Restrições:**
- Stack fixa: Java/Spring Boot (backend), Next.js (frontend), PostgreSQL (único armazenamento)
- Sem cache ou message broker externo — notificação em tempo real via LISTEN/NOTIFY do PostgreSQL
- Autenticação principal via Keycloak (OIDC/OAuth2) com fallback local
- Aplicação containerizada (Docker), orquestração docker-compose
- Multi-pod: backend deve funcionar corretamente com múltiplas instâncias simultâneas
- Compliance regulatório (LGPD, SOC2) não tratado como requisito explícito nesta fase

**Premissas:**
- Usuários existem no sistema via autoprovisionamento no 1º login (Keycloak)
- Inscrição em canal de tempo real não restrita a membros do projeto (débito conhecido)
- Métricas de cobertura de teste automatizada fora de escopo — qualidade por revisão qualitativa
- Compatibilidade de navegador/dispositivo não especificada formalmente

---

## 8. Dependências

| Dependência | Tipo | Impacto |
|-------------|------|---------|
| Keycloak (provedor identidade) | Técnica | Login principal; indisponibilidade aciona fallback local |
| PostgreSQL | Técnica | Armazenamento único + LISTEN/NOTIFY para tempo real |
| Docker / docker-compose | Técnica | Ambiente de execução e deploy |

---

## 9. Critérios de Sucesso (KPIs)

| KPI | Meta | Prazo |
|-----|------|-------|
| Reduzir tempo de execução e impedimento das atividades | Qualitativo — sem meta numérica definida | — |
| Eliminar comunicação dispersa sobre status/impedimentos | Qualitativo — sem meta numérica definida | — |
| Dar visibilidade de andamento e lead-time aos gestores | Qualitativo — sem meta numérica definida | — |
| Propagação de alterações em tempo real | ≤ 2 segundos | Contínuo |
| Zero inconsistência entre instâncias em multi-pod | 0 ocorrências | Contínuo |

---

## 10. Fora do Escopo

- Cache ou message broker externo (Redis, RabbitMQ, Kafka etc.)
- Compliance regulatório formal (LGPD, SOC2, etc.)
- Restringir inscrição no canal de tempo real apenas a membros do projeto (débito conhecido)
- Chaveamento automático para login local ao detectar indisponibilidade do Keycloak
- Métricas avançadas de dashboard além de lead-time médio e tempo médio em impedimento por etapa
- Medição automatizada de cobertura de teste (ferramenta no pipeline)
- Edição de campos do card além do necessário para criação (pertence a módulo de detalhe de tarefa)
- Tela de administração/configuração de projeto no módulo criação-card-board
- Escolha manual de etapa ou raia na criação de card
- Recuperação/restauração de card excluído pela interface (lixeira/desfazer)
- Suporte a múltiplas organizações/clientes
- Integração com sistemas externos de notificação (email, Slack, etc.)
- Controle de horas/timesheet do desenvolvedor

---

## 11. Histórico de Revisões

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-08-27 | opencode | Versão inicial |