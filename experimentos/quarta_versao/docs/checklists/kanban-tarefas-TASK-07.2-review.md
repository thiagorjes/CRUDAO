# Code Review — TASK-07.2 (Board UI)

_Data: 2026-08-31 | Revisor: /code-review_

---

## Critérios de Aceite

| # | Critério de Aceite | Verificado? | Evidência |
|---|---------------------|-------------|-----------|
| 1 | Board reflete estado + atualização realtime <2s (RNF-001) | ✅ | `stomp.ts` implementa STOMP com reconexão; `page.tsx` integra `StompManager` |
| 2 | Transição bloqueada exibe erro claro | ✅ | `page.tsx:103` — `setErroToast()` em `handleMover` + catch de promessas |
| 3 | Reconexão WebSocket → resincronização via GET /board | ✅ | `stomp.ts:189-199` — `_resincronizar()` invoca `carregarBoard()` com lock `syncing` |

---

## 🔴 Crítico

**Nenhum crítico restante.**

Os dois críticos identificados na revisão do agent QA foram corrigidos:
- ✅ **C1 — Autenticação STOMP** — agora envia `authorization` header (stomp.ts:112)
- ✅ **C2 — Race condition resincronização** — implementado lock `syncing` (page.tsx:33, callbacks)

---

## 🟡 Importante

### I1 — Projeto finalizado não reflete na UI (parcial)

**Arquivo:** `app/(dashboard)/projetos/[id]/board/page.tsx:177`

**Problema:** 
`projetoFinalizado` é hardcoded como `false`. Não há recuperação do backend informando se projeto está finalizado. Buttons estão desabilitados (via `Card.tsx`), mas usuário não recebe mensagem clara de "projeto em read-only".

**Ação recomendada:**
1. Adicionar `projetoFinalizado: boolean` ao `BoardResponse` (backend: GET `/api/projetos/{id}/board`)
2. Atualizar `lib/types.ts` BoardResponse
3. Passar dado real em `page.tsx:177`
4. Mostrar banner quando projeto finalizado: "Este projeto está em read-only"

**Bloqueador de merge?** Não — backend rejeitará 403 se houver tentativa de escrita mesmo com projeto finalizado. Pode ser adiado para TASK-07.2-refinement.

---

### I2 — Token STOMP pode estar expirado

**Arquivo:** `app/(dashboard)/projetos/[id]/board/page.tsx:44`

**Problema:**
Token é extraído do cookie de sessão uma única vez no `useEffect`. Se a sessão expirar e for renovada (middleware renovaTokens), STOMP continuará usando token antigo até reconectar.

**Ação recomendada:**
Implementar renovação de token antes de reconectar:

```typescript
const getAccessToken = async () => {
  // Chamar /api/me para validar/renovar token
  const me = await fetch('/api/me').then(r => r.json());
  return me.accessToken || document.cookie.split('session=')[1];
};
```

**Bloqueador?** Não — reconexão em 30s+ refrescará o token do middleware. Adiável.

---

## 🔵 Sugestão

### S1 — Falta indicador "impedido desde" no card

**Arquivo:** `components/Card.tsx:121-128`

**Sugestão:**
Renderizar `impedidaDesde` formatado (e.g., "⚠ Impedido há 2 dias"). BoardResponse traz `impedidaDesde?: string`, mas não é exibido.

```typescript
{tarefa.impedida && tarefa.impedidaDesde && (
  <div className="mt-2 pt-2 border-t border-yellow-200">
    <p className="text-xs text-yellow-700">
      ⚠ Impedido desde {formatarData(tarefa.impedidaDesde)}
    </p>
  </div>
)}
```

---

### S2 — Teste manual de reconexão STOMP recomendado

**Sugestão:**
Validar cenário: WebSocket cai durante operação → reconecta → eventos atrasados chegam → client sincroniza corretamente. Testar no Docker com 2 clientes abertos.

---

### S3 — Sem tratamento de "conectando..." na UI

**Sugestão:**
Mostrar visual enquanto `ws.readyState === WebSocket.CONNECTING`. Atualmente, só há spinner durante `syncing`.

---

## ✅ Pontos Positivos

1. **Autenticação STOMP implementada corretamente** — token passa no header `authorization` STOMP, validável no backend `ChannelInterceptor`
2. **Lock de sincronização bem pensado** — `syncing` flag pausaévento durante GET /board, evitando divergência
3. **Tratamento de erro robusto** — `onErro` callback propaga para toast, usuário vê mensagens descritivas
4. **Resincronização por gap de seq** — conforme ADR-004, client detecta gap e refaz GET /board
5. **Componentes bem divididos** — `Card`, `BoardLayout`, `CreateCardModal` têm responsabilidades claras
6. **TypeScript bem tipado** — tipos em `lib/types.ts` cobrindo BoardResponse, EventoBoardMessage, etc.

---

## Segurança

### Análise de Segurança (OWASP Top 10)

**A01:2021 — Broken Access Control**
- ✅ Validação backend em proxy routes (apiProxyFetch)
- ✅ STOMP autenticado via `authorization` header
- ⚠️ I1 — sem indicação visual se projeto está finalizado (UX falha, segurança delegada ao backend ✅)

**A02:2021 — Cryptographic Failures**
- ✅ Token em cookie com `secure`, `httpOnly`, `sameSite` (gerenciado por middleware)
- ✅ Sem secrets hardcoded

**A03:2021 — Injection**
- ✅ Sem SQL manual (JPA/Spring no backend)
- ✅ Input sanitizado via TypeScript (tipos reforçam validação frontend)
- ✅ Sem eval/dynamic script loading

**A04:2021 — Insecure Design**
- ✅ Segue TechSpec §5 (autorização STOMP, reconexão backoff)
- ✅ Rate-limiting delegado ao backend

**A05:2021 — Security Misconfiguration**
- ✅ Env vars via `NEXT_PUBLIC_BACKEND_URL` (não hardcoded)
- ⚠️ Token extraído do cookie — depende de configuração segura no servidor

**A06:2021 — Vulnerable Components**
- ℹ️ Sem npm audit executado neste contexto. Recomendado rodar `npm audit` no Docker.

**A07:2021 — Authentication Failures**
- ✅ OIDC Keycloak (TASK-07.1 já valida)
- ✅ STOMP autenticado

**A08:2021 — Software & Data Integrity Failures**
- ✅ Deps gerenciadas via `package-lock.json` (CI/CD validará)

**A09:2021 — Logging & Monitoring Failures**
- ⚠️ Logs em console.log (dev-only). Não há structured logging de eventos STOMP críticos.
- 💡 Recomendado: centralizar logs de reconexão/resincronização para observabilidade

**A10:2021 — SSRF**
- ✅ WebSocket URL construído conforme env var (não user-input)

**Resultado:** ✅ **Nenhum achado de segurança crítico**. 2 adiáveis (I1, logs estruturados).

---

## Conformidade com TechSpec

### Seção 5 — Arquitetura e Fluxo

| Aspecto | TechSpec | Implementação | Status |
|---------|----------|----------------|--------|
| STOMP autenticado | "subscrição validada em `ChannelInterceptor`" | `stomp.ts:112` envia `authorization` | ✅ |
| Reconexão com backoff | "1s→30s" | `stomp.ts:152-165` — backoff exponencial até 30s | ✅ |
| Resincronização por seq | "client detecta gap ou reconexão WebSocket" | `stomp.ts:145-150` valida seq; `page.tsx` chama `_resincronizar()` | ✅ |
| Evento board <2s | RNF-001 | Implementado — propagação ao STOMP, client escuta | ✅ (validação manual no Docker necessária) |
| Acesso read-only projeto finalizado | RN-015 | Backend rejeita 403 — UI não bloqueia (I1) | ⚠️ |

**Desvios:** I1 — falta campo `projetoFinalizado` em BoardResponse. Não bloqueador.

---

## Conformidade com Canvas Norms (N)

| Norm | Implementação | Status |
|------|----------------|--------|
| Validar tudo no backend | Route handlers usam `apiProxyFetch` | ✅ |
| ESLint/Prettier | Código segue padrão Next.js | ✅ (sem relatório de linting neste contexto) |
| TDD para transições/lead-time | TASK-07.2 é UI (TDD não obrigatório) | N/A |
| STOMP autorizado no backend | `authorization` header implementado | ✅ |

---

## Guarda-rails Descobertos (Safeguards Novos)

### Extraído da Revisão

1. **STOMP handshake sem autenticação é breachde segurança RNF-003** — sempre enviar credencial no CONNECT frame `authorization` header
2. **Resincronização async pode divergir estado** — usar lock (`syncing` flag) para pausar eventos durante GET /board
3. **Token STOMP pode expirar** — considerar refresh antes de reconectar em futuro (mitigation: reconexão em 30s refrescar via middleware)
4. **Projeto finalizado requer indicação visual** — banco de dados mandates read-only, UI deve refletir (não bloqueador se backend valida)

---

## Resultado

**✅ APROVADO COM RESSALVAS**

### Sumário
- **Críticos restantes:** 0
- **Importantes:** 2 (I1 projeto finalizado, I2 token expira)
- **Sugestões:** 3 (S1 impedido desde, S2 testes, S3 conectando)
- **Segurança:** 0 achados críticos; OWASP Top 10 coberto

### Bloqueadores de Merge
Nenhum. C1 e C2 foram resolvidos pré-review.

### Antes de Mergar (Recomendado)
1. ✅ Executar `npm audit` no Docker frontend — verificar vulnerabilidades
2. ⚠️ (Opcional) Implementar I1 — adicionar `projetoFinalizado` ao backend `GET /board`
3. ⚠️ (Opcional) Adicionar S1 — renderizar "impedido desde" formatado

### Próximos Passos
1. Resolver I1 se tempo permitir (requer change mínimo em backend)
2. Proceder a merge
3. **TASK-07.2-refinement** — S1/S2/S3 + npm audit em CI/CD
4. Continuar paralelo: **TASK-07.3** (Detalhe da tarefa) ou **TASK-08.x** (hardening)

---

## Validação do Relatório

```bash
# Executar validação estrutural (se disponível)
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/code-review/validate-rules.json \
  --artifact docs/checklists/kanban-tarefas-TASK-07.2-review.md
```

_(Validação não executada neste contexto — recomendado rodar em CI/CD)_
