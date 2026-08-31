#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Reset Only Database — Limpa tabelas e re-executa Flyway migrations

.DESCRIPTION
  Deleta APENAS os dados do banco (mantém schema), sem deletar volumes Docker.
  Mais rápido que reset-dev-environment.ps1 pois não recria o Keycloak.

.PARAMETER Force
  Se $true, não pede confirmação

.EXAMPLE
  ./reset-database-only.ps1
  ./reset-database-only.ps1 -Force
#>

param(
  [switch]$Force
)

$ErrorActionPreference = "Stop"
$composePath = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "🗑️  Reset Database Only" -ForegroundColor Cyan
Write-Host ""

# Confirmação
if (-not $Force) {
  Write-Host "⚠️  Aviso: Isso vai deletar:" -ForegroundColor Yellow
  Write-Host "  • Todos os dados do banco PostgreSQL (tarefas, projetos, usuários)"
  Write-Host "  • Keycloak data: NÃO é afetado"
  Write-Host ""
  $confirm = Read-Host "Deseja continuar? (s/n)"
  if ($confirm -ne "s") {
    Write-Host "❌ Cancelado"
    exit 1
  }
}

Write-Host ""
Write-Host "📍 Conectando ao PostgreSQL..." -ForegroundColor Yellow

# Step 1: Conectar e deletar dados
$dbUser = "kanban"
$dbPassword = "kanban"
$dbName = "kanban"
$dbHost = "localhost"
$dbPort = 5432

# Construir string de conexão PSQL
$pgPassword = $dbPassword
$env:PGPASSWORD = $pgPassword

try {
  # Deletar tabelas em ordem reversa (foreign keys)
  Write-Host "  Deletando tabelas..." -NoNewline

  $sqlCommands = @(
    "DROP TABLE IF EXISTS tarefa_observador CASCADE;",
    "DROP TABLE IF EXISTS auditoria CASCADE;",
    "DROP TABLE IF EXISTS notificacao CASCADE;",
    "DROP TABLE IF EXISTS tarefa CASCADE;",
    "DROP TABLE IF EXISTS transicao CASCADE;",
    "DROP TABLE IF EXISTS etapa CASCADE;",
    "DROP TABLE IF EXISTS raia CASCADE;",
    "DROP TABLE IF EXISTS workflow CASCADE;",
    "DROP TABLE IF EXISTS usuario_projeto_papel CASCADE;",
    "DROP TABLE IF EXISTS papel_permissao CASCADE;",
    "DROP TABLE IF EXISTS permissao CASCADE;",
    "DROP TABLE IF EXISTS papel CASCADE;",
    "DROP TABLE IF EXISTS projeto CASCADE;",
    "DROP TABLE IF EXISTS usuario CASCADE;",
    "DROP TABLE IF EXISTS flyway_schema_history CASCADE;"
  )

  foreach ($sql in $sqlCommands) {
    $result = psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -c $sql 2>&1
    if ($LASTEXITCODE -ne 0) {
      Write-Host ""
      Write-Host "  ⚠️  SQL error: $result" -ForegroundColor Yellow
    }
  }

  Write-Host " ✅" -ForegroundColor Green

  # Step 2: Reiniciar backend para executar Flyway
  Write-Host ""
  Write-Host "📍 Reiniciando backend para reexecutar Flyway..." -ForegroundColor Yellow
  Set-Location $composePath
  docker compose restart backend

  # Aguardar
  Write-Host "  Aguardando backend iniciar..."
  Start-Sleep -Seconds 10

  # Verificar saúde
  Write-Host "  Verificando health..." -NoNewline
  for ($i = 0; $i -lt 30; $i++) {
    try {
      $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -ErrorAction Stop
      $health = $response.Content | ConvertFrom-Json
      if ($health.status -eq "UP") {
        Write-Host " ✅" -ForegroundColor Green
        break
      }
    } catch {
      # Aguardar
    }
    Start-Sleep -Seconds 1
  }

  Write-Host ""
  Write-Host "✅ Reset Concluído!" -ForegroundColor Green
  Write-Host ""
  Write-Host "📋 Estado:" -ForegroundColor Cyan
  Write-Host "  • Banco: Limpo, Flyway migrations reexecutadas"
  Write-Host "  • Keycloak: Intacto (usuários permanecem)"
  Write-Host "  • Backend: Reiniciado"

} catch {
  Write-Host ""
  Write-Host "❌ Erro: $_" -ForegroundColor Red
  exit 1
} finally {
  $env:PGPASSWORD = $null
}
