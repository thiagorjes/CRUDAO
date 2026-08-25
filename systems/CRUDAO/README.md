# CRUDAO — Kanban de Tarefas

Setup local (TASK-01.1): ver [docs/techspec/kanban-tarefas/quickstart.md](../../docs/techspec/kanban-tarefas/quickstart.md).

```bash
docker compose up postgres keycloak   # Postgres + Keycloak dev (realm kanban-dev importado automaticamente)
cd backend && ./mvnw spring-boot:run  # backend (profile dev)
cd frontend && npm install && npm run dev
```

Keycloak admin console: http://localhost:8080 (admin/admin). Usuários de teste do realm `kanban-dev`: `dev.teste` / `dev123` (papel `dev`) e `admin.teste` / `admin123` (papel `admin`).
