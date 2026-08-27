# ADR-005 — Flyway para versionamento de schema

_Status: Aceito | Data: 2026-08-25 | Feature: kanban-tarefas_

## Contexto

`stack.md` define PostgreSQL como banco principal via Spring Data JPA/Hibernate, mas não define ferramenta de versionamento de schema. É necessário decidir antes de modelar `data-model.md` e as migrations iniciais.

## Decisão

Usar **Flyway** (`spring-boot-starter` `flyway-core` + `flyway-database-postgresql`) para todas as migrations de schema, com scripts SQL versionados em `backend/src/main/resources/db/migration/V{n}__{descricao}.sql`. `ddl-auto` do Hibernate fica em `validate` (nunca `update`/`create`) — o schema é sempre fonte de verdade via Flyway, nunca gerado automaticamente pelo Hibernate.

## Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **Flyway (escolhida)** | Integração nativa com Spring Boot autoconfigure; scripts SQL simples e auditáveis; padrão de mercado no ecossistema Spring | Menos flexível que Liquibase para rollback automático complexo (não necessário nesta fase) |
| Liquibase | Suporta XML/YAML/JSON além de SQL; rollback declarativo | Curva de aprendizado maior sem ganho concreto para o escopo atual |
| Hibernate `ddl-auto=update` | Zero esforço de setup | Não versiona schema, risco alto de divergência entre ambientes e perda de dados em produção — inaceitável para RNF-002 (múltiplas instâncias) |

## Consequências

- Toda entidade nova ou alteração de coluna exige uma migration Flyway correspondente.
- `data-model.md` referenciará o número da migration que cria cada tabela.

## Referências

RNF-002, RNF-004.
