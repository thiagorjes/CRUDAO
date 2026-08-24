# PRD — Kanban Configurável
_Versão: 1.3 | Status: Draft | Data: 2026-08-23 | Autor: Thiago Goncalves Cavalcante_

---

## 1. Visão Geral

**Problema:** A equipe de desenvolvimento não tem forma centralizada de controlar o andamento das atividades. Reports manuais via email/chat causam perda de mensagens (ex.: impedimento perdido em spam), atrasando entregas e exigindo cobrança e cruzamento manual de informação.

**Solução proposta:** Sistema de kanban com workflows e etapas configuráveis por projeto, onde devs atualizam status e impedimentos das tarefas, com notificação a observadores em transições, cálculo de lead-time (total e em impedimento) por etapa, e dashboard de gestão com lead-time médio. Controle de acesso por papéis configuráveis.

**Público-alvo:** Equipe de desenvolvimento (uso diário, atualização recorrente) e gestores de outros times (consulta, sem atualização).

---

## 2. Stakeholders

| Papel | Nome | Responsabilidade |
|-------|------|-----------------|
| Product Owner | Thiago Goncalves Cavalcante | Aprovação de requisitos |
| Tech Lead | — | Decisões técnicas |
| Usuário final | Equipe de desenvolvimento e gestores | Validação de UX |

---

## 3. Requisitos Funcionais

### RF-001 — Board com colunas configuráveis por projeto

**Como** membro da equipe, **quero** visualizar um board com colunas configuráveis por projeto, **para** acompanhar o andamento das tarefas conforme o fluxo específico daquele projeto.

**Critérios de aceite:**

**Dado que** um projeto possui um workflow com etapas definidas
**Quando** o usuário abre o board do projeto
**Então** o sistema exibe as colunas na ordem configurada, com as tarefas posicionadas na etapa atual.

**Prioridade:** Must Have

---

### RF-002 — Workflows com transições configuráveis entre etapas

**Como** admin/responsável pelo projeto, **quero** definir quais transições entre etapas são permitidas, **para** garantir que o fluxo de trabalho do projeto seja respeitado.

**Critérios de aceite:**

**Dado que** um workflow define transições permitidas entre etapas
**Quando** o usuário tenta mover uma tarefa
**Então** apenas as colunas com transição permitida a partir da etapa atual são destacadas com uma cor diferente, indicando que o movimento é possível.

**Prioridade:** Must Have

---

### RF-003 — CRUD de tarefas

**Como** membro da equipe, **quero** criar, editar e excluir tarefas, **para** manter o board refletindo o trabalho real.

**Critérios de aceite:**

**Dado que** o usuário está no board de um projeto
**Quando** cria, edita ou exclui uma tarefa
**Então** a alteração é refletida imediatamente no board para todos os usuários conectados.

**Dado que** uma tarefa ainda não saiu da etapa inicial do workflow ("não iniciada")
**Quando** um dev edita título/descrição/tipo dessa tarefa
**Então** a edição é permitida; **uma vez que a tarefa saia da etapa inicial pela primeira vez** ("iniciada", RN-013), o dev deixa de poder editar título/descrição/tipo — só movimentação de etapa (avançar/retroceder) continua disponível a ele. Product_owner, project_admin e admin não têm essa restrição em nenhum momento.

**Dado que** o comportamento default de edição/exclusão por papel (RN-013, RN-014) está em vigor num projeto
**Quando** o project_admin daquele projeto configura os toggles de permissão do projeto (RF-016)
**Então** o comportamento passa a seguir a configuração definida ali, sobrepondo o default.

**Prioridade:** Must Have

---

### RF-004 — Sinalização de impedimento

**Como** desenvolvedor, **quero** marcar/desmarcar uma tarefa como impedida, **para** que o impedimento seja visível sem depender de comunicação externa.

**Critérios de aceite:**

**Dado que** uma tarefa está em andamento
**Quando** o usuário marca a tarefa como impedida
**Então** o card exibe um ícone de "semáforo" vermelho (ou artefato visual equivalente definido pelo design), e o estado de impedimento passa a ser contabilizado no lead-time de impedimento da etapa atual. O impedimento não bloqueia nem libera movimentação por si só — a possibilidade de mover a tarefa continua regida exclusivamente pelas transições definidas no workflow do board.

**Prioridade:** Must Have

---

### RF-005 — Notificação de transições aos observadores

**Como** usuário marcado como observador de uma tarefa, **quero** ser notificado quando a tarefa mudar de etapa, **para** acompanhar o andamento sem precisar consultar o board manualmente.

**Critérios de aceite:**

**Dado que** uma tarefa possui uma lista de observadores (usuários cadastrados)
**Quando** a tarefa transita de uma etapa para outra
**Então** todos os observadores da tarefa recebem uma notificação (interna ao sistema, sem integração externa nesta versão).

**Prioridade:** Must Have

---

### RF-006 — Cálculo de lead-time por etapa e lead-time de impedimento

**Como** gestor, **quero** ver o lead-time de cada tarefa por etapa e o tempo total em impedimento, **para** identificar gargalos no fluxo.

**Critérios de aceite:**

**Dado que** uma tarefa passou por uma ou mais etapas, incluindo períodos em impedimento
**Quando** o usuário visualiza a tarefa
**Então** o sistema exibe o tempo decorrido em cada etapa (da entrada até a saída) e uma observação do tempo em impedimento durante aquela etapa.

**Prioridade:** Must Have

---

### RF-007 — Dashboard de gestão com lead-time médio

**Como** gestor, **quero** visualizar um dashboard com o lead-time médio por etapa e o tempo total médio em impedimento, **para** ter visibilidade do andamento sem precisar entrar em cada tarefa.

**Critérios de aceite:**

**Dado que** existem tarefas com histórico de lead-time registrado
**Quando** o gestor acessa o dashboard e seleciona um período (intervalo de datas configurável)
**Então** o sistema exibe o lead-time médio por etapa e o tempo médio em impedimento, agregados por projeto, considerando apenas o período selecionado.

**Prioridade:** Must Have

---

### RF-008 — CRUD de projetos

**Como** admin, **quero** criar, editar e excluir projetos, **para** organizar o trabalho de cada equipe/contexto separadamente.

**Critérios de aceite:**

**Dado que** um projeto não possui tarefas ativas
**Quando** o admin tenta excluí-lo
**Então** a exclusão é permitida; **caso contrário**, o sistema bloqueia a exclusão e exige migração das tarefas antes.

**Dado que** um projeto está ativo
**Quando** admin global ou project_admin daquele projeto marca o projeto como finalizado (preenche a data de finalização)
**Então** o projeto (board, tarefas e configurações) passa a somente leitura para todos os papéis, inclusive admin/project_admin; reabrir (limpar a data de finalização) segue a mesma permissão (RN-015).

**Prioridade:** Must Have

---

### RF-009 — CRUD de workflows por projeto

**Como** admin/responsável pelo projeto, **quero** criar, editar e excluir workflows dentro de um projeto, **para** adaptar o fluxo às necessidades daquele contexto.

**Critérios de aceite:**

**Dado que** um workflow está associado a um projeto
**Quando** o usuário com permissão cria, edita ou exclui o workflow
**Então** a mudança é refletida nas colunas e transições disponíveis no board do projeto.

**Prioridade:** Must Have

---

### RF-010 — CRUD de colunas (etapas) no board

**Como** admin/responsável pelo projeto, **quero** criar, editar e excluir colunas do board, **para** representar as etapas reais do fluxo de trabalho.

**Critérios de aceite:**

**Dado que** uma coluna não possui tarefas
**Quando** o usuário tenta excluí-la
**Então** a exclusão é permitida; **caso contrário**, o sistema bloqueia a exclusão e exige migração das tarefas antes.

**Prioridade:** Must Have

---

### RF-011 — CRUD de raias (swimlanes) no board

**Como** admin/responsável pelo projeto, **quero** criar, editar e excluir raias horizontais no board, **para** organizar tarefas quando mais de um desenvolvedor atua no mesmo projeto.

**Critérios de aceite:**

**Dado que** raias podem ser configuradas livremente por projeto ou definidas como raias default (globais)
**Quando** um projeto não define raias próprias
**Então** o board exibe as raias default globais, as quais podem ser mantidas, editadas ou removidas pelo administrador do projeto (admin ou usuário delegado).

**Prioridade:** Must Have

---

### RF-012 — Etapa final com opção de reabertura

**Como** membro da equipe, **quero** poder retornar uma tarefa finalizada por engano para outra etapa, **para** corrigir o fluxo sem perder o histórico.

**Critérios de aceite:**

**Dado que** uma tarefa está na etapa final (sem transição de saída configurada)
**Quando** o usuário aciona a opção de "desfinalizar"
**Então** a tarefa retorna para uma etapa anterior selecionada, reiniciando a contagem de lead-time daquela etapa. Tanto mover PARA a etapa final quanto "desfinalizar" exigem a permissão `tarefa:finalizar` (RN-011) — dev não tem essa permissão por padrão.

**Prioridade:** Must Have

---

### RF-013 — Controle de acesso por papéis configuráveis, escopados por projeto

**Como** admin, **quero** criar/editar/excluir papéis e definir quais permissões cada um possui, **para** adaptar o controle de acesso à estrutura da equipe (dev, gestor etc.).

**Critérios de aceite:**

**Dado que** existem papéis padrão (admin, user, project_admin, dev, product_owner, gestor)
**Quando** o admin global cria um novo papel e define suas permissões
**Então** o sistema passa a aplicar essas permissões aos usuários com esse papel, sem permitir que o papel admin seja criado, editado ou excluído por outro papel delegado (RN-006). Criar/editar/excluir papéis e suas permissões é exclusivo do admin global — project_admin não tem essa capacidade (RN-008). O conjunto granular de permissões disponíveis será definido tecnicamente em techspec.

**Dado que** o papel `admin` é global (não vinculado a nenhum projeto)
**Quando** um usuário tem o papel admin
**Então** ele tem acesso total a todas as funções em qualquer projeto do sistema, incluindo administrar usuários (RF-015) e o RBAC (papéis/permissões).

**Papéis padrão e suas capacidades default** (granularidade final de permissões em techspec):

| Papel | Escopo | Capacidades default |
|---|---|---|
| admin | Global | Acesso total a tudo, em todos os projetos; administra usuários, papéis e permissões |
| project_admin | Por projeto | Acesso total às funções do(s) projeto(s) associado(s); associa usuários existentes ao projeto e atribui papéis já existentes a eles; configura os toggles de permissão do projeto (RF-016); finaliza/reabre o projeto (RF-008) |
| product_owner | Por projeto | Gerencia qualquer tarefa do projeto (criar/editar/excluir, mesmo depois de iniciada); atribui/reatribui tarefas a devs; marca/desmarca impedimento; executa transições para/da etapa final (`tarefa:finalizar`, RN-011) |
| dev | Por projeto | Cria tarefa; edita título/descrição/tipo só enquanto não iniciada (RN-013); movimenta tarefas (avançar/retroceder) respeitando o workflow; marca/desmarca impedimento; autoatribui (puxa) qualquer tarefa do projeto para si, mesmo já atribuída a outro (RN-012); não atribui tarefa a outro usuário; não executa transição para/da etapa final |
| gestor | Por projeto | Por padrão, só visualiza o dashboard do projeto; sem acesso ao board (configurável pelo project_admin via RF-016) |
| user | Global (legado) | Sem permissões — papel padrão herdado, mantido por compatibilidade (RN-014) |

**Prioridade:** Must Have

---

### RF-015 — Associação de usuário a projeto(s) com papel(is)

**Como** admin global ou project_admin, **quero** associar um usuário a um ou mais projetos, atribuindo um ou mais papéis por projeto, **para** que ele acumule as permissões correspondentes naquele contexto.

**Critérios de aceite:**

**Dado que** um usuário já existe no sistema (autoprovisionado no 1º login via Keycloak — RF-014)
**Quando** admin global ou project_admin do projeto o associa a um projeto com um ou mais papéis (ex.: dev + product_owner)
**Então** o usuário passa a acumular as permissões de todos os papéis atribuídos a ele naquele projeto; o mesmo usuário pode ter papéis diferentes em projetos diferentes (ex.: project_admin no Projeto A, dev no Projeto B).

**Dado que** um usuário já existe no sistema
**Quando** admin global edita seus dados (nome, papel global admin/user, projetos associados)
**Então** a alteração é aplicada; não há tela de pré-cadastro de usuário antes do 1º login (autoprovisionamento via Keycloak continua sendo a única forma de criar a conta).

**Prioridade:** Must Have

---

### RF-016 — Configuração de permissões por projeto (toggles)

**Como** project_admin, **quero** ligar/desligar regras pré-definidas de permissão específicas do meu projeto, **para** adaptar o comportamento default dos papéis às necessidades do meu contexto, sem precisar mexer em RBAC granular.

**Critérios de aceite:**

**Dado que** existe um conjunto fechado de toggles pré-definidos pelo sistema (ex.: "dev pode excluir tarefa", "dev pode editar tarefa já iniciada", "gestor pode ver o board em modo leitura")
**Quando** o project_admin liga ou desliga um toggle do seu projeto
**Então** o comportamento default daquele papel no projeto passa a respeitar a configuração definida, sem afetar outros projetos nem criar novas permissões RBAC.

**Prioridade:** Must Have

---

### RF-017 — Histórico de auditoria da tarefa

**Como** membro da equipe, **quero** ver um histórico de alterações relevantes de uma tarefa (responsável, título, descrição, etapa), **para** entender quem fez o quê e quando.

**Critérios de aceite:**

**Dado que** uma tarefa sofre uma alteração relevante (troca de responsável, edição de título/descrição, mudança de etapa)
**Quando** o usuário consulta o histórico da tarefa
**Então** o sistema exibe cada alteração com quem a fez, o que mudou (de/para), e quando (ex.: "JOAO alterou o responsável da tarefa de PEDRO para JOAO em 23/08/2026 14:32").

**Prioridade:** Must Have

---

### RF-014 — Login via SSO (Keycloak)

**Como** usuário, **quero** autenticar via SSO (Keycloak), **para** não precisar gerenciar credenciais separadas.

**Critérios de aceite:**

**Dado que** o Keycloak está configurado como provedor de identidade
**Quando** o usuário acessa o sistema
**Então** ele é autenticado via SSO sem necessidade de cadastro de senha local.

**Prioridade:** Should Have

---

## 4. Requisitos Não-Funcionais

### RNF-001 — Atualização em tempo real

**Categoria:** Performance

**Métrica:** Tempo entre uma alteração (ex.: mover card) e sua exibição para outros usuários conectados ao mesmo board.

**Critério:** Alterações devem ser refletidas para todos os usuários conectados em até 2 segundos, sem necessidade de refresh manual.

---

### RNF-002 — Escalabilidade horizontal sem inconsistência

**Categoria:** Confiabilidade

**Métrica:** Número de usuários simultâneos e de instâncias (pods) suportadas sem divergência de estado entre usuários.

**Critério:** O sistema deve operar corretamente com 1 pod e escalar horizontalmente para 2+ pods simultâneos, suportando de dezenas a centenas de usuários simultâneos, sem gerar inconsistência de dados entre instâncias.

---

### RNF-003 — Controle de acesso por papel

**Categoria:** Segurança

**Métrica:** Cobertura de ações restritas por permissão de papel, validadas no backend.

**Critério:** Toda ação de criação/edição/exclusão de entidades administrativas (projetos, workflows, colunas, raias, papéis, associações usuário-projeto) deve respeitar as permissões do(s) papel(is) do usuário autenticado no projeto em questão. Toda validação de permissão exibida/aplicada no frontend (esconder botão, desabilitar ação) deve ser re-validada de forma independente no backend — o frontend nunca é a única salvaguarda; nenhuma ação de escrita pode depender apenas de dado enviado pelo cliente para decidir autorização.

---

### RNF-004 — Portabilidade via containers

**Categoria:** Portabilidade

**Métrica:** Capacidade de execução em ambiente containerizado.

**Critério:** O sistema deve ser empacotado e executável em containers (Docker) e orquestrável em OpenShift/Kubernetes.

---

### RNF-005 — Responsividade e compatibilidade de navegador

**Categoria:** Usabilidade

**Métrica:** Renderização correta em resoluções desktop e nos principais navegadores.

**Critério:** A interface deve ser responsiva para uso em desktop e funcionar em qualquer navegador moderno.

---

## 5. Regras de Negócio

| ID | Regra | Origem |
|----|-------|--------|
| RN-001 | O lead-time de uma etapa conta da entrada até a saída da tarefa naquela etapa. | Entrevista PRD |
| RN-002 | O tempo em que uma tarefa fica marcada como impedida é registrado separadamente e somado ao lead-time de impedimento da etapa e ao total, exibido no dashboard de gestão. | Entrevista PRD |
| RN-003 | Toda etapa deve ter ao menos uma transição de saída configurada, exceto a etapa final. Uma coluna pode ou não ter transição de entrada/saída conforme o workflow (atributo opcional, detalhado em techspec). | Entrevista PRD |
| RN-004 | A etapa final não possui transição de saída padrão, mas permite "desfinalizar" a tarefa, retornando-a para outra etapa do workflow. | Entrevista PRD |
| RN-005 | Não é permitido excluir um projeto, workflow, coluna ou raia que possua tarefas ativas vinculadas — é necessário migrar as tarefas antes. | Entrevista PRD |
| RN-006 | O admin pode delegar a outros papéis a permissão de criar/editar/excluir papéis e permissões, exceto a permissão de criar, editar ou excluir o papel admin. **Superseded em parte por RN-008 (v1.3, achado do comitê de análise técnica da TechSpec v1.1):** com RBAC por projeto (BDR-001), não existe mais um "papel delegado" com alcance global — `papel:gerenciar` deixa de ser uma permissão atribuível via `UsuarioProjetoPapel` (nenhum papel de projeto, incluindo `project_admin`, pode gerenciar papéis/permissões) e passa a ser checada exclusivamente contra `Usuario.admin`. Na prática, a delegação descrita nesta regra não tem mais um portador possível além do próprio admin global — mantida como registro histórico da intenção original, sem efeito prático distinto de "só admin gerencia papéis" (RN-008). | Entrevista PRD; revisão v1.3 |
| RN-007 | Observadores de uma tarefa são exclusivamente usuários cadastrados no sistema. | Entrevista PRD |
| RN-008 | O papel `admin` é global, sem vínculo de projeto — quem o possui tem acesso total ao sistema. Os demais papéis (`project_admin`, `product_owner`, `dev`, `gestor`, `user`) são atribuídos por par (usuário, projeto); um usuário pode ter mais de um papel no mesmo projeto (permissões acumuladas) e papéis diferentes em projetos diferentes. Criar/editar/excluir papéis e suas permissões (RF-013) é exclusivo do admin global; `project_admin` só associa usuários existentes a papéis já existentes, dentro do seu projeto (RF-015). | /clarify v1.2 |
| RN-009 | Por padrão, `dev` cria tarefas e edita título/descrição/tipo livremente enquanto a tarefa não tiver saído da etapa inicial do workflow pela primeira vez ("não iniciada"); uma vez "iniciada" (RN-010), o dev só movimenta a tarefa entre etapas (não edita mais título/descrição/tipo). `product_owner`, `project_admin` e `admin` não têm essa restrição. Excluir tarefa é restrito por padrão a `product_owner`/`project_admin`/`admin`. Esse comportamento default é configurável por projeto via toggles (RF-016). | /clarify v1.2 |
| RN-010 | Uma tarefa é considerada "iniciada" assim que sai da etapa inicial do workflow pela primeira vez — mesmo que retorne a ela depois, permanece "iniciada" permanentemente. | /clarify v1.2 |
| RN-011 | Executar a transição que move uma tarefa PARA uma etapa marcada como final (`etapaFinal=true`), assim como "desfinalizar" (RF-012), exige a permissão `tarefa:finalizar` — concedida por padrão a `product_owner`, `project_admin` e `admin`; `dev` não a possui por padrão. | /clarify v1.2 |
| RN-012 | Qualquer `dev` do projeto pode autoatribuir ("puxar") para si uma tarefa a qualquer momento, mesmo que já esteja atribuída a outro usuário, sem necessidade de aprovação; `dev` não pode atribuir uma tarefa a outro usuário — só a si mesmo. `product_owner` (e `project_admin`/`admin`) pode atribuir/reatribuir qualquer tarefa a qualquer `dev` do projeto a qualquer momento. Toda troca de responsável é registrada no histórico de auditoria da tarefa (RF-017). | /clarify v1.2 |
| RN-013 | Marcar/desmarcar impedimento (RF-004) é permitido por padrão a `dev` e `product_owner` (além de `project_admin`/`admin`); `gestor` não marca impedimento — só visualiza o dashboard por padrão (RF-013), configurável por projeto (RF-016). | /clarify v1.2 |
| RN-014 | O papel `user` (legado, sem permissões) permanece coexistindo com os novos papéis como fallback: um usuário autoprovisionado via Keycloak sem nenhuma role correspondente a um papel configurado recebe `user`. | /clarify v1.2 |
| RN-015 | Um projeto marcado como finalizado (data de finalização preenchida) fica somente leitura para todos os papéis, inclusive `admin`/`project_admin` — nenhuma escrita (tarefas, workflow, colunas, raias) é permitida até ser reaberto. Finalizar/reabrir um projeto usa a mesma permissão de gerenciar o projeto (`projeto:gerenciar`), disponível a `admin` global e ao `project_admin` daquele projeto. | /clarify v1.2 |
| RN-016 | Toda alteração relevante de uma tarefa (troca de responsável, edição de título/descrição, mudança de etapa) é registrada em um log de auditoria genérico da tarefa (RF-017), com autor, valor anterior, valor novo e data/hora. | /clarify v1.2 |

---

## 6. Casos de Uso

### UC-001 — Mover tarefa entre etapas

**Ator:** Desenvolvedor
**Fluxo principal:**
1. Desenvolvedor abre o board do projeto.
2. Seleciona uma tarefa e inicia o movimento (drag ou ação equivalente).
3. Sistema destaca as colunas com transição permitida a partir da etapa atual.
4. Desenvolvedor solta a tarefa em uma coluna permitida.
5. Sistema atualiza a etapa da tarefa, registra o lead-time da etapa anterior e notifica os observadores.

**Fluxo alternativo:** Se a coluna de destino não tem transição permitida, o sistema não permite o drop (coluna não destacada).

---

### UC-002 — Marcar tarefa como impedida

**Ator:** Desenvolvedor
**Fluxo principal:**
1. Desenvolvedor abre a tarefa.
2. Marca a tarefa como impedida.
3. Sistema exibe o indicador visual de impedimento no card.
4. Sistema inicia a contagem do tempo em impedimento.
5. Ao desmarcar, o sistema encerra a contagem e soma ao total de tempo em impedimento da etapa.

**Fluxo alternativo:** —

---

## 7. Restrições e Premissas

**Restrições:**
- Notificações são internas ao sistema — sem integração com email/Slack/etc. nesta versão.
- Sem controle de horas/timesheet do desenvolvedor.
- Sem suporte a múltiplas organizações/clientes.
- Sem dependência entre projetos nesta versão.

**Premissas:**
- Existe (ou existirá) uma instância de Keycloak disponível para integração SSO (Should Have).
- Ambiente de execução em containers (Docker/OpenShift) já é premissa de infraestrutura da organização.

---

## 8. Dependências

| Dependência | Tipo | Impacto |
|-------------|------|---------|
| Keycloak (SSO) | Técnica | Necessário para RF-014 (Should Have); sistema deve funcionar com autenticação própria caso indisponível |
| Infraestrutura de containers (Docker/OpenShift) | Técnica | Necessário para RNF-002 e RNF-004 |
| Mecanismo de tempo real (ex.: WebSocket/pub-sub) | Técnica | Necessário para RNF-001, a detalhar em techspec |

---

## 9. Critérios de Sucesso (KPIs)

| KPI | Meta | Prazo |
|-----|------|-------|
| Redução do tempo de execução/impedimento das tarefas | Qualitativo — sem meta numérica definida | — |
| Redução da comunicação dispersa sobre status/impedimentos | Qualitativo — sem meta numérica definida | — |
| Visibilidade de lead-time para gestores | Qualitativo — sem meta numérica definida | — |

---

## 10. Fora do Escopo

- Integração com sistemas externos de notificação (email, Slack etc.).
- Controle de horas/timesheet do desenvolvedor.
- Suporte a múltiplas organizações/clientes.
- Dependência entre projetos (bloqueio de um projeto por impedimento em outro).

---

## 11. Histórico de Revisões

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-08-22 | Thiago Goncalves Cavalcante | Versão inicial |
| 1.1 | 2026-08-22 | Thiago Goncalves Cavalcante | Clarificações: RNF-001 (limiar 2s), RNF-002 (escala de usuários/pods), RF-011 (coexistência de raias default), RF-013 (permissões deferidas ao techspec), RF-004 (impedimento não bloqueia movimentação), RF-007 (dashboard com período configurável) |
| 1.2 | 2026-08-23 | Thiago Goncalves Cavalcante | RBAC por projeto: papel `admin` global vs. papéis por projeto acumuláveis (RF-013, RN-008); novos RF-015 (associação usuário-projeto-papel), RF-016 (toggles de permissão por projeto), RF-017 (histórico de auditoria da tarefa); RF-003 com regra de edição travada após tarefa "iniciada" (RN-009, RN-010); RF-012 com permissão `tarefa:finalizar` (RN-011); RF-008 com finalização de projeto somente-leitura (RN-015); RN-012 (autoatribuição de tarefa) e RN-016 (auditoria); RNF-003 reforçando backend como única fonte de verdade de autorização. "Board" confirmado como sinônimo de projeto (sem entidade nova) |
| 1.3 | 2026-08-23 | Thiago Goncalves Cavalcante | Achado do comitê de análise técnica (security) sobre a TechSpec v1.1: RN-006 (delegação de `papel:gerenciar`) entrava em conflito com RN-008 (BDR-001) e deixava aberto um vetor de escalação de privilégio (project_admin manipulando permissões de um papel existente). RN-006 marcada como superseded em parte — `papel:gerenciar` passa a ser checada exclusivamente contra `Usuario.admin`, nunca atribuível via papel de projeto |
