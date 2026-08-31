#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Create Keycloak Users via API — Cria usuários no Keycloak programaticamente

.DESCRIPTION
  Usa API REST do Keycloak para criar/atualizar usuários.
  Requer que Keycloak esteja rodando em http://localhost:8080

.PARAMETER Users
  Array de PSCustomObject com campos: username, email, password, firstName, lastName, roles

.EXAMPLE
  ./create-keycloak-users.ps1

.EXAMPLE
  # Criar usuários customizados
  $customUsers = @(
    @{ username = "user1"; email = "user1@example.com"; password = "pass123"; firstName = "User"; lastName = "One"; roles = @("dev") }
  )
  ./create-keycloak-users.ps1
#>

param(
  [string]$KeycloakUrl = "http://localhost:8080",
  [string]$Realm = "kanban-dev",
  [string]$AdminUser = "admin",
  [string]$AdminPassword = "admin"
)

$ErrorActionPreference = "Stop"

Write-Host "🔐 Create Keycloak Users" -ForegroundColor Cyan
Write-Host ""

# Usuários padrão
$users = @(
  @{
    username  = "dev.teste"
    email     = "dev.teste@crudao.local"
    password  = "dev123"
    firstName = "Dev"
    lastName  = "Teste"
    roles     = @("dev")
  },
  @{
    username  = "admin.teste"
    email     = "admin.teste@crudao.local"
    password  = "admin123"
    firstName = "Admin"
    lastName  = "Teste"
    roles     = @("admin")
  },
  @{
    username  = "po.teste"
    email     = "po.teste@crudao.local"
    password  = "po123"
    firstName = "PO"
    lastName  = "Teste"
    roles     = @("product_owner")
  },
  @{
    username  = "gestor.teste"
    email     = "gestor.teste@crudao.local"
    password  = "gestor123"
    firstName = "Gestor"
    lastName  = "Teste"
    roles     = @("gestor")
  }
)

Write-Host "📍 Step 1: Obter token admin..." -ForegroundColor Yellow

# Obter token de acesso para admin
$tokenUrl = "$KeycloakUrl/realms/master/protocol/openid-connect/token"
$tokenBody = @{
  grant_type   = "password"
  client_id    = "admin-cli"
  username     = $AdminUser
  password     = $AdminPassword
} | ConvertTo-Json

try {
  $tokenResponse = Invoke-WebRequest -Uri $tokenUrl -Method Post `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "grant_type=password&client_id=admin-cli&username=$AdminUser&password=$AdminPassword" `
    -ErrorAction Stop

  $token = ($tokenResponse.Content | ConvertFrom-Json).access_token
  Write-Host "  ✅ Token obtido" -ForegroundColor Green
} catch {
  Write-Host "  ❌ Erro ao obter token: $_" -ForegroundColor Red
  exit 1
}

# Headers com autenticação
$headers = @{
  Authorization = "Bearer $token"
  "Content-Type" = "application/json"
}

Write-Host ""
Write-Host "📍 Step 2: Criando usuários..." -ForegroundColor Yellow

foreach ($user in $users) {
  Write-Host "  Criando: $($user.username)" -NoNewline

  # Verificar se usuário já existe
  $searchUrl = "$KeycloakUrl/admin/realms/$Realm/users?username=$($user.username)"
  try {
    $existing = Invoke-WebRequest -Uri $searchUrl -Method Get -Headers $headers -ErrorAction Stop
    $existingUsers = $existing.Content | ConvertFrom-Json

    if ($existingUsers.Count -gt 0) {
      Write-Host " (existe, pulando)" -ForegroundColor Yellow
      continue
    }
  } catch {
    # Continuar mesmo com erro na busca
  }

  # Criar usuário
  $userPayload = @{
    username  = $user.username
    email     = $user.email
    firstName = $user.firstName
    lastName  = $user.lastName
    enabled   = $true
    credentials = @(
      @{
        type  = "password"
        value = $user.password
        temporary = $false
      }
    )
  } | ConvertTo-Json

  try {
    $createUrl = "$KeycloakUrl/admin/realms/$Realm/users"
    $result = Invoke-WebRequest -Uri $createUrl -Method Post `
      -Headers $headers `
      -Body $userPayload `
      -ErrorAction Stop

    Write-Host " ✅" -ForegroundColor Green

    # Atribuir roles
    if ($user.roles) {
      Write-Host "    → Atribuindo roles: $($user.roles -join ', ')" -NoNewline

      # Obter ID do usuário criado
      $searchUrl = "$KeycloakUrl/admin/realms/$Realm/users?username=$($user.username)"
      $searchResult = Invoke-WebRequest -Uri $searchUrl -Method Get -Headers $headers
      $userId = ($searchResult.Content | ConvertFrom-Json)[0].id

      # Obter role IDs
      foreach ($roleName in $user.roles) {
        $roleUrl = "$KeycloakUrl/admin/realms/$Realm/roles/$roleName"
        try {
          $roleResult = Invoke-WebRequest -Uri $roleUrl -Method Get -Headers $headers -ErrorAction Stop
          $roleId = ($roleResult.Content | ConvertFrom-Json).id

          # Atribuir role ao usuário
          $roleAssignUrl = "$KeycloakUrl/admin/realms/$Realm/users/$userId/role-mappings/realm"
          $rolePayload = @(
            @{
              id   = $roleId
              name = $roleName
            }
          ) | ConvertTo-Json

          Invoke-WebRequest -Uri $roleAssignUrl -Method Post `
            -Headers $headers `
            -Body $rolePayload `
            -ErrorAction Stop | Out-Null
        } catch {
          Write-Host ""
          Write-Host "    ⚠️  Role '$roleName' não encontrada" -ForegroundColor Yellow
        }
      }

      Write-Host " ✅" -ForegroundColor Green
    }

  } catch {
    Write-Host " ❌" -ForegroundColor Red
    Write-Host "    Erro: $_" -ForegroundColor Red
  }
}

Write-Host ""
Write-Host "✅ Conclusão!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Usuários Criados:" -ForegroundColor Cyan
foreach ($user in $users) {
  Write-Host "  • $($user.username) / $($user.password) (Roles: $($user.roles -join ', '))"
}
Write-Host ""
Write-Host "🔗 Teste:" -ForegroundColor Cyan
Write-Host "  http://localhost:3000 → Login → Usar credenciais acima"
