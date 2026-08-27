# Discovery — Kanban Configurável
_Data: 2026-08-22 | Facilitador: Thiago Goncalves Cavalcante_

---

## 1. Problema

**Qual problema estamos resolvendo?**
A equipe de desenvolvimento não tem uma forma centralizada de controlar o andamento das atividades. O acompanhamento hoje depende de reports manuais via email e chat, que precisam ser cruzados por alguém para identificar impedimentos e direcioná-los ao responsável.

**Para quem é o problema?**
Para a equipe de desenvolvimento (devs e liderança) e para gestores de outros times que acompanham o andamento sem participar da execução.

**Como sabemos que é um problema real?**
Mensagens de impedimento se perdem em meio a outros comunicados (ex.: spam de email), causando cobranças recorrentes entre a equipe. Já houve caso concreto de uma demanda parada por 5 dias porque o aviso de impedimento do desenvolvedor não foi visto a tempo.

---

## 2. Personas

### Persona Principal: Equipe de Desenvolvimento (devs e liderança)

- **Perfil:** Desenvolvedores e líderes técnicos que executam e acompanham as atividades diariamente.
- **Objetivo principal:** Atualizar e visualizar o andamento das próprias tarefas, incluindo status e impedimentos, sem depender de comunicação dispersa.
- **Frustrações atuais:** Mensagens de status/impedimento perdidas em email e chat; necessidade de cobrança manual e cruzamento de informações.
- **Como usa a solução:** Uso diário e recorrente, com atualizações frequentes e eventualmente simultâneas na mesma tarefa por diferentes participantes.

### Persona Secundária: Gestores de outros times

- **Perfil:** Gestores que não executam as atividades, mas precisam acompanhar o andamento dos projetos.
- **Objetivo:** Visibilidade do progresso, impedimentos e lead-time sem necessidade de atualizar dados.

---

## 3. Objetivos de Negócio

| Objetivo | Métrica de sucesso | Prazo |
|----------|--------------------|-------|
| Reduzir tempo de execução e de impedimento das atividades | Qualitativo (sem meta numérica definida por ora) | — |
| Eliminar comunicação dispersa sobre status/impedimentos | Qualitativo (sem meta numérica definida por ora) | — |
| Dar visibilidade de andamento e lead-time aos gestores | Qualitativo (sem meta numérica definida por ora) | — |

---

## 4. Hipótese de Solução

**Acreditamos que** um sistema de kanban com etapas configuráveis por projeto, onde os próprios desenvolvedores atualizam status e sinalizam impedimentos, com notificações automáticas de impedimento e lead-time visível por etapa (no board da tarefa) e agregado em dashboard,

**Para** a equipe de desenvolvimento e gestores de outros times,

**Resultará em** menos tempo de tarefas paradas por impedimentos não vistos e menos esforço de comunicação manual/cobrança.

**Saberemos que funcionou quando** impedimentos forem identificados e resolvidos rapidamente, sem depender de cruzamento manual de reports, e gestores conseguirem visualizar andamento e lead-time diretamente no sistema.

---

## 5. Contexto Adicional

### Restrições conhecidas
- Etapas do kanban devem ser configuráveis por projeto (não fixas).
- Múltiplos usuários podem atualizar a mesma tarefa simultaneamente.

### Dependências identificadas
- Mecanismo de notificação automática de impedimento (interno ao sistema, sem integração externa nesta versão).
- Cálculo e exibição de lead-time por etapa (no board) e lead-time médio (em dashboard).

### O que está fora do escopo
- Integração com sistemas externos de notificação (email, Slack etc.).
- Controle de horas/timesheet do desenvolvedor.
- Suporte a múltiplas organizações/clientes.

### Referências e materiais de apoio
- —
