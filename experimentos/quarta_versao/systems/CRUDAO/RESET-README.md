# 🔄 Scripts de Reset — Ambiente de Desenvolvimento

Conjunto de scripts PowerShell para resetar/limpar o ambiente de desenvolvimento CRUDAO.

## 📋 Scripts Disponíveis

### 1. **reset-dev-environment.ps1** — Reset Completo

**O que faz:**
- ⏹️ Para todos os containers Docker
- 🗑️ Deleta volumes (PostgreSQL + Keycloak)
- 🚀 Inicia stack novamente
- 📦 PostgreSQL recria schema via Flyway
- 🔐 Keycloak reimporta realm com usuários padrão
- ✅ Valida saúde dos serviços

**Quando usar:**
- Depois de mudanças estruturais (migrations, realm config)
- Quando quer um ambiente limpo do zero
- Para testes de integração completos

**Uso:**

```powershell
cd systems/CRUDAO
./reset-dev-environment.ps1

# Sem confirmação (force)
./reset-dev-environment.ps1 -Force
```

**Tempo:** ~1-2 minutos

---

### 2. **reset-database-only.ps1** — Reset Apenas Banco

**O que faz:**
- 🗑️ Deleta TODOS os dados do banco PostgreSQL
- 📦 Re-executa Flyway migrations
- 🔐 Keycloak não é afetado (usuários permanecem)
- 🚀 Reinicia backend
- ✅ Valida saúde

**Quando usar:**
- Teste de fluxos de dados
- Limpar tarefas/projetos criadas durante testes
- Mais rápido que reset completo

**Uso:**

```powershell
cd systems/CRUDAO
./reset-database-only.ps1

# Sem confirmação
./reset-database-only.ps1 -Force
```

**Tempo:** ~30 segundos

**Pré-requisito:** 
- PostgreSQL client (`psql`) instalado e no PATH
- Banco deve estar rodando (`docker compose ps`)

---

### 3. **create-keycloak-users.ps1** — Criar Usuários Keycloak

**O que faz:**
- 🔐 Conecta à API REST do Keycloak
- 👥 Cria usuários padrão (dev.teste, admin.teste, po.teste)
- 🎯 Atribui roles (dev, admin, product_owner)
- ✅ Verifica se usuário já existe (evita duplicatas)

**Usuários Criados:**

| Username | Senha | Email | Roles |
|----------|-------|-------|-------|
| dev.teste | dev123 | dev.teste@crudao.local | dev |
| admin.teste | admin123 | admin.teste@crudao.local | admin |
| po.teste | po123 | po.teste@crudao.local | product_owner |
| gestor.teste | gestor123 | gestor.teste@crudao.local | gestor |

**Quando usar:**
- Depois de deletar realm do Keycloak manualmente
- Para adicionar novos usuários programaticamente
- Customizar usuários para testes

**Uso:**

```powershell
cd systems/CRUDAO
./create-keycloak-users.ps1

# Customizar parâmetros
./create-keycloak-users.ps1 `
  -KeycloakUrl "http://localhost:8080" `
  -Realm "kanban-dev" `
  -AdminUser "admin" `
  -AdminPassword "admin"
```

**Pré-requisito:**
- Keycloak rodando em http://localhost:8080
- Credenciais admin válidas

---

## 🎯 Fluxos de Teste Comuns

### Cenário A: Começar do Zero
```powershell
# Reset completo (banco + keycloak)
./reset-dev-environment.ps1 -Force

# Resultado: Ambiente limpo, usuários padrão criados
```

### Cenário B: Limpar Apenas Dados
```powershell
# Reset do banco (keycloak intacto)
./reset-database-only.ps1 -Force

# Usar mesmos usuários do Keycloak para login
```

### Cenário C: Adicionar Usuário Novo
```powershell
# Edit create-keycloak-users.ps1 e adicione na array $users
# Depois execute:
./create-keycloak-users.ps1
```

### Cenário D: Testes E2E Repetidos
```powershell
# Entre os testes:
./reset-database-only.ps1 -Force

# Banco limpo, Keycloak com mesmos usuários
# Tempo: ~30s vs ~2min (reset completo)
```

---

## 🔧 Troubleshooting

### Erro: "psql: command not found"
**Solução:**
- Instalar PostgreSQL Client: `choco install postgresql`
- Ou adicionar caminho ao PATH: `C:\Program Files\PostgreSQL\16\bin`

### Erro: "Connection refused" no Keycloak
**Solução:**
- Verificar que containers estão rodando: `docker compose ps`
- Aguardar mais tempo: `Start-Sleep -Seconds 10`

### Erro: "Ambiguous app routes" após reset
**Solução:**
- Frontend precisa recompilar: `docker compose restart frontend`
- Aguardar 10s antes de acessar http://localhost:3000

### Usuários não aparecem no Keycloak
**Solução:**
- Verificar admin console: http://localhost:8080
- Realm deve ser "kanban-dev"
- Executar: `./create-keycloak-users.ps1`

---

## 📊 Estado Após Reset

### reset-dev-environment.ps1
```
PostgreSQL:  ✅ Limpo, Flyway V1-V7 aplicadas
Keycloak:    ✅ Realm importado (realm-export.json)
Backend:     ✅ Pronto (migrations Ok)
Frontend:    ✅ Pronto
Usuários:    ✅ dev.teste, admin.teste, po.teste
```

### reset-database-only.ps1
```
PostgreSQL:  ✅ Limpo, Flyway V1-V7 aplicadas
Keycloak:    ⏸️ Intacto (mesmos usuários)
Backend:     ✅ Reiniciado
Frontend:    ⏸️ Não afetado
Usuários:    ⏸️ Permanecem do reset anterior
```

---

## 🔗 Acessos Após Reset

| Serviço | URL | Admin | Senha |
|---------|-----|-------|-------|
| Frontend | http://localhost:3000 | dev.teste | dev123 |
| Backend | http://localhost:8081 | — | — |
| Keycloak | http://localhost:8080 | admin | admin |
| PostgreSQL | localhost:5432 | kanban | kanban |

---

## 📝 Logs

Ver logs durante/após reset:

```powershell
# Backend
docker compose logs -f backend

# Keycloak
docker compose logs -f keycloak

# PostgreSQL
docker compose logs -f postgres

# Frontend
docker compose logs -f frontend
```

---

## ⚠️ Notas Importantes

1. **Dados Perdidos:** Reset deleta TODOS os dados. Não há backup automático.
2. **Migrations:** Flyway é idempotente — rodar 2x é seguro.
3. **Realm Export:** `keycloak/realm-export.json` é fonte de verdade para configuração Keycloak.
4. **Performance:** Reset completo é mais lento mas mais confiável para testes críticos.

---

## 🚀 Próximos Passos

Após reset, você pode:

1. **Fazer login:** http://localhost:3000 (usar dev.teste/dev123)
2. **Criar projeto:** Via UI
3. **Testar TASK-07.3:** Navegar board → clicar card → ver detalhe
4. **Executar testes:** `npm test` (frontend) ou `mvn test` (backend)

---

**Versão:** 1.0 | **Data:** 2026-08-31 | **Autor:** Sistema Kanban CRUDAO
