# PRD — Kanban Configurável
_Versão: 1.1 | Status: Draft | Data: 2026-08-22 | Autor: Thiago Goncalves Cavalcante_

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
**Então** a tarefa retorna para uma etapa anterior selecionada, reiniciando a contagem de lead-time daquela etapa.

**Prioridade:** Must Have

---

### RF-013 — Controle de acesso por papéis configuráveis

**Como** admin, **quero** criar/editar/excluir papéis e definir quais permissões cada um possui, **para** adaptar o controle de acesso à estrutura da equipe (dev, gestor etc.).

**Critérios de aceite:**

**Dado que** existem papéis padrão (admin, user)
**Quando** o admin cria um novo papel e define suas permissões
**Então** o sistema passa a aplicar essas permissões aos usuários com esse papel, sem permitir que o papel admin seja criado, editado ou excluído por outro papel delegado. O conjunto granular de permissões disponíveis será definido tecnicamente em techspec.

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

**Métrica:** Cobertura de ações restritas por permissão de papel.

**Critério:** Toda ação de criação/edição/exclusão de entidades administrativas (projetos, workflows, colunas, raias, papéis) deve respeitar as permissões do papel do usuário autenticado.

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
| RN-006 | O admin pode delegar a outros papéis a permissão de criar/editar/excluir papéis e permissões, exceto a permissão de criar, editar ou excluir o papel admin. | Entrevista PRD |
| RN-007 | Observadores de uma tarefa são exclusivamente usuários cadastrados no sistema. | Entrevista PRD |

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
