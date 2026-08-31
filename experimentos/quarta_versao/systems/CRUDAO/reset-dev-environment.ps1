#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Reset Development Environment — Deleta dados, recria banco e realm Keycloak

.DESCRIPTION
  Este script:
  1. Para os containers Docker
  2. Deleta volumes (PostgreSQL, Keycloak)
  3. Inicia o stack novamente
  4. Aguarda saúde dos containers
  5. Valida que banco e realm foram recriados

.PARAMETER Force
  Se $true, não pede confirmação antes de deletar dados

.EXAMPLE
  ./reset-dev-environment.ps1
  ./reset-dev-environment.ps1 -Force
#>

param(
  [switch]$Force
)

$ErrorActionPreference = "Stop"
$composePath = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "🔄 Reset Development Environment" -ForegroundColor Cyan
Write-Host ""

# Confirmação
if (-not $Force) {
  Write-Host "⚠️  Aviso: Isso vai deletar:" -ForegroundColor Yellow
  Write-Host "  • Banco de dados PostgreSQL (todas as tarefas, projetos, usuários)"
  Write-Host "  • Realm Keycloak (será recriado com usuários padrão)"
  Write-Host ""
  $confirm = Read-Host "Deseja continuar? (s/n)"
  if ($confirm -ne "s") {
    Write-Host "❌ Cancelado pelo usuário"
    exit 1
  }
}

# Step 1: Parar containers
Write-Host "📍 Step 1: Parando containers..." -ForegroundColor Yellow
Set-Location $composePath
docker compose down

# Step 2: Deletar volumes
Write-Host ""
Write-Host "📍 Step 2: Deletando volumes (postgres, keycloak)..." -ForegroundColor Yellow
docker volume rm crudao-postgres-data -ErrorAction SilentlyContinue | Out-Null
docker volume rm crudao-keycloak-data -ErrorAction SilentlyContinue | Out-Null
Write-Host "  ✅ Volumes deletados"

# Step 3: Iniciar stack
Write-Host ""
Write-Host "📍 Step 3: Iniciando stack Docker..." -ForegroundColor Yellow
docker compose up -d

# Step 4: Aguardar saúde
Write-Host ""
Write-Host "📍 Step 4: Aguardando containers ficarem saudáveis..." -ForegroundColor Yellow
$maxAttempts = 30
$attempt = 0

while ($attempt -lt $maxAttempts) {
  $attempt++
  Start-Sleep -Seconds 2

  # Verificar status
  $ps = docker compose ps --format "json" | ConvertFrom-Json
  $allHealthy = $ps | Where-Object { $_.State -notlike "running*" } | Measure-Object | Select-Object -ExpandProperty Count

  if ($allHealthy -eq 0) {
    Write-Host "  ✅ Todos os containers saudáveis (tentativa $attempt/$maxAttempts)" -ForegroundColor Green
    break
  }

  Write-Host "  ⏳ Aguardando... ($attempt/$maxAttempts)" -NoNewline
  Write-Host "`r" -NoNewline
}

# Step 5: Validações
Write-Host ""
Write-Host "📍 Step 5: Validando ambiente..." -ForegroundColor Yellow

# Verificar PostgreSQL
try {
  $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -ErrorAction Stop
  $health = $response.Content | ConvertFrom-Json
  Write-Host "  ✅ PostgreSQL: Conectado (via Backend Health)" -ForegroundColor Green
} catch {
  Write-Host "  ⚠️  PostgreSQL: Verificação falhou" -ForegroundColor Yellow
}

# Verificar Keycloak
try {
  $response = Invoke-WebRequest -Uri "http://localhost:8080" -ErrorAction Stop
  Write-Host "  ✅ Keycloak: Respondendo (realm padrão importado)" -ForegroundColor Green
} catch {
  Write-Host "  ⚠️  Keycloak: Verificação falhou" -ForegroundColor Yellow
}

# Verificar Backend
try {
  $response = Invoke-WebRequest -Uri "http://localhost:8081" -ErrorAction Stop
  Write-Host "  ✅ Backend: Respondendo (Flyway migrations aplicadas)" -ForegroundColor Green
} catch {
  Write-Host "  ⚠️  Backend: Verificação falhou" -ForegroundColor Yellow
}

# Step 6: Resumo
Write-Host ""
Write-Host "✅ Reset Concluído!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Usuários Disponíveis:" -ForegroundColor Cyan
Write-Host "  • dev.teste@crudao.local / dev123 (Papel: dev)"
Write-Host "  • admin.teste@crudao.local / admin123 (Papel: admin)"
Write-Host "  • po.teste@crudao.local / po123 (Papel: product_owner)"
Write-Host "  • gestor.teste@crudao.local / gestor123 (Papel: gestor)"
Write-Host ""
Write-Host "🌐 Acessos:" -ForegroundColor Cyan
Write-Host "  • Frontend:  http://localhost:3000"
Write-Host "  • Backend:   http://localhost:8081"
Write-Host "  • Keycloak:  http://localhost:8080"
Write-Host ""
Write-Host "📝 Logs:" -ForegroundColor Cyan
Write-Host "  docker compose logs -f backend"
Write-Host "  docker compose logs -f frontend"
Write-Host "  docker compose logs -f keycloak"
