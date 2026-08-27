---
id: ADR-006
type: ADR
status: accepted
date: 2026-08-23
supersedes: —
superseded-by: —
---

# ADR-006 — Enforcement de RBAC por projeto: checagem explícita no Service, não mais só AOP genérico

## Decisão

`@ExigePermissao` (validado por `PermissaoAspect`, hoje checando só o papel global único do usuário) continua existindo, mas passa a valer **apenas para ações verdadeiramente globais** (CRUD de Papel/Permissão — RF-013; edição de usuário pelo admin global — RF-015). Para toda ação escopada a um projeto (CRUD de Workflow/Etapa/Transição/Raia, CRUD de Tarefa, impedimento, associação de membro ao projeto, configuração de toggles, finalização de projeto), a checagem passa a ser uma chamada explícita a um novo `AutorizacaoProjetoService.exigirPermissao(UUID usuarioId, UUID projetoId, String permissao)`, feita dentro de cada Service, no ponto em que o `projetoId` do recurso já foi resolvido (seja recebido diretamente, seja obtido ao carregar a entidade antes da escrita).

`AutorizacaoProjetoService` decide com esta ordem:
1. Usuário tem papel global `admin` → autorizado, sem consultar projeto.
2. Projeto tem `data_finalizacao` preenchida e a operação é de escrita → 403/409, mesmo que o usuário tivesse a permissão (RN-015). **(Revisão — achado do comitê de análise, architect):** esta checagem é feita dentro do próprio `exigirPermissao`, no mesmo ponto único, e não replicada como validação separada em cada Service — reduz a superfície de esquecimento a um só lugar.
3. Usuário tem, para aquele `projetoId`, ao menos um papel (`UsuarioProjetoPapel`) cujo conjunto de permissões acumuladas contém a chave exigida → autorizado.
4. Caso contrário → `AcessoNegadoException` (403).

**`papel:gerenciar` é caso especial (revisão — achado do comitê de análise, security):** essa chave nunca é atribuível via `UsuarioProjetoPapel` — não entra no catálogo de permissões que um `project_admin` (ou qualquer papel de projeto) pode ter. `PapelController` (RF-013) continua usando `@ExigePermissao("papel:gerenciar")`, mas `PermissaoAspect`, pós-refatoração, checa essa chave exclusivamente contra `Usuario.admin` — nunca contra papéis de projeto. Isso fecha o vetor "project_admin manipula permissões de um papel existente para escalar privilégio", identificado na revisão: sem `papel:gerenciar` no catálogo por projeto, não há caminho de `UsuarioProjetoPapel` até lá. RN-006 (PRD) foi marcada como superseded em parte por este motivo (PRD v1.3).

## Motivação

O `PermissaoAspect` atual assume "1 usuário = 1 papel global" e não tem acesso ao `projetoId` do recurso sendo escrito — generalizar isso via AOP puro exigiria um mecanismo de resolução de `projetoId` por assinatura de método (reflection sobre parâmetros/anotações) que reintroduz complexidade e um ponto único de erro sutil (resolver o projeto errado silenciosamente). Os Services já carregam a entidade (ou já recebem `projetoId` explícito, como em `RaiaController.listarParaProjeto`) antes de qualquer escrita — colocar a checagem ali, explicitamente, é mais simples de auditar e consistente com o padrão de "revalidar no ponto de uso" já adotado no frontend (G-AUTH da TASK-05.0, `returnTo`).

**Problema que resolve:**
Permitir que a mesma permissão (ex.: `workflow:gerenciar`) autorize um usuário em um projeto e não em outro, sem um mecanismo genérico de resolução de projeto que seria frágil.

**Restrições consideradas:**
- RNF-003 (reforçada no PRD v1.2): nenhuma autorização pode depender de dado enviado pelo cliente — `AutorizacaoProjetoService` sempre recebe `projetoId` resolvido no backend (da entidade carregada ou do path), nunca aceita um `projetoId` de payload sem cruzar com o recurso real.
- BDR-001: papel `admin` continua global; demais papéis por par (usuário, projeto).

## Consequências

**Positivas:**
- Checagem de autorização fica no mesmo lugar onde o `projetoId` já é conhecido com certeza — sem resolução mágica via reflection.
- `@ExigePermissao`/`PermissaoAspect` continuam existindo e válidos para o subconjunto de ações realmente globais, sem retrabalho ali.

**Negativas / trade-offs:**
- Perde-se a garantia "por construção" do AOP (é possível esquecer de chamar `AutorizacaoProjetoService` num Service novo). **Mitigação reforçada (achado convergente do comitê — security e architect apontaram o mesmo risco):** além do teste de integração dedicado por endpoint (403), a suíte de testes ganha um teste estrutural (grep/reflection sobre os métodos públicos de `@Service` no pacote `domain.*` que gravam entidade com `projetoId`) que falha o build se algum não contiver chamada a `autorizacaoProjetoService.exigirPermissao` — não é análise estática perfeita, mas transforma "esquecimento silencioso" em falha de CI, não em achado de code review. Registrado como guardrail no canvas (dimensão S).
- Todos os Services de CRUD administrativo (Workflow, Etapa, Transição, Raia) e o de Tarefa precisam de retrabalho para trocar `@ExigePermissao("x")` por `autorizacaoProjetoService.exigirPermissao(usuarioAtual, projetoId, "x")`.

**Downstream afetado:**
- TASK-04.1 (RBAC): retrabalho do modelo (`UsuarioProjetoPapel` substitui `Usuario.papel_id` como fonte principal, mantendo um campo booleano/flag para o `admin` global) e do enforcement.
- Todas as tasks de CRUD administrativo já implementadas (TASK-01.1, TASK-01.2, TASK-02.1) precisam de ajuste nos Services para passar a resolver e checar `projetoId`.

## Alternativas Consideradas

### Alternativa 1 — Generalizar `@ExigePermissao` com resolução de `projetoId` via SpEL/reflection sobre os parâmetros do método
**Descartada porque:** exige anotar cada endpoint com a expressão de onde extrair o `projetoId` (do path, do body, ou "resolver via Service X") — na prática tão ou mais verboso que a chamada explícita, mas com o risco adicional de resolver o projeto errado silenciosamente se a expressão estiver mal escrita, sem o compilador ajudando.

### Alternativa 2 — Middleware/filter HTTP que resolve `projetoId` da URL antes do Controller
**Descartada porque:** nem todo endpoint tem `projetoId` na URL (ex.: `PUT /api/workflows/{id}` — o id é do Workflow, não do Projeto); o filter precisaria da mesma lógica de "carregar entidade para achar o projeto" que o Service já faz, duplicando trabalho numa camada mais distante do domínio.
