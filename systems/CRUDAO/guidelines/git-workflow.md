# Git Workflow — CRUDAO
_Versão: 1.0 | Data: 2026-08-22_

## Branching

- **Branch principal:** `main`.
- **Estratégia:** GitFlow simplificado — branches `feature/*`, `fix/*`, `release/*` etc.

## Commits e push

- Um commit/push por tarefa executada.
- Commit e push na branch atual são permitidos quando testes e lint passarem localmente (sem pipeline de CI/CD nesta fase).

## Merge/PR

- Merge/PR é conduzido manualmente por um humano — não há automação de merge.

## CI/CD

- Nenhum pipeline automatizado nesta fase.
- Testes, lint e build são executados localmente antes de commit/push.
- Deploy/validação local via Docker.

## Checks obrigatórios antes de commit/push

- Testes (unitários e de integração) passando.
- Lint (Spotless/Checkstyle no backend, ESLint/Prettier no frontend) sem erros.
