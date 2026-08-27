# Discovery — kanban-configuravel
_Data: 2026-08-26 | Facilitador: opencode_

---

## 1. Problema

**Qual problema estamos resolvendo?**
A equipe de desenvolvimento não tinha um jeito centralizado de acompanhar o andamento das tarefas — o status e os impedimentos eram reportados por email/chat e se perdiam no meio de outras mensagens, causando cobranças manuais e, em um caso concreto, uma demanda parada por 5 dias sem que ninguém visse o aviso de impedimento a tempo. O kanban-configuravel resolve isso com um board onde os próprios devs atualizam status/impedimentos em tempo real, com etapas configuráveis por projeto e lead-time visível — eliminando o cruzamento manual de reports, tanto para quem executa quanto para gestores que só acompanham.

**Para quem é o problema?**
Equipe de Desenvolvimento (devs e liderança) — desenvolvedores e líderes técnicos que executam e acompanham as atividades diariamente. Persona secundária: gestores de outros times que precisam de visibilidade de progresso, impedimentos e lead-time sem precisar atualizar dados.

**Como sabemos que é um problema real?**
- Caso concreto documentado: uma demanda ficou parada por 5 dias porque o aviso de impedimento do desenvolvedor se perdeu e não foi visto a tempo.
- Padrão recorrente relatado: mensagens de status/impedimento se perdem em meio a outros comunicados (ex.: spam de email), gerando cobranças recorrentes entre a equipe — não é um incidente isolado, é descrito como algo que já vinha acontecendo.
- Não há métricas quantitativas (frequência, tempo médio perdido, nº de ocorrências) — os objetivos de negócio estão marcados como "qualitativo, sem meta numérica definida por ora". A evidência é anedótica/observacional da própria equipe, não dado instrumentado.

---

## 2. Personas

### Persona Principal: Equipe de Desenvolvimento

- **Perfil:** desenvolvedores e líderes técnicos que executam e acompanham as atividades diariamente.
- **Objetivo principal:** atualizar e visualizar o andamento das próprias tarefas — status e impedimentos — sem depender de comunicação dispersa (email/chat).
- **Frustrações atuais:** mensagens de status/impedimento perdidas; necessidade de cobrança manual e cruzamento de informações para saber o real andamento.
- **Como usa a solução:** uso diário e recorrente, com atualizações frequentes e eventualmente simultâneas na mesma tarefa por diferentes participantes (múltiplos usuários editando ao mesmo tempo).

### Persona Secundária (opcional): Gestores de outros times

- **Perfil:** gestores que não executam as atividades mas precisam de visibilidade de progresso, impedimentos e lead-time.
- **Objetivo:** consultar andamento e lead-time direto no sistema, sem intermediação.

---

## 3. Objetivos de Negócio

| Objetivo | Métrica de sucesso | Prazo |
|----------|--------------------|-------|
| Reduzir tempo de execução e de impedimento das atividades | Qualitativa, sem meta numérica definida | — |
| Eliminar comunicação dispersa sobre status/impedimentos | Qualitativa, sem meta numérica definida | — |
| Dar visibilidade de andamento e lead-time aos gestores | Qualitativa, sem meta numérica definida | — |

---

## 4. Hipótese de Solução

**Acreditamos que** um sistema de kanban com etapas configuráveis por projeto, onde os próprios desenvolvedores atualizam status e sinalizam impedimentos, com notificações automáticas de impedimento e lead-time visível por etapa (no board da tarefa) e agregado em dashboard

**Para** a equipe de desenvolvimento e gestores de outros times

**Resultará em** menos tempo de tarefas paradas por impedimentos não vistos e menos esforço de comunicação manual/cobrança

**Saberemos que funcionou quando** impedimentos forem identificados e resolvidos rapidamente, sem depender de cruzamento manual de reports, e gestores conseguirem visualizar andamento e lead-time diretamente no sistema

---

## 5. Contexto Adicional

### Restrições conhecidas
- Integração com sistemas externos de notificação (email, Slack etc.) está fora do escopo
- Controle de horas/timesheet do desenvolvedor está fora do escopo
- Suporte a múltiplas organizações/clientes está fora do escopo

### Dependências identificadas
- Nenhuma dependência externa identificada até o momento

### O que está fora do escopo
- Integração com sistemas externos de notificação (email, Slack etc.)
- Controle de horas/timesheet do desenvolvedor
- Suporte a múltiplas organizações/clientes

### Referências e materiais de apoio
- Nenhuma referência adicional documentada