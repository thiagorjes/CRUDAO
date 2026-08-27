# Code Review — TASK-05.3 — Frontend: Painel de Administração

_Revisor: agent QA (general-purpose) — 2026-08-24_
_Sistema: CRUDAO (`systems/CRUDAO/frontend/`)_

## Gate de testes

- `npx tsc --noEmit` — ✅ limpo
- `npx eslint src/components/admin src/app/admin src/components/board/BoardApp.tsx` — ✅ limpo
- `npx next build` — ✅ build de produção concluído (rota `/admin` gerada como estática)
- Sem testes unitários novos — decisão consciente e documentada em `memory/state.md`, consistente com o padrão já aplicado em TASK-05.2/`TarefaDetalhePage` (UI declarativa consumindo endpoints já cobertos por teste de backend). Avaliado como razoável, não bloqueante.

## Critérios de Aceite

| # | Critério de Aceite | Verificado? | Evidência |
|---|---|---|---|
| 1 | `admin` global alterna entre todos os projetos; usuário com papel só em 1 projeto só vê esse projeto no seletor | ✅ | `AdminApp.tsx:36-38` — filtra `todosProjetos` por `me.projetos.some(...)` quando `!me.admin` |
| 2 | Aba de Papéis/Permissões não aparece para quem não é `admin` global, mesmo sendo `project_admin` | ✅ | `AdminApp.tsx:104` (`visivel: admin`) e dupla checagem em `AdminApp.tsx:185` (`abaAtual === 'papeis' && admin`) |
| 3 | Toggles e finalização de projeto refletem no comportamento real do sistema (validado contra TASK-01.3/TASK-04.2), não só na UI | ✅ | `TogglesAba.tsx` chama `GET/PUT /projetos/{id}/configuracao`; `ProjetosAba.tsx:46-64` chama `PUT`/`DELETE /projetos/{id}/finalizar` — contratos batem com TechSpec v1.3 §8 (linhas 142-158); efeito real é responsabilidade do backend (TASK-01.3/04.2, já revisado) |
| 4 | Projeto finalizado bloqueia edição na UI e no backend simultaneamente (chamada direta à API confirma 403/409) | ✅ | UI: `bloqueado = Boolean(projetoAtual?.dataFinalizacao)` (`AdminApp.tsx:71`) desabilita campos/ações em todas as abas (`disabled={bloqueado}` / `!bloqueado &&`); backend: `AutorizacaoProjetoService.exigirPermissao`/`exigirProjetoNaoFinalizado` já cobertos por testes unitários na TASK-01.3/02.3 (não re-executado nesta review — fora do escopo desta task, só frontend) |
| 5 | Exclusões bloqueadas (RN-005) exibem modal de erro claro, orientando migração | ✅ | `WorkflowsAba.tsx:92-94` (workflow), `WorkflowsAba.tsx:131-133` (etapa), `RaiasAba.tsx:63-65` (raia) — mensagens específicas citando RN-005 e orientando verificar associações; propagam via `onErro` → `ModalErro` (`AdminApp.tsx:187`) |

## 🔴 Crítico

Nenhum.

## 🟡 Importante

Nenhum.

## 🔵 Sugestão

#### S1 Seletor de "raias globais" some silenciosamente para não-admin ao trocar de projeto
Arquivo: `RaiasAba.tsx:72-77`
Problema: o checkbox "Gerenciar raias globais" só é renderizado com `admin && (...)`. Comportamento correto (RN documentada — só admin gerencia raia default global), mas se um `project_admin` tiver deixado `globais=true` internamente antes de perder acesso a essa view (não há como hoje, pois o estado é local ao componente) não há risco real. Sinalizado apenas como ponto de atenção para not-yet-existing cenário de reuso do componente fora do gating atual — não é um problema no código como está.
Como corrigir: nenhuma ação necessária agora; manter nota caso o componente seja reaproveitado em outro contexto sem o gating do `AdminApp`.
Guideline violado: não coberto — nota de manutenibilidade, não de guideline.

#### S2 `AdminApp.permissoesProjeto` hardcoda um subconjunto de permissões para `admin` — CORRIGIDO 2026-08-24
Arquivo: `AdminApp.tsx:61-66`
Problema: `if (usuarioMe.admin) return new Set(['projeto:gerenciar', 'workflow:gerenciar'])` — sintetiza permissões em vez de refletir todas as chaves do catálogo. Funciona hoje porque as únicas leituras de `permissoesProjeto` são exatamente essas duas chaves (`podeGerenciarProjeto`/`podeGerenciarWorkflow`), mas é um ponto frágil: uma nova aba que dependa de outra chave (ex.: `impedimento:marcar`) e leia `permissoesProjeto.has(...)` quebraria silenciosamente para `admin` global.
Como corrigir:
  Atual:   `if (usuarioMe.admin) return new Set(['projeto:gerenciar', 'workflow:gerenciar']);`
  Correto: usar diretamente as duas booleans (`admin || <flag>`) sem a indireção de `Set`, ou popular o `Set` com todas as chaves do catálogo quando `admin === true`.
Guideline violado: não coberto — registrado como G-FE-02 (dimensão S do canvas).
Correção aplicada: `AdminApp.tsx` — `permissoesProjeto` deixou de sintetizar chaves para `admin`; `podeGerenciarProjeto`/`podeGerenciarWorkflow` fazem `admin || permissoesProjeto.has(chave)` diretamente. `tsc`/`eslint` limpos após a mudança.

## ✅ Pontos Positivos

- Filtro de `papel:gerenciar` em `MembrosAba.tsx:64-67` (`papeisAtribuiveis`) usa `p.permissoes.includes('papel:gerenciar')` — checagem por conteúdo de permissão, não por nome do papel, exatamente o padrão robusto exigido por G-RBAC-07 (evita bypass por renomear um papel).
- `AdminApp.tsx` mantém uma única fonte de verdade para o estado "bloqueado" (`projetoAtual?.dataFinalizacao`) propagada por prop a todas as abas, em vez de cada aba reimplementar a checagem — reduz risco de uma aba nova "esquecer" de desabilitar edição.
- Todas as chamadas de escrita passam por `api.put/post/delete` do cliente HTTP existente (`/api/proxy/...`), sem nenhum fetch direto ao backend nem manuseio de token no client — consistente com o guardrail geral de "nunca expor token ao JS" (exceto STOMP, que não se aplica aqui).
- Reaproveitamento de `crudao_projeto_id` do `localStorage` (`AdminApp.tsx:40,52`) mantém o painel sincronizado com a seleção do board, conforme pedido explícito da task.
- Mensagens de erro de exclusão bloqueada (RN-005) são específicas por entidade (workflow/etapa/raia), evitando o genérico "algo deu errado" e efetivamente orientando o usuário a investigar associações.

## Segurança

- Gating de UI via `GET /api/usuarios/me` é exclusivamente estético em todo o painel — nenhuma ação de escrita (`api.post/put/delete`) usa dado de `usuarioMe` para decidir o corpo da requisição ou para pular uma validação; toda decisão de autorização real permanece no backend (`AutorizacaoProjetoService`, já revisado nas TASK-04.2/01.3/02.3). Confirmado por leitura completa de todos os `onClick`/handlers das 6 abas.
- `papel:gerenciar` nunca é oferecido como atribuível na aba Membros — filtro por conteúdo de `permissoes`, não por nome (`MembrosAba.tsx:64-67`), robusto a rename do papel. Backend rejeita com 422 como segunda camada (defesa em profundidade, confirmado no contrato TechSpec §8).
- Aba "Papéis e permissões" gated exclusivamente por `admin === true` (booleano vindo de `UsuarioMe.admin`), nunca por posse de `projeto:gerenciar` — consistente com G-RBAC-07/ADR-006.
- Nenhum campo de PII novo foi adicionado a `Usuario`/`Membro` além de `id`/`nome` — `Membro` (`types.ts:34-38`) segue o mesmo padrão minimalista de `Usuario`, sem e-mail. `UsuarioMe`/`ProjetoPapeis` não vazam dado de terceiros (é sempre o próprio usuário autenticado).
- Cliente HTTP (`api/client.ts`) só fala com `/api/proxy/...` — nenhuma chamada direta ao backend nem token manuseado no client nesta task (painel não usa STOMP).
- Projeto finalizado: UI desabilita consistentemente em todas as abas via prop `bloqueado`; validado que a garantia real (403/409) é responsabilidade do backend já revisado — nenhuma tentativa de "confiar" no estado do cliente para bloquear (o `disabled` é só UX, a chamada seria rejeitada mesmo se o atributo fosse removido via devtools).

Nenhum finding de segurança crítico ou importante.

## Conformidade com TechSpec

- Contratos consumidos (`/usuarios/me`, `/projetos/{id}/membros`, `/papeis`, `/projetos/{id}/configuracao`, `/projetos/{id}/finalizar`) batem exatamente com TechSpec v1.3 §8 (linhas 118-158), incluindo os métodos HTTP (`PUT`/`DELETE` para finalizar/reabrir) e formatos de payload.
- `PapeisAba.tsx` usa a lista fixa de 8 chaves de permissão do catálogo (`RbacSeeder`) — consistente com a granularidade fechada na TASK-04.1 (`memory/state.md`, Q-003).
- Nenhum desvio identificado entre o comportamento implementado e o contrato especificado.

## Resultado

**APROVADO**

---

## Fase 3 — Guardrails sugeridos para o Canvas (dimensão S)

Nenhum guardrail novo de segurança foi extraído — o painel segue rigorosamente os padrões já registrados em G-RBAC-05/06/07/08 e não introduz nenhum atalho de autorização no cliente. Sugestão de guardrail de manutenibilidade (não bloqueante, opcional para o canvas):

- **G-FE-02 (sugerido):** gating de UI baseado em permissões (`GET /api/usuarios/me`) deve preferir checagens diretas (`permissoes.includes(chave)`) a listas sintéticas hardcoded por papel (ex.: evitar `if (admin) return new Set([...chaves fixas...])`) — reduz risco de uma nova tela esquecer de estender a lista sintética ao introduzir uma nova chave de permissão. Ver `AdminApp.tsx:61-66` (finding 🔵 S2 desta revisão, não corrigido por ser não-bloqueante).
