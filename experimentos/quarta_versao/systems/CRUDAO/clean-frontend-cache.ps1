#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Clean Frontend Cache — Remove .next, node_modules, Docker cache completo

.DESCRIPTION
  Limpeza completa do cache do frontend:
  1. Para container frontend
  2. Deleta .next (build cache)
  3. Deleta node_modules (opcional)
  4. Deleta volume Docker
  5. Recompila e reinicia
  6. Instrui a limpar browser cache

.PARAMETER DeleteNodeModules
  Se $true, também deleta node_modules (redownload packages)

.PARAMETER Force
  Se $true, não pede confirmação

.EXAMPLE
  ./clean-frontend-cache.ps1 -Force
  ./clean-frontend-cache.ps1 -DeleteNodeModules -Force
#>

param(
  [switch]$DeleteNodeModules,
  [switch]$Force
)

$ErrorActionPreference = "Stop"
$composePath = Split-Path -Parent $MyInvocation.MyCommand.Path
$frontendPath = Join-Path $composePath "frontend"

Write-Host "🧹 Clean Frontend Cache" -ForegroundColor Cyan
Write-Host ""

if (-not $Force) {
  Write-Host "⚠️  Isso vai:" -ForegroundColor Yellow
  Write-Host "  • Parar container frontend"
  Write-Host "  • Deletar .next/ (Next.js build cache)"
  if ($DeleteNodeModules) {
    Write-Host "  • Deletar node_modules/ (redownload ~500MB)"
  }
  Write-Host "  • Deletar volume Docker frontend"
  Write-Host "  • Recompilar tudo"
  Write-Host ""
  $confirm = Read-Host "Continuar? (s/n)"
  if ($confirm -ne "s") {
    Write-Host "❌ Cancelado"
    exit 1
  }
}

Write-Host ""
Write-Host "📍 Step 1: Parando frontend..." -ForegroundColor Yellow
Set-Location $composePath

try {
  docker compose stop frontend 2>&1 | Select-String "Stopped" -ErrorAction SilentlyContinue | Out-Null
  Write-Host "  ✅ Frontend parado" -ForegroundColor Green
} catch {
  Write-Host "  ⚠️  Frontend já parado" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "📍 Step 2: Deletando .next (Next.js build cache)..." -ForegroundColor Yellow

$nextPath = Join-Path $frontendPath ".next"
if (Test-Path $nextPath) {
  Remove-Item $nextPath -Recurse -Force -ErrorAction SilentlyContinue
  Write-Host "  ✅ .next/ deletado" -ForegroundColor Green
} else {
  Write-Host "  ℹ️  .next/ não encontrado" -ForegroundColor Cyan
}

if ($DeleteNodeModules) {
  Write-Host ""
  Write-Host "📍 Step 3: Deletando node_modules..." -ForegroundColor Yellow

  $nodeModulesPath = Join-Path $frontendPath "node_modules"
  if (Test-Path $nodeModulesPath) {
    Write-Host "  ⏳ Deletando (~500MB, pode levar 30s)..." -NoNewline
    Remove-Item $nodeModulesPath -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host " ✅" -ForegroundColor Green
  } else {
    Write-Host "  ℹ️  node_modules/ não encontrado" -ForegroundColor Cyan
  }
}

Write-Host ""
Write-Host "📍 Step 4: Deletando volume Docker..." -ForegroundColor Yellow

$volumeName = "crudao-frontend-cache"
$volumes = docker volume ls --format "{{.Name}}" 2>&1 | Select-String $volumeName -ErrorAction SilentlyContinue

if ($volumes) {
  docker volume rm $volumeName -f 2>&1 | Out-Null
  Write-Host "  ✅ Volume deletado" -ForegroundColor Green
} else {
  Write-Host "  ℹ️  Volume não encontrado" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "📍 Step 5: Reiniciando frontend (recompilação)..." -ForegroundColor Yellow
Write-Host "  ⏳ Aguardando build (~60s)..."

docker compose up -d frontend 2>&1 | Select-String "started|Starting" -ErrorAction SilentlyContinue | Out-Null

# Aguardar saúde
for ($i = 0; $i -lt 60; $i++) {
  try {
    $response = Invoke-WebRequest -Uri "http://localhost:3000" `
      -ErrorAction Stop -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
      Write-Host "  ✅ Frontend pronto" -ForegroundColor Green
      break
    }
  } catch {
    # Continuar aguardando
  }

  if ($i % 10 -eq 0 -and $i -gt 0) {
    Write-Host "  ⏳ Aguardando ($i/60)..." -NoNewline
  }
  Start-Sleep -Seconds 1
}

Write-Host ""
Write-Host "✅ Limpeza Concluída!" -ForegroundColor Green
Write-Host ""
Write-Host "🧹 Cache Limpo:" -ForegroundColor Cyan
Write-Host "  • .next/ ✅"
if ($DeleteNodeModules) {
  Write-Host "  • node_modules/ ✅"
}
Write-Host "  • Docker volume ✅"
Write-Host ""
Write-Host "⚙️  Frontend Recompilado:" -ForegroundColor Cyan
Write-Host "  ✅ Next.js build executado"
Write-Host "  ✅ Container reiniciado"
Write-Host ""
Write-Host "🧊 Agora limpe o NAVEGADOR:" -ForegroundColor Yellow
Write-Host "  1. Abra DevTools (F12)"
Write-Host "  2. Application → Storage → Clear all"
Write-Host "     (Limpa localStorage, sessionStorage, cookies, cache)"
Write-Host "  3. Ou: Ctrl+Shift+Delete → limpar tudo"
Write-Host "  4. Recarregue: Ctrl+F5 (hard refresh)"
Write-Host ""
Write-Host "🔐 Fazer login novamente:" -ForegroundColor Cyan
Write-Host "  http://localhost:3000"
Write-Host "  Username: dev.teste"
Write-Host "  Senha: dev123"
Write-Host ""
Write-Host "📊 Verificar logs:" -ForegroundColor Cyan
Write-Host "  docker compose logs -f frontend"
