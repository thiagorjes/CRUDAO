# Code Review — TASK-04.2

_Versão: 1.0 | Data: 2026-08-28 | Revisor: Claude Code / /code-review skill_

> Revisão de código implementado para **TASK-04.2 — Mover tarefa: engine de transição + congelamento + lead-time + RN-012**.
> 
> **Pré-review:** Testes executados: 15 testes, 100% verde ✅. Suíte completa de `TarefaMoverServiceTest.java` passa.

---

## Critérios de Aceite

| # | Critério de Aceite (TASK-04.2) | Verificado? | Evidência |
|---|---|---|---|
| 1 | Transição bloqueada quando não configurada | ✅ | TarefaService:150-155 (validação via transicaoRepository) |
| 2 | Mover para/reabrir etapa final sem `tarefa:finalizar` → `403` | ✅ | TarefaService:158-165 (permissaoGuard.exigir() se etapaFinal) |
| 3 | Edição de campo estrutural após início bloqueada; campos editáveis permanecem | ✅ | TarefaService:216-219 (bloqueio de titulo/descricaoEscopo se iniciada=true) |
| 4 | Lead-time por etapa calculado corretamente, incluindo etapa em andamento | ✅ | TarefaService:300-310 (obterComLeadTime calcula até Instant.now() se saidaEm=null) |
| 5 | Toda movimentação gera linha em `TarefaAuditoria` | ✅ | TarefaService:193-200 (TarefaAuditoria com campo "etapa") |
| 6 | RN-012: dev só se autoatribui; product_owner/admin atribuem livremente | ✅ | TarefaService:229-233 + PermissaoGuard:96-100 (validarAutoatribuicaoRN012) |

---

## 🔴 Crítico

**Nenhum finding crítico identificado.**

---

## 🟡 Importante

### I1 — Método `editar()` não valida entrada de títulos/descricoes muito longas

**Status:** ✅ **RESOLVIDO em 2026-08-28**

**Arquivo:** `TarefaService.java:203-283`, novo `EditarTarefaRequest.java`

**Problema (original):** 
Quando o usuário edita `titulo` ou `descricaoEscopo`, o código aceitava qualquer string sem validar tamanho máximo ou nulidade.

**Solução implementada:**

1. ✅ Criado `EditarTarefaRequest` com Bean Validation:
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditarTarefaRequest {
    @NotBlank(message = "Título não pode ser vazio")
    @Size(min = 1, max = 255, message = "Título deve ter entre 1 e 255 caracteres")
    private String titulo;

    @Size(max = 4096, message = "Descrição não pode exceder 4096 caracteres")
    private String descricaoEscopo;

    private UUID responsavelId;
}
```

2. ✅ Método `editar()` refatorado para receber `EditarTarefaRequest` em vez de `Map<String, Object>`:
   - Spring Bean Validation valida automaticamente (@NotBlank, @Size)
   - Eliminada casting inseguro e validação manual

3. ✅ Lógica preservada:
   - Congelamento de campos estruturais após iniciada
   - Auditoria de cada alteração (título, descrição, responsável)
   - RN-012 (autoatribuição) mantido

**Impacto:** Testes existentes em `TarefaMoverServiceTest.java` continuam válidos (15/15 passando). DTOs não são re-testados nesta task — validação automatizada via Spring Boot.

**Guideline coberto:** `security.md` (validação de entrada em pontos de entrada externos) + `coding-standards.md` (uso de Bean Validation como padrão).

---

## 🔵 Sugestão

### S1 — Extract method: lógica de "encontrar último histórico aberto"

**Arquivo:** `TarefaService.java:168-172`

**Sugestão:**
A lógica de encontrar o histórico de etapa atual com `saidaEm=null` aparece em `mover()` e potencialmente será replicada em `obterComLeadTime()` se houver refatorações posteriores. Extrair para método reutilizável:

```java
private TarefaEtapaHistorico obterHistoricoAbetoAtual(UUID tarefaId) {
    return tarefaEtapaHistoricoRepository.findByTarefaIdOrderByEntradaEmAsc(tarefaId)
            .stream()
            .filter(h -> h.getSaidaEm() == null)
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Histórico de etapa não encontrado"));
}
```

**Benefício:** Facilita manutenção e reduz duplicação.

---

### S2 — Adicionar javadoc ao método `obterComLeadTime()`

**Arquivo:** `TarefaService.java:285-332`

**Sugestão:**
Documentar o comportamento de arredondamento de timestamps e casos edge (etapa em andamento, múltiplos ciclos de impedimento). Exemplo:

```java
/**
 * Obter detalhe da tarefa com cálculo de lead-time por etapa e tempo total de impedimento.
 * RF-006: Lead-time calculado a partir de `TarefaEtapaHistorico`, incluindo etapa em andamento.
 * 
 * <p><strong>Lead-time:</strong> Calculado em segundos (ChronoUnit.SECONDS.between) até Instant.now()
 * se a etapa está em andamento (saidaEm=null). Arredonda para segundo mais próximo.
 * 
 * <p><strong>Tempo de impedimento (RN-002):</strong> Acumula múltiplos ciclos marca/desmarca.
 * Se um impedimento está aberto (desmarcadoEm=null), inclui até Instant.now().
 * 
 * @param tarefaId UUID da tarefa
 * @return TarefaDetalheResponse com histórico de etapas e tempo de impedimento agregado
 * @throws ResponseStatusException 404 se tarefa não encontrada
 */
@Transactional(readOnly = true)
public TarefaDetalheResponse obterComLeadTime(UUID tarefaId) {
    // ...
}
```

---

## ✅ Pontos Positivos

1. **Testes abrangentes com mocks e ArgumentCaptor:** A suíte de 15 testes cobre caminho feliz, validações de permissão, congelamento, lead-time e auditoria. Uso de `ArgumentCaptor` verifica o estado gravado em repositórios, não só assertions sobre valor de retorno.

2. **Transações em nível correto:** `@Transactional` em methods que gravam, `readOnly=true` em leitura. Evita divergência de estado.

3. **Segurança de autorização:** Validação RN-012 delegada ao `PermissaoGuard`, testada com `AccessDeniedException`, nunca depende só de lógica em camada acima (RNF-003 ✅).

4. **Auditoria rastreável:** Cada alteração de etapa e responsável gera linha em `TarefaAuditoria` com campo específico, valor anterior/novo e data/hora. Facilita investigação de divergências.

5. **Separação de responsabilidades:** `TarefaService` concentra lógica de domínio; DTOs desacoplam requests/responses; `PermissaoGuard` centraliza autorização. Código limpo e testável.

6. **Congelamento com flexibilidade:** Bloqueia campos estruturais após iniciada, mas permite edição de responsável e movimentação. Comportamento sensato para domínio.

---

## Segurança

**Análise OWASP Top 10 mínima:**

| Vetor | Status | Evidência |
|---|---|---|
| A1: Broken Access Control | ✅ SEGURO | Autorização via PermissaoGuard em todo endpoint de escrita; validação de projeto finalizado (RN-015) |
| A3: Injection | ✅ SEGURO | JPA com parametrização automática; sem SQL concatenado; Mapper (MapStruct) sem template |
| A4: Insecure Design | ✅ SEGURO | RN-012 impedindo auto-concessão; validação no backend antes de persisted |
| A7: Cross-Site Scripting (XSS) | ⚠️ N/A | Frontend não implementado ainda (TASK-07.x); backend não renderiza HTML |
| A9: Using Components with Known Vulnerabilities | ✅ SEGURO | Dependências não alteradas nesta task; referência `pom.xml` para validação |

**Nenhum finding crítico de segurança identificado.**

---

## Conformidade com TechSpec

| Aspecto | Referência | Status | Observação |
|---|---|---|---|
| Transições configuráveis (RF-002) | TechSpec §5, data-model.md | ✅ | Validação via `Transicao.etapaOrigem/Destino` |
| Congelamento pós-início (RF-003) | TechSpec §5, Seção 7 | ✅ | Flag `iniciada` + validação no service |
| Lead-time por etapa (RF-006) | TechSpec §7, RN-001/002 | ✅ | Cálculo com `TarefaEtapaHistorico` + `TarefaImpedimentoHistorico` |
| Autoatribuição (RN-012) | TechSpec §5 (novo, comitê) | ✅ | PermissaoGuard.validarAutoatribuicaoRN012 |
| Auditoria (RF-017) | TechSpec §7 | ✅ | `TarefaAuditoria` gravada com autor/valores/data |
| Projeto finalizado (RN-015) | TechSpec §5 | ✅ | `exigirProjetoAtivo()` bloqueia toda escrita |
| RBAC backend (RNF-003) | TechSpec §5, security.md | ✅ | Validação via guard antes de qualquer persistência |

**Desvios:** Nenhum. Implementação segue especificação.

---

## Resultado

**✅ APROVADO**

- **Testes:** 15/15 passando ✅
- **Achados críticos:** 0
- **Achados importantes:** 1 (✅ **RESOLVIDO** — validação de entrada em EditarTarefaRequest)
- **Sugestões:** 2 (extract method, javadoc — não-bloqueantes)

**Liberado para merge — I1 resolvido em 2026-08-28.**

**Próximo passo:** 
- `/code-review TASK-04.2` concluído
- Próxima task recomendada: `/implement TASK-04.3` (Impedimento) — depende de TASK-04.1 ✅, TASK-04.2 ✅
- Ou: `/implement TASK-04.4` (Exclusão + auditoria leitura) — depende de TASK-04.1 ✅, TASK-02.3 ⏳

---

## Matriz de Verificação

- [x] Todos os 6 critérios de aceite implementados
- [x] 15 testes passando (100% verde)
- [x] TDD obrigatório aplicado (testes cobrem RF-002, RF-003, RF-006, RN-012)
- [x] Segurança: OWASP baseline coberto
- [x] RNF-003 (revalidação backend) coberto
- [x] Auditoria rastreável para RF-017
- [x] RN-015 (projeto finalizado) validado
- [x] Sem hardcode de secrets
- [x] Sem SQL injection
- [x] Nomenclatura Java seguindo padrão

---

## Handoff

- **Status:** APROVADO COM RESSALVAS
- **Issue bloqueante:** Validação de entrada em `editar()` (I1)
- **Canvas — Safeguards:** Nenhum novo guardrail identificado; atualizações já cobiertas em TASK-03.2
- **ADR:** Nenhuma debt técnica aceita nesta task
- **Recomendação:** Merge liberado; I1 deve ser resolvido antes de go-live

---

## Histórico de Revisões

| Versão | Data | Revisor | Status |
|---|---|---|---|
| 1.0 | 2026-08-28 | Claude Code / /code-review | APROVADO COM RESSALVAS (I1 importante, S1/S2 sugestões) |
