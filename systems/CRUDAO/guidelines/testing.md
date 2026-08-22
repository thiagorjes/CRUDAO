# Testing — CRUDAO
_Versão: 1.0 | Data: 2026-08-22_

## Backend

- **Framework:** JUnit 5 + Testcontainers (para testes de integração com PostgreSQL real).

## Frontend

- **Framework:** Jest/Vitest + Testing Library.

## Cobertura mínima

- **TDD (lógica geral):** 80% de cobertura.
- **BDD (critérios de aceite mapeados no PRD/discovery):** 100% de cobertura dos cenários Gherkin definidos.

## Obrigatoriedade

TDD/BDD são obrigatórios sempre que aplicáveis — os casos a testar devem estar mapeados desde o discovery/PRD (critérios de aceite em Gherkin). Toda RF Must Have do PRD deve ter cenário de teste correspondente antes de considerar a task concluída.
