# AGENTS.md — CRUDAO

> Referência canônica de skills e agents disponíveis neste workspace SSPDD.
> Gerado automaticamente por `init.py` em 2026-08-24. Não editar manualmente — reexecute `init.py`/`update.ps1` para regenerar.

---

## Pipeline SSPDD

```
/guidelines → /discovery → /prd → [/clarify] → [/checklist] → [/designer] → /techspec → /tasks → [/analyze] → /implement ou /tdd → /code-review → /tests → [/spdd-sync]
```

`/guidelines` é setup por sistema (uma vez, não por feature). O REASONS Canvas é iniciado em `/discovery` e enriquecido progressivamente por cada skill até `/code-review`. Skills entre colchetes `[/skill]` são opcionais mas recomendadas.

---

## Skills disponíveis

- **/analyze** — Realiza análise cross-artefato entre PRD, TechSpec e Tasks no processo SDD, detectando inconsistências, lacunas de cobertura, ambiguidades e contradições. Use ao validar a consistência entre artefatos ou revisar especificações antes de iniciar a implementação.
- **/checklist** — Gera checklists de qualidade para validar o quão bem os requisitos estão escritos no PRD e na TechSpec, funcionando como testes unitários dos requisitos. Use ao verificar se requisitos estão completos, claros e mensuráveis antes de avançar para TechSpec ou implementação.
- **/clarify** — Identifica e resolve ambiguidades no PRD ativo, fazendo perguntas direcionadas uma de cada vez e atualizando o documento incrementalmente. Use quando o PRD tem termos vagos, métricas ausentes, critérios de aceite imprecisos ou casos de borda não definidos que podem gerar retrabalho.
- **/code-review** — Realiza code review integrado contra guidelines do projeto, TechSpec e critérios de aceite da task, com análise de segurança obrigatória. Ao final, extrai guardrails para atualizar dimensão S (Safeguards) do canvas. Use após implementar uma task antes de mergar.
- **/designer** — Conduz entrevista de descoberta de design (após /prd, antes de /techspec) para features com interface visual — mapeia telas, fluxos, estados e requisitos de acessibilidade, gera o Design Brief e aciona o agente prototipador autônomo para gerar tokens e protótipos navegáveis. Atualiza dimensão E do canvas com entidades visuais/UX. Pular para features puramente backend/API.
- **/discovery** — Conduz levantamento leve de problema, personas e contexto de negócio, inicializando o REASONS Canvas com dimensões R e E em DRAFT. Use como porta de entrada do pipeline antes do /prd.
- **/guidelines** — Conduz entrevista de stack e arquitetura para gerar os arquivos de guidelines do sistema (stack, architecture, coding-standards, testing, security, observability, git-workflow, skill-conventions, spdd-integration). Cria ADRs para decisões técnicas significativas. Use no início de cada novo sistema antes de /techspec.
- **/implement** — Executa uma task de implementação com precisão seguindo TechSpec, guidelines e canvas do projeto. Lê dimensões N (Norms) e S (Safeguards) do canvas como contexto. Use para implementar uma task específica do documento de tasks, produzindo código rastreável pronto para code review.
- **/prd** — Conduz entrevista estruturada de requisitos e gera PRD completo com RFs, RNFs, regras de negócio e critérios de aceite Gherkin. Use no início de qualquer nova feature após /discovery (ou direto, se discovery não foi executado).
- **/spdd-canvas** — Gera o REASONS Canvas completo a partir de PRD, TechSpec e Tasks para features que não passaram por /discovery, e é usada para atualização manual do canvas quando necessário. Use após aprovar a TechSpec quando o canvas ainda não existe ou está incompleto.
- **/spdd-sync** — Detecta divergências entre o REASONS Canvas e o código implementado e oferece resolução bidirecional (corrigir canvas ou reverter código), registrando cada desvio em deviations.md. Use após /implement ou /code-review quando o código evoluiu de forma diferente do que o canvas descreve.
- **/tasks** — Transforma PRD e TechSpec em tasks de implementação completas e auto-contidas, ordenadas por dependência, com oportunidades de paralelismo identificadas. Atualiza dimensão O do canvas. Use após aprovar a TechSpec.
- **/tdd** — Executa o ciclo TDD completo (Red → Green → Refactor → Review integrado) para implementar uma task com cobertura de testes desde o início. Lê dimensões N e S do canvas como contexto. Use como alternativa ao /implement quando testes são obrigatórios ou quando a lógica é complexa o suficiente para TDD ser mais seguro.
- **/techspec** — Gera especificação técnica completa a partir do PRD e guidelines do sistema, incluindo decisões arquiteturais, data model, contratos de API, estratégia de testes e matriz de rastreabilidade. Atualiza dimensões E, A, S, N do canvas. Use após aprovar o PRD.
- **/tests** — Gera e executa suíte de testes completa a partir dos critérios de aceite da task e da estratégia de testes da TechSpec, suportando modo TDD e modo audit. Use para criar cobertura de testes expressiva e alinhada às guidelines de testing do sistema.

---

## Agents disponíveis

Agents são especialistas invocáveis para decisões pontuais durante o pipeline (não substituem as skills — as complementam).

- **architect** — Responsável por decisões de arquitetura de software: estrutura de módulos, padrões de design, trade-offs técnicos e consistência entre sistemas.
- **database** — Responsável por modelagem de dados, esquemas, migrações e desempenho de consultas.
- **designer** — Desenvolvedor Frontend Prototipador operando em background no pipeline SSPDD. Materializa as definições de negócio (PRD) e estética (Design Brief) em código real — protótipos navegáveis de alta fidelidade. **Não faz perguntas ao usuário**: lê, analisa, gera os artefatos diretamente nos arquivos do projeto e informa que concluiu.
- **devops** — Responsável por CI/CD, infraestrutura, deploy e observabilidade operacional.
- **qa** — Responsável pela estratégia de testes, cobertura e qualidade funcional das entregas.
- **security** — Responsável pela análise de segurança: vulnerabilidades, controles de acesso e conformidade com guardrails.

---

## Convenções

- Toda skill tem `SKILL.md` canônico em `.agents/skills/[skill]/SKILL.md` e `validate-rules.json` para validação estrutural.
- Todo agent tem definição em `.agents/agents/[agent].md` com Role, Especialidade, Quando Invocar e Outputs Esperados.
- `memory/constitution.md` — princípios estáveis e ADRs. `memory/state.md` — estado operacional.


## Tools
- caso precise rodar docker execute antes, verifique se está ativo e em caso negativo execute:
"C:\Users\User\AppData\Local\Programs\DockerDesktop\Docker Desktop.exe"
