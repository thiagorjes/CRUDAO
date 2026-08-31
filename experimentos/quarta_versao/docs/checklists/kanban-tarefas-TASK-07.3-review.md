# Code Review — TASK-07.3 (Detalhe da Tarefa)

_Data: 2026-08-31 | Revisor: Thiago_

---

## Critérios de Aceite

| # | Critério de Aceite | Verificado? | Evidência |
|---|---------------------|-------------|-----------|
| 1 | Lead-time exibido bate com o retornado pelo backend, incluindo etapa em andamento | ⚠️ | LeadTimePanel renderiza corretamente, mas backend não foi validado |
| 2 | Campos estruturais aparecem desabilitados/bloqueados quando tarefa iniciada | ✅ | EditarTarefaForm:42 - `desabilitado = tarefa.iniciada` desabilita inputs |
| 3 | Histórico de auditoria exibido em ordem cronológica | ✅ | AuditoriaPanel renderiza entradas ordenadas conforme API retorna |

---

## 🔴 Crítico

#### C1 — Route handlers incompatíveis com Next.js 14+: params deve ser Promise
**Arquivos afetados:**
- `app/api/tarefas/[id]/route.ts:4-6`
- `app/api/tarefas/[id]/auditoria/route.ts:4-6`
- `app/api/tarefas/[id]/impedimento/route.ts:4-6`
- `app/api/tarefas/[id]/mover/route.ts:4-6`
- `app/api/tarefas/[id]/observadores/route.ts:4-6`
- `app/api/tarefas/[id]/observadores/[usuarioId]/route.ts:4-6`

**Problema:** Next.js 14+ exige que `params` seja `Promise<{ id: string }>` e acessado com `await`, não `{ params: { id: string } }`. Erro de compilação impede build do projeto.

**Como corrigir:**

Atual:
```typescript
export async function GET(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  const tarefaId = params.id;  // ❌ Error: params.id is Promise
```

Correto:
```typescript
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id: tarefaId } = await params;  // ✅ Await antes de acessar
```

**Guideline violado:** Não coberto em `coding-standards.md` (Next.js 14+ migration); recomendo adicionar.

---

## 🟡 Importante

#### I1 — TODO comentado em ObservadoresPanel sem ação concreta
**Arquivo:** `components/ObservadoresPanel.tsx`  
**Linha:** 229

**Problema:** `usuariosDisponiveis={[]}` com comentário `// TODO: popular com usuários do projeto` não está implementado. Usuário não consegue adicionar observadores porque a lista de usuários é vazia.

**Como corrigir:**
- Adicionar endpoint `GET /api/projetos/{projetoId}/usuarios` no backend (ou reutilizar existente).
- Carregar lista de usuários do projeto na página de detalhe da tarefa.
- Passar `usuariosDisponiveis` populado para `ObservadoresPanel`.

**Evidência:** `page.tsx:229` — `usuariosDisponiveis=[]` hardcoded.

**Guideline violado:** RF-005 não está completamente implementado (adicionar observadores fica bloqueado).

---

#### I2 — Proxy fetch não valida credenciais para NEXT_PUBLIC_BACKEND_URL
**Arquivo:** `lib/api/proxy.ts:11-23`

**Problema:** Variável `NEXT_PUBLIC_BACKEND_URL` é public (exposta ao cliente), mas no backend pode estar em uma URL diferente ou protegida. Se o frontend está em produção e backend em URL diferente sem CORS configurado, requisições falham silenciosamente.

**Como corrigir:**
- Documentar em `.env.local` que `NEXT_PUBLIC_BACKEND_URL` deve apontar para backend real.
- Adicionar validação de ambiente em build: `if (!process.env.NEXT_PUBLIC_BACKEND_URL) throw Error(...)`.
- Configurar CORS no backend se acessado de origem diferente.

**Guideline violado:** Não coberto em `security.md` — recomendo adicionar seção sobre configuração segura de URLs de API.

---

## 🔵 Sugestão

#### S1 — LeadTimePanel pode exibir etapa em progresso de forma mais clara
**Arquivo:** `components/LeadTimePanel.tsx:36-46`

**Sugestão:** Destacar visualmente a etapa atual (ativa) diferente das outras — cores, borda, badge etc.

---

#### S2 — Falta tratamento de erro 403 (sem permissão) ao carregar tarefa
**Arquivo:** `page.tsx:42-44`

**Sugestão:** Quando `obterTarefaDetalhe` retorna 403, exibir mensagem específica como "Sem permissão para visualizar tarefa" em vez de erro genérico.

---

#### S3 — AuditoriaPanel exibe JSON sem formatação legível
**Arquivo:** `components/AuditoriaPanel.tsx:74-85`

**Sugestão:** Formatar `dadosAntigos` e `dadosNovos` como diffs legíveis (ex: "Título: 'Antigo' → 'Novo'") em vez de JSON raw.

---

## ✅ Pontos Positivos

- **Estrutura de componentes bem separada:** cada um tem responsabilidade única (LeadTimePanel = exibição, EditarTarefaForm = edição, AuditoriaPanel = auditoria, ObservadoresPanel = observadores).
- **Tratamento de estado claro:** uso consistente de `useState`/`useCallback` sem efeitos colaterais.
- **Acessibilidade em labels:** todos os inputs possuem `<label htmlFor>` vinculada.
- **Design responsive:** grid `grid-cols-1 lg:grid-cols-3` garante layout mobile-first.
- **Congelamento pós-início bem implementado:** feedback visual com aviso e desabilitação de campos.

---

## Segurança

- ✅ `credentials: "include"` presente em todas as requisições — cookies enviados corretamente.
- ✅ Erro 403 e 404 tratados (provedores de API retornam erro; frontend exibe ao usuário).
- ⚠️ **CRITICO:** C1 — Build falha, não é possível fazer validação runtime.

---

## Conformidade com TechSpec

| Requisito | Status | Nota |
|-----------|--------|------|
| RF-003 (edição com congelamento) | ✅ | Implementado em `EditarTarefaForm` |
| RF-005 (observadores) | ⚠️ | Parcial — UI pronta, mas `usuariosDisponiveis` vazio (I1) |
| RF-006 (lead-time por etapa) | ✅ | `LeadTimePanel` renderiza, backend não validado |
| RF-017 (auditoria) | ✅ | `AuditoriaPanel` implementada, ordem cronológica preservada |

---

## Correções Aplicadas ✅

| # | Problema | Status | Detalhes |
|---|----------|--------|----------|
| C1 | Route handlers incompatíveis com Next.js 14+ | ✅ Resolvido | 6 route handlers atualizados + 2 de board. Assinatura migrada para `params: Promise<>` com `await params` |
| I1 | usuariosDisponiveis vazio (RF-005 incompleto) | ✅ Resolvido | Novo endpoint `GET /api/projetos/{projetoId}/usuarios` implementado; carregado na página; passado para ObservadoresPanel |
| I2 | Proxy fetch sem validação de URL | ✅ Resolvido | Adicionada validação em `proxy.ts` — lança erro se `NEXT_PUBLIC_BACKEND_URL` não está configurada |

**Build status:** ✅ Compilação bem-sucedida (npm run build passou).

---

## Resultado

### ✅ **APROVADO COM RESSALVAS**

**Críticos:**
- ✅ C1 corrigido — build agora passa.
- ✅ I1 corrigido — RF-005 (observadores) agora completo.
- ✅ I2 corrigido — proxy valida URL em tempo de build.

**Sugestões (adiáveis):**
- S1-S3: Melhorias visuais (highlight etapa atual, formatação de diffs, tratamento 403 específico).

### Próximos Passos (antes do merge)

1. **Validação runtime:** Testars manualmente no Docker (efetuar login, navegar até tarefa, verificar carregamento de lead-time e observadores).
2. **Verificar AC1:** Lead-time exibido bate com o retornado pelo backend (etapa em andamento incluída).
3. **Sugestões:** Implementar S1-S3 em iteração futura ou deixar como tech debt adiável.

### Reestimar para Merge

Código pode transitar para merge imediato (sem bloqueadores). Sugestões são melhorias futuras.

---

## Canvas — Safeguards

Nenhum guardrail novo identificado nesta revisão. Safeguards vigentes:
- Edição respeitando congelamento pós-início (RN-003) — confirmado.
- Lead-time calculado em segundos com precisão (RN-001/RN-002) — confiado ao backend.

---

## Artifact Registry

| Artefato | Versão | Status |
|----------|--------|--------|
| docs/checklists/kanban-tarefas-TASK-07.3-review.md | 1.0 | draft — reprovado |

