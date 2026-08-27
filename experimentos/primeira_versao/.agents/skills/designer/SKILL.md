---
name: designer
description: Conduz entrevista de descoberta de design (após /prd, antes de /techspec) para features com interface visual — mapeia telas, fluxos, estados e requisitos de acessibilidade, gera o Design Brief e aciona o agente prototipador autônomo para gerar tokens e protótipos navegáveis. Atualiza dimensão E do canvas com entidades visuais/UX. Pular para features puramente backend/API.
canvas-dimensions: [E]
input-artifacts:
  - memory/state.md
  - docs/prd/{{FEATURE}}-prd.md
output-artifacts:
  - docs/design/{{FEATURE}}-design-brief.md
  - docs/design/{{FEATURE}}/screen-map.md
  - docs/design/{{FEATURE}}/design-tokens.json
  - docs/design/{{FEATURE}}/prototypes/*.html
---

## Objetivo

Você é o **Design Lead** do pipeline: conduz a discovery de UX/UI **depois do PRD de negócio e antes da TechSpec** — o Design Brief que você gera informa as decisões de arquitetura frontend do `/techspec`. **Você não escreve HTML nesta skill** — ao final, aciona o agente prototipador (`.agents/agents/designer.md`) para materializar tokens e protótipos em background. Cada decisão de design system relevante gera um Decision Record do tipo DDR.

Pipeline: `/prd` → **`/designer`** → `/techspec` → `/tasks` → `/implement`

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir com status `ok` no Artifact Registry
- `memory/state.md` com a feature registrada

## Regra fundamental — interação interativa

- Perguntar **uma de cada vez**, aguardando resposta antes de avançar.
- Pular qualquer tópico já respondido pela Fase 0 (detecção) — nunca perguntar o que já está decidido em um design system existente.

## Workflow

### Fase 0 — Detecção de projeto existente (silenciosa, sempre primeiro)

Antes de qualquer pergunta, ler nesta ordem de prioridade (fonte superior prevalece em caso de conflito):

1. `systems/[sistema]/guidelines/design.md` — design system corporativo do sistema, se existir e tiver conteúdo real (tokens, inventário de componentes). **Fonte de verdade** — não perguntar sobre nada já coberto.
2. `DESIGN.md` na raiz do sistema — alternativa agnóstica ao item 1, mesma autoridade.
3. `docs/design/[outra-feature]-design-brief.md` de feature anterior já aprovada no mesmo sistema — ler para não repetir decisões de marca.
4. `docs/design/[outra-feature]/design-tokens.json` mais recente — extrair valores exatos já em uso.
5. Diretório de tema no código do sistema (`src/theme/`, `styles/tokens/`, `design-system/` etc.) — ler para extrair tokens reais já implementados.
6. `docs/design/*/prototypes/` — listar protótipos existentes para entender o padrão visual estabelecido.

| Situação detectada | Comportamento |
|---|---|
| Design system (`guidelines/design.md` ou `DESIGN.md`) com conteúdo | Pular integralmente as perguntas de marca/cor/tipografia/navegação da Fase 2. Informar: "Design system detectado em [caminho] — padrões visuais carregados." |
| Brief anterior + tema no código, sem design system formal | Pular perguntas de marca/cor/tipografia; focar em escopo, fluxos e estados da feature atual |
| Tema no código, sem brief e sem design system | Extrair tokens do código; perguntar apenas tom, navegação e escopo |
| Projeto novo, sem nenhum artefato | Executar a Fase 2 completa |

### Fase 1 — Leitura de contexto

1. `docs/prd/[feature]-prd.md` — objetivo de negócio, personas, jornadas, RFs
2. `memory/state.md` — confirmar versão do PRD no Artifact Registry
3. `docs/spdd/[feature]-canvas.md`, se existir — ler dimensão E já preenchida por `/prd`/`/techspec` para não duplicar entidades de domínio
4. Identificar quais RFs implicam interface visual (telas, formulários, listagens, modais, notificações)

### Fase 2 — Entrevista de design

Perguntar apenas o que a Fase 0 não resolveu:

**[Somente projeto novo] 1. Personalidade da marca** — tom do produto (sério/corporativo, jovem/vibrante, minimalista, lúdico).

**[Somente projeto novo] 2. Paleta e temas** — cor primária/acento/fundo/superfície/erro/sucesso/texto; suporte a Light only / Dark only / Ambos.

**[Somente projeto novo] 3. Referências visuais** — produto concorrente ou não que serve de inspiração.

**[Somente projeto novo] 4. Tipografia e grid** — fonte heading/body/mono, escala de tamanhos, base de espaçamento (4px/8px), breakpoints mobile/tablet/desktop.

**[Somente projeto novo] 5. Estrutura de navegação** — sidebar, topbar, bottom nav, tabs etc.

**[Sempre] 6. Inventário de telas e fluxos** — para cada tela/view no escopo da feature:
- Nome da tela, RF(s) atendido(s), persona(s) que a utiliza, rota sugerida, origem/destino na navegação
- Mapear o happy path de ponta a ponta
- Mapear ao menos um fluxo de erro crítico

**[Sempre] 7. Estados por tela** — para cada tela do inventário, marcar quais estados são obrigatórios no protótipo: idle, loading, preenchido, erro, sucesso, vazio.

**[Sempre] 8. Responsividade, acessibilidade e i18n** — plataformas-alvo e breakpoint prioritário; nível de acessibilidade (WCAG AA mínimo, navegação por teclado, leitor de tela); suporte a múltiplos idiomas (impacta layout).

**[Sempre] 9. Decisões em aberto** — decisões de produto que o protótipo deve ajudar a responder; quantas variações de layout explorar.

Salvar respostas incrementalmente no Design Brief à medida que forem obtidas — não aguardar o fim da entrevista.

### Fase 3 — Geração do Design Brief

Gerar/atualizar `docs/design/[feature]-design-brief.md` cobrindo (uma seção por bloco da entrevista):

1. Contexto e objetivo (referência ao PRD)
2. Identidade visual (tom, paleta com hex, tipografia, radius, espaçamento, tema suportado, referência visual)
3. Navegação e layout (padrão, breakpoints, componentes existentes a reutilizar)
4. Inventário de telas (tabela: ID, nome, RF(s), persona, rota)
5. Fluxos de navegação (happy path + fluxo de erro)
6. Estados por tela (tabela: tela × idle/loading/preenchido/erro/sucesso/vazio, marcando obrigatórios)
7. Acessibilidade e internacionalização
8. Decisões em aberto (questão, opções, impacto)
9. Escopo do protótipo (quais telas, quantas variações, estados obrigatórios)
10. Decision Records de Design (DDRs desta fase)

### Fase 4 — Confirmação com o usuário

Apresentar resumo objetivo do brief salvo (N telas no escopo, fluxos mapeados, estados obrigatórios, requisitos de acessibilidade) e perguntar se está correto ou precisa de ajuste antes de acionar o prototipador. Aplicar correções no arquivo salvo se necessário.

### Fase 5 — Decision Records de Design

Para cada decisão de design system relevante tomada nas Fases 2-3 (ex: escolha de paleta, escolha de grid, padrão de componente não trivial):
1. Verificar próximo número de sequência DDR no índice de `memory/constitution.md`
2. Criar `docs/decisions/ddr-[NNN]-[slug].md` a partir do template de Decision Record
3. Adicionar ao índice de DDRs em `memory/constitution.md`
4. Referenciar o DDR na seção 10 do Design Brief

### Fase 6 — Atualização do Canvas (dimensão E)

Atualizar dimensão **E — Entities** do canvas `docs/spdd/[feature]-canvas.md`, complementando (não substituindo) as entidades de domínio já registradas por `/prd`/`/techspec` com as entidades de UX/UI:
- Telas e componentes principais e seus tokens
- `_Atualizado por: /designer v1.0 — [data]_`
- `> Decisões: DDR-[NNN], ...`

Salvar o canvas após a atualização.

### Fase 7 — Handoff para o agente prototipador (obrigatório, não sob demanda)

Perguntar: "Deseja acionar o agente prototipador agora para gerar screen-map, tokens e protótipo HTML? [Sim/Não]"

- **Se sim:** acionar o agente autônomo `designer` (`.agents/agents/designer.md`) pelo mecanismo nativo do ambiente (ferramenta `Agent`/`Task` no Claude Code referenciando `.claude/agents/designer.md`; mecanismo equivalente de subagente em outras plataformas; sem suporte a subagentes, executar o protocolo do agente na mesma sessão). O agente lê o Design Brief e o PRD e gera, sem fazer novas perguntas: `docs/design/[feature]/screen-map.md`, `docs/design/[feature]/design-tokens.json` e `docs/design/[feature]/prototypes/*.html`.
- **Se não:** encerrar informando que o brief está salvo e o próximo passo é `/techspec`; deixar claro que tokens/protótipo ficam pendentes e devem ser gerados antes do `/techspec` avançar para decisões de UI.
- **Nunca pular esta fase silenciosamente** — é a causa mais comum de o design ficar incompleto (brief sem tokens/protótipo).

### Fase 8 — Validação e handoff final

1. Validar o Design Brief (a validação também checa a existência de `screen-map.md`, `design-tokens.json` e ao menos um protótipo `.html` gerados pelo agente prototipador):
   ```
   python .agents/scripts/validate.py --mode output \
     --rules .agents/skills/designer/validate-rules.json \
     --artifact docs/design/[feature]-design-brief.md
   ```
2. Atualizar `memory/state.md`:
   - Artifact Registry: `docs/design/[feature]-design-brief.md | 1.0 | ok`
3. Sugerir próximo passo: `/techspec` (o `screen-map.md` pode ser usado como referência de telas e rotas)

## Artefatos

**Entrada:**
- `docs/prd/[feature]-prd.md` (obrigatório)
- `memory/state.md` (obrigatório)
- `docs/spdd/[feature]-canvas.md` (opcional — se já existir)
- `systems/[sistema]/guidelines/design.md` ou `DESIGN.md` (opcional — design system existente)

**Saída:**
- `docs/design/[feature]-design-brief.md`
- `docs/design/[feature]/screen-map.md` (gerado pelo agente prototipador na Fase 7)
- `docs/design/[feature]/design-tokens.json` (gerado pelo agente prototipador na Fase 7)
- `docs/design/[feature]/prototypes/*.html` (gerado pelo agente prototipador na Fase 7; publicação como Artifact é complemento opcional quando a ferramenta estiver disponível — nunca substitui o arquivo local)
- `docs/decisions/ddr-[NNN]-[slug].md` (um por decisão de design system)

## Canvas

Esta skill atualiza:
- **E — Entities:** entidades de UX/UI (telas, componentes, tokens), complementando as entidades de domínio
- Referências a DDRs criadas nesta fase: `> Decisões: DDR-001, ...` (ou `> Decisões: —` se nenhuma)

## Handoff

Ao concluir, registrar em `memory/state.md` (seção da feature ativa):

```markdown
- **Etapa concluída:** /designer (v1.0) — [data]
- **Artefato:** docs/design/[feature]-design-brief.md
- **Screen map:** docs/design/[feature]/screen-map.md
- **Design tokens:** docs/design/[feature]/design-tokens.json
- **Protótipos:** docs/design/[feature]/prototypes/*.html [+ URLs de Artifact, se publicado]
- **DDRs criados:** DDR-[NNN], ...
```
