# ADR-006 — Sem fallback de autenticação local quando Keycloak indisponível

_Status: Aceito | Data: 2026-08-25 | Feature: kanban-tarefas_

## Contexto

`security.md` deixava em aberto um "fallback de autenticação própria caso Keycloak esteja indisponível". O PRD assume Keycloak como premissa já disponível (Seção 7) e RF-014 é Should Have.

## Decisão

Não implementar fallback de autenticação local. Se o Keycloak estiver indisponível, o login falha e o sistema exibe erro — sem mecanismo alternativo de senha própria.

## Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **Sem fallback (escolhida)** | Menor superfície de ataque; sem gestão de senha própria/reset; consistente com a premissa do PRD | Sistema fica indisponível se Keycloak cair — aceito como risco operacional de infraestrutura, fora do escopo funcional desta feature |
| Fallback local (usuário/senha) | Resiliência a indisponibilidade do IdP | Exige tabela de credenciais, hashing, fluxo de reset de senha — RNF/RF não previstos no PRD; amplia escopo de segurança sem requisito correspondente |

## Consequências

- Disponibilidade do sistema fica acoplada à disponibilidade do Keycloak (risco documentado na Seção 10 da TechSpec — Questões em Aberto / infraestrutura).
- Nenhuma entidade de credencial local é modelada no data model.

## Referências

RF-014, security.md.
