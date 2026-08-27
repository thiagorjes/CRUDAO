# PRD — Kanban de Tarefas
_Versão: 1.0 | Status: Draft | Data: 2026-08-24 | Autor: Thiago Goncalves Cavalcante_

---

## 1. Visão Geral

**Problema:** A equipe de desenvolvimento não tem uma forma centralizada de controlar o andamento das atividades. O acompanhamento hoje depende de reports manuais via email e chat, que precisam ser cruzados manualmente para identificar impedimentos e direcioná-los ao responsável — gerando perda de mensagens críticas (há caso registrado de demanda parada por 5 dias por aviso de impedimento não visto a tempo).

**Solução proposta:** Sistema de kanban com workflows e colunas configuráveis por projeto, onde os próprios desenvolvedores atualizam status e sinalizam impedimentos, com notificações internas de impedimento, lead-time visível por etapa no board e agregado em dashboard, controle de acesso por papéis escopados por projeto e autenticação via SSO (Keycloak).

**Público-alvo:** Equipe de desenvolvimento (devs e liderança técnica) e gestores de outros times que precisam de visibilidade sem participar da execução.

---

## 2. Stakeholders

| Papel | Nome | Responsabilidade |
|-------|------|-----------------|
| Product Owner / Aprovador | Thiago Goncalves Cavalcante | Aprovação de requisitos |
| Usuário final — Equipe de Dev | Devs e liderança técnica | Atualização de status/impedimentos, validação de UX |
| Usuário final — Gestores | Gestores de outros times | Consumo de dashboard e visibilidade de lead-time |

---

## 3. Requisitos Funcionais

### RF-001 — Board com colunas configuráveis por projeto

**Como** membro da equipe de desenvolvimento, **quero** visualizar um board com colunas configuráveis por projeto, **para** acompanhar o andamento das tarefas de acordo com o fluxo de trabalho específico daquele projeto.

**Critérios de aceite:**

**Dado que** um projeto possui um workflow com colunas definidas
**Quando** um usuário abre o board do projeto
**Então** o sistema exibe as colunas na ordem configurada, cada uma com as tarefas na etapa correspondente

**Prioridade:** Must Have

---

### RF-002 — Workflows com transições configuráveis entre etapas

**Como** administrador de projeto, **quero** configurar quais transições entre etapas são permitidas, **para** garantir que o fluxo de trabalho do projeto seja respeitado.

**Critérios de aceite:**

**Dado que** um workflow define transições permitidas entre colunas
**Quando** um usuário tenta mover uma tarefa para uma coluna sem transição configurada a partir da coluna atual
**Então** o sistema bloqueia a movimentação e informa que a transição não é permitida

**Prioridade:** Must Have

---

### RF-003 — CRUD de tarefas com trava de edição pós-"iniciada"

**Como** membro da equipe de desenvolvimento, **quero** criar, visualizar, editar e excluir tarefas, **para** manter o board atualizado com o trabalho real, respeitando o congelamento de campos após a tarefa ser iniciada.

**Critérios de aceite:**

**Dado que** uma tarefa já teve sua execução iniciada (saiu da primeira etapa do workflow)
**Quando** um usuário tenta editar campos estruturais da tarefa (ex.: descrição de escopo)
**Então** o sistema bloqueia a edição desses campos, permitindo apenas os campos definidos como editáveis pós-início (ex.: responsável, status, impedimento)

**Prioridade:** Must Have

---

### RF-004 — Sinalização de impedimento

**Como** membro da equipe de desenvolvimento, **quero** marcar e desmarcar uma tarefa como impedida, **para** sinalizar bloqueios sem depender de comunicação dispersa.

**Critérios de aceite:**

**Dado que** uma tarefa está em andamento
**Quando** um usuário autorizado marca a tarefa como impedida
**Então** o sistema registra o início do período de impedimento, exibe o indicador visual no board e inicia a contagem de lead-time de impedimento

**Prioridade:** Must Have

---

### RF-005 — Notificação de transições aos observadores

**Como** membro da equipe de desenvolvimento ou gestor, **quero** ser notificado internamente quando uma tarefa que observo muda de etapa ou é marcada como impedida, **para** agir rapidamente sem depender de cruzamento manual de reports.

**Critérios de aceite:**

**Dado que** um usuário observa uma tarefa (é responsável, criador ou observador explícito)
**Quando** a tarefa muda de etapa ou tem impedimento marcado/desmarcado
**Então** o sistema gera uma notificação interna visível ao usuário observador

**Prioridade:** Must Have

---

### RF-006 — Cálculo de lead-time por etapa

**Como** gestor ou membro da equipe, **quero** visualizar o lead-time de cada tarefa por etapa, **para** identificar gargalos no fluxo de trabalho.

**Critérios de aceite:**

**Dado que** uma tarefa passou por uma ou mais etapas
**Quando** um usuário abre o detalhe da tarefa
**Então** o sistema exibe o tempo decorrido em cada etapa e o tempo total de impedimento acumulado, conforme RN-001 e RN-002

**Prioridade:** Must Have

---

### RF-007 — Dashboard de gestão com lead-time médio

**Como** gestor, **quero** visualizar um dashboard com o lead-time médio por etapa e por projeto, **para** ter visibilidade de andamento sem precisar acompanhar a execução diretamente.

**Critérios de aceite:**

**Dado que** existem tarefas com histórico de movimentação em um projeto
**Quando** um gestor acessa o dashboard do projeto
**Então** o sistema exibe o lead-time médio por etapa, incluindo o tempo médio de impedimento agregado

**Prioridade:** Must Have

---

### RF-008 — CRUD de projetos (incl. finalizar/reabrir)

**Como** administrador, **quero** criar, editar, finalizar e reabrir projetos, **para** gerenciar o ciclo de vida dos projetos no sistema.

**Critérios de aceite:**

**Dado que** um projeto está ativo
**Quando** um administrador finaliza o projeto
**Então** o projeto passa a ser somente leitura para todos os usuários (conforme RN-015), até que seja reaberto

**Prioridade:** Must Have

---

### RF-009 — CRUD de workflows por projeto

**Como** administrador de projeto, **quero** criar, editar e excluir workflows associados a um projeto, **para** adaptar o fluxo de trabalho às necessidades do time.

**Critérios de aceite:**

**Dado que** um workflow não possui tarefas ativas vinculadas
**Quando** um administrador exclui o workflow
**Então** o sistema remove o workflow; caso existam tarefas ativas vinculadas, o sistema bloqueia a exclusão (RN-005)

**Prioridade:** Must Have

---

### RF-010 — CRUD de colunas (etapas) no board

**Como** administrador de projeto, **quero** criar, editar, reordenar e excluir colunas de um workflow, **para** representar as etapas reais do processo.

**Critérios de aceite:**

**Dado que** uma coluna não é a etapa final e não possui tarefas ativas
**Quando** um administrador configura a coluna
**Então** o sistema exige ao menos uma transição de saída configurada (RN-003), exceto para a etapa final

**Prioridade:** Must Have

---

### RF-011 — CRUD de raias (swimlanes) no board

**Como** administrador de projeto, **quero** criar, editar e excluir raias (swimlanes) no board, **para** organizar tarefas por categoria, time ou critério definido pelo projeto.

**Critérios de aceite:**

**Dado que** um projeto possui ao menos uma raia
**Quando** um usuário visualiza o board
**Então** as tarefas são agrupadas visualmente pelas raias configuradas

**Prioridade:** Must Have

---

### RF-012 — Etapa final com opção de reabertura

**Como** usuário autorizado, **quero** mover uma tarefa para a etapa final ou reabri-la ("desfinalizar"), **para** concluir ou retomar o trabalho quando necessário.

**Critérios de aceite:**

**Dado que** uma tarefa está na etapa final
**Quando** um usuário com permissão `tarefa:finalizar` executa a ação de "desfinalizar"
**Então** o sistema retorna a tarefa para a etapa selecionada, conforme RN-004 e RN-011

**Prioridade:** Must Have

---

### RF-013 — Controle de acesso por papéis configuráveis, escopados por projeto

**Como** administrador, **quero** que o acesso às ações do sistema seja controlado por papéis configuráveis por projeto, **para** garantir que cada usuário só execute ações compatíveis com sua responsabilidade.

**Critérios de aceite:**

**Dado que** um usuário possui um papel específico em um projeto
**Quando** ele tenta executar uma ação administrativa ou sensível
**Então** o sistema valida a permissão no backend antes de autorizar a ação, independentemente do estado da UI (RNF-003)

**Prioridade:** Must Have

---

### RF-014 — Login via SSO (Keycloak)

**Como** usuário do sistema, **quero** autenticar via SSO (Keycloak), **para** acessar o sistema sem precisar cadastrar ou lembrar uma senha local.

**Critérios de aceite:**

**Dado que** um usuário possui credenciais válidas no Keycloak
**Quando** ele inicia o login no sistema
**Então** o sistema redireciona para o fluxo de autenticação do Keycloak e, após sucesso, estabelece a sessão do usuário sem exigir senha local

**Prioridade:** Should Have

---

### RF-015 — Associação de usuário a projeto(s)

**Como** administrador, **quero** associar usuários a um ou mais projetos com um papel definido, **para** controlar quem tem acesso a cada projeto e com quais permissões.

**Critérios de aceite:**

**Dado que** um usuário não está associado a um projeto
**Quando** um administrador o associa com um papel
**Então** o usuário passa a visualizar e operar o board do projeto conforme as permissões do papel atribuído

**Prioridade:** Must Have

---

### RF-016 — Configuração de permissões por projeto (toggles)

**Como** administrador de projeto, **quero** habilitar/desabilitar toggles de permissão específicos por projeto (ex.: `devPodeExcluirTarefa`), **para** ajustar o controle de acesso às particularidades de cada time.

**Critérios de aceite:**

**Dado que** um toggle de permissão está desabilitado em um projeto
**Quando** um usuário do papel afetado tenta executar a ação correspondente
**Então** o sistema bloqueia a ação, mesmo que o papel normalmente permitisse

**Prioridade:** Must Have

---

### RF-017 — Histórico de auditoria da tarefa

**Como** gestor ou administrador, **quero** consultar o histórico de alterações relevantes de uma tarefa, **para** ter rastreabilidade de quem fez o quê e quando.

**Critérios de aceite:**

**Dado que** uma tarefa sofre alteração de responsável, título, etapa ou impedimento
**Quando** a alteração é confirmada
**Então** o sistema registra autor, valor anterior, valor novo e data/hora no histórico de auditoria (RN-016)

**Prioridade:** Must Have

---

### RF-018 — Criar card pelo board

**Como** membro da equipe de desenvolvimento com permissão `tarefa:gerenciar`, **quero** criar um card diretamente pelo board, **para** registrar uma nova tarefa sem sair da visão do fluxo de trabalho.

**Critérios de aceite:**

**Dado que** o projeto está ativo e o usuário possui `tarefa:gerenciar`
**Quando** o usuário cria um novo card pelo board sem informar responsável ou raia
**Então** o sistema cria o card sem responsável (RN-004) e o posiciona na etapa de menor ordem, na primeira raia do projeto ou raia default global (RN-005)

**Prioridade:** Must Have

---

### RF-019 — Excluir card pelo board

**Como** membro da equipe de desenvolvimento com permissão `tarefa:gerenciar`, **quero** excluir um card pelo board, **para** remover tarefas criadas indevidamente ou que não são mais válidas.

**Critérios de aceite:**

**Dado que** o usuário possui `tarefa:gerenciar` (e, se for do papel dev, o toggle `devPodeExcluirTarefa` está habilitado)
**Quando** o usuário exclui um card em um projeto que não está finalizado
**Então** o sistema remove o card, emite o evento `TAREFA_EXCLUIDA` para os demais usuários conectados e reflete a remoção nos boards abertos em até 2 segundos (RNF-001)

**Prioridade:** Must Have

---

## 4. Requisitos Não-Funcionais

### RNF-001 — Atualização em tempo real

**Categoria:** Performance

**Métrica:** Latência de propagação de alterações entre usuários conectados

**Critério:** Alterações no board (movimentação, criação, exclusão de card, impedimento) devem ser refletidas para os demais usuários conectados em até 2 segundos, sem necessidade de refresh manual.

---

### RNF-002 — Escalabilidade horizontal sem inconsistência

**Categoria:** Confiabilidade / Escalabilidade

**Métrica:** Divergência de estado entre instâncias sob escala horizontal

**Critério:** O sistema deve operar corretamente com 1 pod e escalar para 2 ou mais pods, suportando de dezenas a centenas de usuários simultâneos, sem divergência de estado (dados ou eventos em tempo real) entre instâncias.

---

### RNF-003 — Controle de acesso por papel revalidado no backend

**Categoria:** Segurança

**Métrica:** Cobertura de revalidação backend para ações administrativas/sensíveis

**Critério:** Toda ação administrativa ou sensível deve respeitar as permissões do(s) papel(is) do usuário no projeto; toda validação exibida na UI (esconder/desabilitar elemento) deve ser revalidada no backend — nenhuma escrita pode depender exclusivamente de validação client-side.

---

### RNF-004 — Empacotamento em containers

**Categoria:** Portabilidade

**Métrica:** Executável via imagem de container orquestrável

**Critério:** O sistema deve ser empacotado e executável em containers (Docker), orquestrável em OpenShift/Kubernetes, sem dependências que impeçam a execução em ambiente containerizado padrão.

---

### RNF-005 — Responsividade desktop

**Categoria:** Usabilidade

**Métrica:** Suporte a diferentes resoluções/navegadores desktop

**Critério:** A interface deve ser responsiva para uso em desktop, funcionando corretamente nos principais navegadores desktop utilizados pela equipe.

---

## 5. Regras de Negócio

| ID | Regra | Origem |
|----|-------|--------|
| RN-001 | Lead-time de uma etapa conta da entrada até a saída da tarefa naquela etapa. | kanban-configuravel |
| RN-002 | Tempo marcado como impedida é registrado separadamente, somado ao lead-time de impedimento da etapa e ao total, exibido no dashboard. | kanban-configuravel |
| RN-003 | Toda etapa deve ter ao menos uma transição de saída configurada, exceto a etapa final. | kanban-configuravel |
| RN-004 | Etapa final não tem transição de saída padrão, mas permite "desfinalizar" (retorna a tarefa a outra etapa). | kanban-configuravel |
| RN-005 | Não é permitido excluir projeto, workflow, coluna ou raia com tarefas ativas vinculadas — exige migração antes. | kanban-configuravel |
| RN-011 | Mover PARA etapa final ou "desfinalizar" exige permissão `tarefa:finalizar` (product_owner/project_admin/admin por padrão; dev não tem por padrão). | kanban-configuravel |
| RN-012 | Qualquer dev pode autoatribuir ("puxar") uma tarefa a qualquer momento, mesmo já atribuída a outro; dev não atribui a terceiros, só a si. product_owner/project_admin/admin atribuem/reatribuem livremente. Toda troca de responsável vai para auditoria (RF-017). | kanban-configuravel |
| RN-013 | Marcar/desmarcar impedimento é permitido por padrão a dev e product_owner (+ project_admin/admin); gestor não marca por padrão. | kanban-configuravel |
| RN-015 | Projeto finalizado fica somente leitura para todos, inclusive admin/project_admin, até reabertura. | kanban-configuravel |
| RN-016 | Toda alteração relevante (responsável, título, etapa, impedimento) é registrada em auditoria com autor, valor anterior/novo e data/hora. | kanban-configuravel |
| RN-CB-001 | Criar/excluir card exigem permissão `tarefa:gerenciar` no projeto. | criacao-card-board |
| RN-CB-002 | Exclusão por usuário do papel dev exige adicionalmente o toggle `devPodeExcluirTarefa` habilitado no projeto. | criacao-card-board |
| RN-CB-003 | Criação e exclusão de card são bloqueadas se o projeto estiver finalizado. | criacao-card-board |
| RN-CB-004 | Card é criado sem responsável quando não informado na criação. | criacao-card-board |
| RN-CB-005 | Card é criado, por padrão, na etapa de menor ordem, na primeira raia do projeto (ou raia default global, se não houver raia específica). | criacao-card-board |

---

## 6. Casos de Uso

### UC-001 — Criar tarefa pelo board e sinalizar impedimento

**Ator:** Membro da equipe de desenvolvimento (papel dev)
**Fluxo principal:**
1. Usuário abre o board do projeto e clica em "Novo card" na etapa de menor ordem
2. Sistema cria o card sem responsável, na primeira raia do projeto (RN-CB-004, RN-CB-005)
3. Usuário se autoatribui à tarefa (RN-012)
4. Usuário move a tarefa entre etapas conforme avança no trabalho
5. Usuário identifica um bloqueio e marca a tarefa como impedida (RF-004)
6. Sistema notifica observadores da tarefa (RF-005) e inicia contagem de lead-time de impedimento (RN-002)

**Fluxo alternativo:** Se o toggle `devPodeExcluirTarefa` estiver desabilitado e o usuário tentar excluir o card, o sistema bloqueia a ação (RN-CB-002).

---

### UC-002 — Gestor acompanha lead-time via dashboard

**Ator:** Gestor de outro time
**Fluxo principal:**
1. Gestor acessa o dashboard do projeto (sem necessidade de papel de execução)
2. Sistema exibe lead-time médio por etapa e tempo médio de impedimento agregado (RF-006, RF-007)
3. Gestor identifica etapa com maior tempo médio e aciona o time responsável fora do sistema

**Fluxo alternativo:** Se o projeto estiver finalizado, o dashboard permanece acessível em modo somente leitura (RN-015).

---

### UC-003 — Administrador finaliza projeto

**Ator:** Administrador de projeto
**Fluxo principal:**
1. Administrador acessa a configuração do projeto e aciona "Finalizar projeto"
2. Sistema valida que a ação é permitida (papel com `tarefa:finalizar` e regras de projeto)
3. Sistema marca o projeto como finalizado e torna-o somente leitura para todos os usuários, inclusive admin/project_admin (RN-015)

**Fluxo alternativo:** Administrador reabre o projeto posteriormente, restaurando a capacidade de edição (RF-008).

---

## 7. Restrições e Premissas

**Restrições:**
- Notificações são internas ao sistema — sem integração com email, Slack ou outros canais externos.
- Sistema não controla horas/timesheet do desenvolvedor.
- Sistema não suporta múltiplas organizações/clientes (single-tenant).
- Sistema não suporta dependência entre projetos.
- Sem importação em massa de cards, templates de card, duplicação de card ou anexos/arquivos nos cards.

**Premissas:**
- Keycloak já está disponível como provedor de identidade para o SSO (RF-014).
- Ambiente de destino suporta execução containerizada em OpenShift/Kubernetes (RNF-004).
- Times de dev e gestores já operam com noção mínima de fluxo kanban (não é necessário treinamento extensivo de conceito).

---

## 8. Dependências

| Dependência | Tipo | Impacto |
|-------------|------|---------|
| Servidor Keycloak configurado e acessível | Técnica | Bloqueia RF-014 (login SSO) se indisponível |
| Infraestrutura de containers (Docker/OpenShift/Kubernetes) disponível | Técnica | Bloqueia RNF-004 e RNF-002 (escalabilidade horizontal) |
| Mecanismo de comunicação em tempo real entre instâncias (para RNF-001/RNF-002 com múltiplos pods) | Técnica | Impacta RF-005, RF-019 e RNF-001 sob escala horizontal — detalhar em TechSpec |

---

## 9. Critérios de Sucesso (KPIs)

| KPI | Meta | Prazo |
|-----|------|-------|
| Redução de tempo de tarefas paradas por impedimento não visto | Qualitativa — sem meta numérica definida (ver discovery) | — |
| Redução de comunicação dispersa sobre status/impedimentos | Qualitativa — sem meta numérica definida (ver discovery) | — |
| Visibilidade de andamento e lead-time para gestores via dashboard | Qualitativa — sem meta numérica definida (ver discovery) | — |

---

## 10. Fora do Escopo

- Integração com sistemas externos de notificação (email, Slack etc.) — notificações são apenas internas.
- Controle de horas/timesheet do desenvolvedor.
- Suporte a múltiplas organizações/clientes (multi-tenant).
- Dependência entre projetos.
- Importação em massa de cards.
- Templates de card.
- Duplicar card.
- Anexos/arquivos em cards.

---

## 11. Histórico de Revisões

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-08-24 | Thiago Goncalves Cavalcante | Versão inicial — consolidação de kanban-configuravel e criacao-card-board |
