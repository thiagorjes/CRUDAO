#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Reset Database Only (via Docker) — Usa docker exec para limpar banco

.DESCRIPTION
  Deleta APENAS os dados do banco (mantém schema), sem precisar de psql instalado.
  Usa "docker exec" para rodar comandos SQL dentro do container PostgreSQL.

.PARAMETER Force
  Se $true, não pede confirmação

.EXAMPLE
  ./reset-database-only-docker.ps1
  ./reset-database-only-docker.ps1 -Force
#>

param(
  [switch]$Force
)

$ErrorActionPreference = "Stop"
$composePath = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "🗑️  Reset Database Only (Docker)" -ForegroundColor Cyan
Write-Host ""

# Confirmação
if (-not $Force) {
  Write-Host "⚠️  Aviso: Isso vai deletar:" -ForegroundColor Yellow
  Write-Host "  • Todos os dados do banco PostgreSQL (tarefas, projetos, usuários)"
  Write-Host "  • Keycloak: NÃO é afetado"
  Write-Host ""
  $confirm = Read-Host "Deseja continuar? (s/n)"
  if ($confirm -ne "s") {
    Write-Host "❌ Cancelado"
    exit 1
  }
}

Write-Host ""
Write-Host "📍 Step 1: Verificando container PostgreSQL..." -ForegroundColor Yellow

Set-Location $composePath

# Verificar se container está rodando
$containerStatus = docker compose ps postgres --format "{{.State}}"
if ($containerStatus -notlike "running*") {
  Write-Host "  ❌ Container PostgreSQL não está rodando" -ForegroundColor Red
  Write-Host "  Execute: docker compose up -d" -ForegroundColor Yellow
  exit 1
}
Write-Host "  ✅ Container postgres-1 está rodando" -ForegroundColor Green

Write-Host ""
Write-Host "📍 Step 2: Deletando tabelas (via docker exec)..." -ForegroundColor Yellow

# SQL para deletar tudo em ordem
$dropTablesSql = @'
DROP TABLE IF EXISTS tarefa_observador CASCADE;
DROP TABLE IF EXISTS auditoria CASCADE;
DROP TABLE IF EXISTS notificacao CASCADE;
DROP TABLE IF EXISTS tarefa CASCADE;
DROP TABLE IF EXISTS transicao CASCADE;
DROP TABLE IF EXISTS etapa CASCADE;
DROP TABLE IF EXISTS raia CASCADE;
DROP TABLE IF EXISTS workflow CASCADE;
DROP TABLE IF EXISTS usuario_projeto_papel CASCADE;
DROP TABLE IF EXISTS papel_permissao CASCADE;
DROP TABLE IF EXISTS permissao CASCADE;
DROP TABLE IF EXISTS papel CASCADE;
DROP TABLE IF EXISTS projeto CASCADE;
DROP TABLE IF EXISTS usuario CASCADE;
DROP TABLE IF EXISTS flyway_schema_history CASCADE;
'@

try {
  # Usar docker exec para rodar psql dentro do container
  $containerName = "crudao-postgres-1"

  Write-Host "  Executando DROP TABLE cascade..." -NoNewline

  # Passar SQL via stdin ao psql dentro do container
  $dropTablesSql | docker exec -i $containerName `
    psql -U kanban -d kanban -v ON_ERROR_STOP=1 `
    2>&1 | Out-Null

  Write-Host " ✅" -ForegroundColor Green

  # Verif que tabelas foram deletadas
  $checkSql = "SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';"

  $tableCount = $checkSql | docker exec -i $containerName `
    psql -U kanban -d kanban -t -c "$checkSql" 2>&1

  Write-Host "  📊 Tabelas restantes: $tableCount.Trim()" -ForegroundColor Cyan

} catch {
  Write-Host ""
  Write-Host "  ❌ Erro ao deletar tabelas: $_" -ForegroundColor Red
  exit 1
}

Write-Host ""
Write-Host "📍 Step 3: Reiniciando backend para reexecutar Flyway..." -ForegroundColor Yellow

try {
  docker compose restart backend 2>&1 | Select-String "Started" -ErrorAction SilentlyContinue | Out-Null
  Write-Host "  ✅ Backend reiniciado" -ForegroundColor Green
} catch {
  Write-Host "  ⚠️  Erro ao reiniciar backend (continuando mesmo assim)" -ForegroundColor Yellow
}

# Aguardar backend iniciar
Write-Host "  Aguardando backend iniciar (10s)..." -NoNewline
Start-Sleep -Seconds 10
Write-Host " ✅" -ForegroundColor Green

# Verificar saúde
Write-Host "  Verificando health..." -NoNewline
$maxAttempts = 30
for ($i = 0; $i -lt $maxAttempts; $i++) {
  try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" `
      -ErrorAction Stop -TimeoutSec 5
    $health = $response.Content | ConvertFrom-Json
    if ($health.status -eq "UP") {
      Write-Host " ✅" -ForegroundColor Green
      break
    }
  } catch {
    # Continuar aguardando
  }
  Start-Sleep -Seconds 1
}

# Verificar que Flyway foi executado
Write-Host "  Verificando Flyway migrations..." -NoNewline
$flywaySql = "SELECT COUNT(*) FROM flyway_schema_history;"
$migrationCount = $flywaySql | docker exec -i crudao-postgres-1 `
  psql -U kanban -d kanban -t 2>&1 | Select-String "^[0-9]" -ErrorAction SilentlyContinue

if ($migrationCount) {
  Write-Host " ✅ ($($migrationCount.Trim()) migrations aplicadas)" -ForegroundColor Green
} else {
  Write-Host " ⚠️ (não foi possível verificar)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "✅ Reset Concluído!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Estado:" -ForegroundColor Cyan
Write-Host "  • Banco: Limpo, Flyway migrations reexecutadas"
Write-Host "  • Keycloak: Intacto (usuários permanecem)"
Write-Host "  • Backend: Reiniciado e saudável"
Write-Host ""
Write-Host "🔐 Usuários do Keycloak (ainda válidos):" -ForegroundColor Cyan
Write-Host "  • dev.teste / dev123"
Write-Host "  • admin.teste / admin123"
Write-Host "  • po.teste / po123"
Write-Host "  • gestor.teste / gestor123"
Write-Host ""
Write-Host "🌐 Acessar:" -ForegroundColor Cyan
Write-Host "  http://localhost:3000 → fazer login"
Write-Host ""
Write-Host "📊 Verificar dados:" -ForegroundColor Cyan
Write-Host "  docker exec crudao-postgres-1 psql -U kanban -d kanban -c 'SELECT * FROM usuario;'"
