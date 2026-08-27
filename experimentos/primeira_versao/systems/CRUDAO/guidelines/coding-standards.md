# Coding Standards — CRUDAO
_Versão: 1.0 | Data: 2026-08-22_

## Backend (Java)

- **Linter/formatter:** Spotless + Checkstyle (padrão default do ecossistema Spring — sem regra customizada definida ainda).
- **Nomenclatura:** convenção padrão Java (camelCase para métodos/variáveis, PascalCase para classes).
- **Tamanho de função/método:** sem limite rígido definido nesta fase.
- **Lombok:** usar para reduzir boilerplate (getters/setters/construtores), evitando lógica de negócio em anotações.
- **MapStruct:** usar para toda conversão entidade↔DTO — proibido mapeamento manual repetitivo.

## Frontend (Next.js/TypeScript)

- **Linter/formatter:** ESLint + Prettier (configuração default do Next.js — sem regra customizada definida ainda).
- **Nomenclatura:** camelCase para variáveis/funções, PascalCase para componentes.

## Geral

- Sem convenções de nomenclatura de domínio específicas definidas nesta fase — revisar quando surgir necessidade concreta.
- **Evitar nomes de campo boolean com duas letras maiúsculas seguidas após o prefixo (ex.: `eFinal`, `xATivo`).** A introspecção padrão de JavaBeans (usada por Jackson, MapStruct e afins) só decapitaliza a primeira letra quando ela não é seguida por outra maiúscula — `isEFinal()` vira a propriedade `EFinal`, não `eFinal`, quebrando serialização/mapeamento silenciosamente (sem erro de compilação ou teste óbvio). Achado real na TASK-01.1 (`Etapa.eFinal` → renomeado para `etapaFinal`). Prefira nomes onde a letra após o prefixo já é minúscula.
