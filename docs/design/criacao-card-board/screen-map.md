# Screen Map: Criação e gerenciamento de cards no board

**Gerado em:** 2026-08-24
**PRD:** docs/prd/criacao-card-board-prd.md
**Design Brief:** docs/design/criacao-card-board-design-brief.md

## Cobertura de RFs

| RF | Descrição | Tela(s) | Status |
|----|-----------|---------|--------|
| RF-001 | Criar card pelo board | TL-01, TL-02 | coberto |
| RF-002 | Excluir card pelo board | TL-03, TL-04 | coberto |

## Inventário de Telas

| ID | Nome | Rota | Estados cobertos no protótipo |
|----|------|------|-------------------------------|
| TL-01 | Board com botão "Novo card" | `/` (existente) | default (botão visível), oculto (sem permissão) |
| TL-02 | Modal "Novo card" | overlay sobre `/` | idle, erro de validação (título vazio), salvando, sucesso |
| TL-03 | Card com ícone de exclusão | `/` (existente) | visível (com permissão), oculto (sem permissão/toggle) |
| TL-04 | Modal de confirmação de exclusão | overlay sobre `/` | idle, loading (excluindo) |

## Fluxos

**Happy path criação:** TL-01 (clique "Novo card") → TL-02 (idle → preenchido → salvando → sucesso) → fecha modal, toast, card aparece na coluna 0/primeira raia
**Erro de validação:** TL-02 (idle) → salvar sem título → TL-02 (erro, campo sinalizado, sem chamada à API) → corrige → salvando → sucesso
**Happy path exclusão:** TL-03 (clique lixeira) → TL-04 (idle) → confirma → TL-04 (loading) → card removido, modal fecha
**Cancelamento:** TL-04 (idle) → cancelar → modal fecha, nenhuma alteração

## Gaps Identificados

Nenhum. Todos os RFs Must Have (RF-001, RF-002) têm tela mapeada e todos os estados obrigatórios do design brief (§6) estão prototipados. Decisão de evento `TAREFA_EXCLUIDA` (tempo real) é de backend/techspec, não afeta o protótipo visual desta feature.
