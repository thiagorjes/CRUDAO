<#
.SYNOPSIS
  Roda os testes de integracao (@SpringBootTest, profile "it") contra o stack Docker final.

.DESCRIPTION
  1. Sobe o docker-compose.yml completo (postgres, keycloak, backend, frontend) com build.
  2. Aguarda postgres e keycloak ficarem healthy.
  3. Cria o banco kanban_it (isolado do banco de runtime "kanban" usado pelo container backend).
  4. Executa `mvn -P integration-tests test` num container Maven, com o datasource e o Keycloak
     apontando para os servicos do compose via host.docker.internal.

  O stack fica no ar ao final (nao derruba), para inspecao. Use -Down para derrubar depois.

.PARAMETER Down
  Em vez de rodar os testes, apenas derruba o stack (docker compose down -v).

.PARAMETER SkipBuild
  Nao rebuilda as imagens do compose (usa as existentes).
#>
param(
    [switch]$Down,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = Join-Path $scriptDir 'docker-compose.yml'
$backendDir = Join-Path $scriptDir 'backend'
$project = 'crudao'
$m2 = Join-Path $HOME '.m2'
$mavenImage = 'maven:3.9-eclipse-temurin-25'
$itDb = 'kanban_it'

function Invoke-Compose { docker compose -f $composeFile -p $project @args }

if ($Down) {
    Invoke-Compose down -v
    exit $LASTEXITCODE
}

# --- 0. Docker ativo? ---
try { docker info *> $null } catch {
    Write-Host 'Docker nao esta ativo. Iniciando Docker Desktop...' -ForegroundColor Yellow
    Start-Process "$env:LOCALAPPDATA\Programs\DockerDesktop\Docker Desktop.exe"
    $deadline = (Get-Date).AddMinutes(3)
    do { Start-Sleep 3; $ok = $?; try { docker info *> $null; $ok = $true } catch { $ok = $false } }
    while (-not $ok -and (Get-Date) -lt $deadline)
    if (-not $ok) { throw 'Docker nao subiu em 3 min.' }
}

# --- 1. Sobe o stack ---
Write-Host '==> Subindo stack (postgres, keycloak, backend, frontend)...' -ForegroundColor Cyan
if ($SkipBuild) { Invoke-Compose up -d } else { Invoke-Compose up -d --build }
if ($LASTEXITCODE -ne 0) { throw 'docker compose up falhou.' }

# --- 2. Aguarda healthchecks ---
function Wait-Healthy($svc, $timeoutSec = 180) {
    Write-Host "==> Aguardando '$svc' ficar healthy..." -ForegroundColor Cyan
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        $cid = (Invoke-Compose ps -q $svc) 2>$null
        if ($cid) {
            $state = docker inspect -f '{{.State.Health.Status}}' $cid 2>$null
            if ($state -eq 'healthy') { return }
            if ($state -eq 'unhealthy') { throw "'$svc' ficou unhealthy." }
        }
        Start-Sleep 4
    }
    throw "Timeout aguardando '$svc'."
}
Wait-Healthy 'postgres'
Wait-Healthy 'keycloak'

# --- 3. Prepara banco isolado kanban_it (criado se nao existir; schema sempre zerado) ---
Write-Host "==> Preparando banco '$itDb'..." -ForegroundColor Cyan
# CREATE DATABASE falha se ja existir — erro esperado, suprimido.
Invoke-Compose exec -T postgres psql -U kanban -d kanban -v ON_ERROR_STOP=0 -c "CREATE DATABASE $itDb" 2>&1 | Out-Null
# Zera o schema para o Flyway rodar todas as migrations do zero a cada execucao.
Invoke-Compose exec -T postgres psql -U kanban -d $itDb -v ON_ERROR_STOP=1 -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;" | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Falha ao preparar o schema de '$itDb'." }
Write-Host "    banco '$itDb' pronto (schema zerado)." -ForegroundColor Green

# --- 4. Roda os testes num container Maven ---
Write-Host '==> Executando mvn -P integration-tests test...' -ForegroundColor Cyan
docker run --rm `
    -v "${backendDir}:/app" `
    -v "${m2}:/root/.m2" `
    -w /app `
    -e IT_DB_URL="jdbc:postgresql://host.docker.internal:5432/$itDb" `
    -e IT_DB_USER=kanban `
    -e IT_DB_PASS=kanban `
    -e IT_KEYCLOAK_ISSUER_URI="http://host.docker.internal:8080/realms/kanban-dev" `
    -e IT_KEYCLOAK_INTROSPECTION_URI="http://host.docker.internal:8080/realms/kanban-dev/protocol/openid-connect/token/introspect" `
    --add-host host.docker.internal:host-gateway `
    $mavenImage `
    mvn -B -s settings.xml -P integration-tests test

$mvnExit = $LASTEXITCODE
Write-Host ''
if ($mvnExit -eq 0) {
    Write-Host 'BUILD SUCCESS — testes de integracao verdes.' -ForegroundColor Green
} else {
    Write-Host "BUILD FAILURE (exit $mvnExit) — ver output acima / target/surefire-reports." -ForegroundColor Red
}
Write-Host "Stack continua no ar. Para derrubar: .\run-integration-tests.ps1 -Down" -ForegroundColor DarkGray
exit $mvnExit
