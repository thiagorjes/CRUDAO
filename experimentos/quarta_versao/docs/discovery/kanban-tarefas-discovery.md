# Discovery — Kanban de Tarefas
_Data: 2026-08-24 | Facilitador: /discovery_

---

## 1. Problema

**Qual problema estamos resolvendo?**
A equipe de desenvolvimento não tem uma forma centralizada de controlar o andamento das atividades: o acompanhamento hoje depende de reports manuais via email e chat, que precisam ser cruzados manualmente para identificar impedimentos e direcioná-los ao responsável.

**Para quem é o problema?**
Para a própria equipe de dev (que atualiza status/impedimentos) e para gestores de outros times, que precisam de visibilidade sem participar da execução.

**Como sabemos que é um problema real?**
Evidência qualitativa/anedótica: mensagens de impedimento se perdem em meio a outros comunicados (ex.: spam de email), gerando cobranças recorrentes entre a equipe. Há um caso concreto registrado de uma demanda parada por 5 dias porque o aviso de impedimento do desenvolvedor não foi visto a tempo. Não há dados quantitativos levantados até o momento.

---

## 2. Personas

### Persona Principal: Equipe de Desenvolvimento (devs e liderança técnica)

- **Perfil:** desenvolvedores e líderes técnicos que executam e acompanham as atividades diariamente.
- **Objetivo principal:** atualizar e visualizar o andamento das próprias tarefas, incluindo status e impedimentos, sem depender de comunicação dispersa.
- **Frustrações atuais:** mensagens de status/impedimento perdidas em email e chat; necessidade de cobrança manual e cruzamento de informações.
- **Como usa a solução:** uso diário e recorrente, com atualizações frequentes e eventualmente simultâneas na mesma tarefa por diferentes participantes.

### Persona Secundária: Gestores de outros times

- **Perfil:** gestores que não executam as atividades, mas precisam acompanhar o andamento dos projetos.
- **Objetivo:** visibilidade do progresso, impedimentos e lead-time sem necessidade de atualizar dados.

---

## 3. Objetivos de Negócio

| Objetivo | Métrica de sucesso | Prazo |
|----------|--------------------|-------|
| Reduzir tempo de execução e de impedimento das atividades | Qualitativa (sem meta numérica definida) | — |
| Eliminar comunicação dispersa sobre status/impedimentos | Qualitativa (sem meta numérica definida) | — |
| Dar visibilidade de andamento e lead-time aos gestores | Qualitativa (sem meta numérica definida) | — |

---

## 4. Hipótese de Solução

**Acreditamos que** um sistema de kanban com etapas configuráveis por projeto, onde os próprios desenvolvedores atualizam status e sinalizam impedimentos, com notificações automáticas de impedimento (internas ao sistema) e lead-time visível por etapa (no board da tarefa) e agregado em dashboard,

**Para** a equipe de desenvolvimento e gestores de outros times,

**Resultará em** menos tempo de tarefas paradas por impedimentos não vistos e menos esforço de comunicação manual/cobrança.

**Saberemos que funcionou quando** impedimentos forem identificados e resolvidos rapidamente, sem depender de cruzamento manual de reports, e gestores conseguirem visualizar andamento e lead-time diretamente no sistema.

---

## 5. Contexto Adicional

### Restrições conhecidas
- Notificação de impedimento é interna ao sistema (sem integração com email/Slack — ver escopo).

### Dependências identificadas
- —

### O que está fora do escopo
- Integração com sistemas externos de notificação (email, Slack etc.)
- Controle de horas/timesheet do desenvolvedor
- Suporte a múltiplas organizações/clientes

### Referências e materiais de apoio
- —
