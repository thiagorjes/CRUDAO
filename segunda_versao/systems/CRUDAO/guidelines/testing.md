# Testing — CRUDAO
_Versão: 1.0 | Data: 2026-08-22_

## Backend

- **Framework:** JUnit 5 + Testcontainers (para testes de integração com PostgreSQL real).
- **Pré-requisito de ambiente (achado TASK-02.3):** `SecurityConfig` registra `spring-boot-starter-oauth2-client`, cujo autoconfigure resolve o _issuer_ OIDC (`ClientRegistrationRepository`) **eagerly na subida do `ApplicationContext`** — qualquer teste que suba contexto Spring completo (`@SpringBootTest`, ou `@WebMvcTest` que importe a configuração de segurança real) exige um Keycloak acessível em `http://localhost:8080/realms/kanban-dev`. Antes de rodar `mvn test` localmente: `docker compose up -d keycloak postgres` (na raiz de `systems/CRUDAO/`) e aguardar `healthy`. Testes que usam apenas mocks (Mockito, sem `@SpringBootTest`) não precisam disso.

## Frontend

- **Framework:** Jest/Vitest + Testing Library.

## Cobertura mínima

- **TDD (lógica geral):** 80% de cobertura.
- **BDD (critérios de aceite mapeados no PRD/discovery):** 100% de cobertura dos cenários Gherkin definidos.

## Obrigatoriedade

TDD/BDD são obrigatórios sempre que aplicáveis — os casos a testar devem estar mapeados desde o discovery/PRD (critérios de aceite em Gherkin). Toda RF Must Have do PRD deve ter cenário de teste correspondente antes de considerar a task concluída.
