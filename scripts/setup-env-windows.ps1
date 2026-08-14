#Requires -Version 5.1
<#
.SYNOPSIS
  Aplica variables EnergiAI en Windows.

.DESCRIPTION
  Modo automatico (preferido):
    Si existe setup-env-windows.generated.ps1 (generado desde ~/.profile en Linux),
    lo ejecuta sin preguntar nada.

  Modo manual:
    .\setup-env-windows.ps1 -Interactive

  Generar el .generated.ps1 en Linux:
    ./scripts/generate-windows-env-from-profile.sh
#>

[CmdletBinding()]
param(
    [switch]$Interactive,
    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$generated = Join-Path $here "setup-env-windows.generated.ps1"

function Read-Value {
    param(
        [string]$Name,
        [string]$Default = "",
        [string]$Help = "",
        [switch]$Secret
    )
    if ($Help) { Write-Host "  ($Help)" -ForegroundColor DarkGray }
    $suffix = if ($Default -ne "") { " [$Default]" } else { "" }
    if ($Secret) {
        $secure = Read-Host -Prompt "$Name$suffix" -AsSecureString
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try {
            $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
        if ([string]::IsNullOrWhiteSpace($plain)) { return $Default }
        return $plain
    }
    $raw = Read-Host -Prompt "$Name$suffix"
    if ([string]::IsNullOrWhiteSpace($raw)) { return $Default }
    return $raw.Trim()
}

function Set-UserEnv {
    param([string]$Name, [string]$Value)
    if ($WhatIf) {
        $preview = if ($Name -match '(PASSWORD|SECRET|KEY|TOKEN)') { '(oculto)' } else { $Value }
        Write-Host "  [WhatIf] $Name = $preview" -ForegroundColor Yellow
        return
    }
    [Environment]::SetEnvironmentVariable($Name, $Value, "User")
    Set-Item -Path "Env:$Name" -Value $Value
}

# --- Automatico desde profile exportado ---
if (-not $Interactive -and (Test-Path -LiteralPath $generated)) {
    Write-Host "Encontre setup-env-windows.generated.ps1 — modo automatico." -ForegroundColor Cyan
    if ($WhatIf) {
        Write-Host "WhatIf: se ejecutaria $generated" -ForegroundColor Yellow
        Get-Content -LiteralPath $generated | Select-String '^Set-UserEnv' | ForEach-Object {
            if ($_ -match 'Set-UserEnv "([^"]+)"') { Write-Host "  - $($Matches[1])" }
        }
        exit 0
    }
    & $generated
    exit $LASTEXITCODE
}

if (-not $Interactive) {
    Write-Host "No esta setup-env-windows.generated.ps1" -ForegroundColor Yellow
    Write-Host "En Linux genera con:  ./scripts/generate-windows-env-from-profile.sh"
    Write-Host "Copia el .generated.ps1 junto a este script y vuelve a ejecutar."
    Write-Host ""
    Write-Host "O corre modo manual:  .\setup-env-windows.ps1 -Interactive"
    exit 1
}

# --- Fallback interactivo ---
Write-Host ""
Write-Host "=== EnergiAI — setup manual (Interactive) ===" -ForegroundColor Cyan

$dbHost = Read-Value "ENERGIAI_DB_HOST" "localhost" "Host PostgreSQL"
$dbPort = Read-Value "ENERGIAI_DB_PORT" "5432"
$dbName = Read-Value "ENERGIAI_DB_NAME" "miapp"
$dbUser = Read-Value "SPRING_DATASOURCE_USERNAME" "oracleone18"
$dbPass = Read-Value "SPRING_DATASOURCE_PASSWORD" "" "Password BD" -Secret
$profileActive = Read-Value "SPRING_PROFILES_ACTIVE" "dev"
$jwtSecret = Read-Value "JWT_SECRET" "" "Min 32 chars" -Secret
$jwtExp = Read-Value "JWT_EXPIRATION_MS" "86400000"
$cors = Read-Value "APP_CORS_ALLOWED_ORIGINS" "http://localhost:5173,http://localhost:3000"
$oauthEnabled = Read-Value "APP_OAUTH2_ENABLED" "false"
$oauthRedirect = Read-Value "APP_OAUTH2_SUCCESS_REDIRECT" "http://localhost:8080/oauth-callback.html"
$googleId = Read-Value "GOOGLE_CLIENT_ID" ""
$googleSecret = Read-Value "GOOGLE_CLIENT_SECRET" "" -Secret
$recModo = Read-Value "APP_RECOMENDACIONES_MODO" "hibrido"
$geminiKey = Read-Value "GEMINI_API_KEY" "" -Secret

$jdbc = "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}"

Set-UserEnv "ENERGIAI_DB_HOST" $dbHost
Set-UserEnv "ENERGIAI_DB_PORT" $dbPort
Set-UserEnv "ENERGIAI_DB_NAME" $dbName
Set-UserEnv "SPRING_PROFILES_ACTIVE" $profileActive
Set-UserEnv "SPRING_DATASOURCE_URL" $jdbc
Set-UserEnv "SPRING_DATASOURCE_USERNAME" $dbUser
if ($dbPass) { Set-UserEnv "SPRING_DATASOURCE_PASSWORD" $dbPass }
if ($jwtSecret) { Set-UserEnv "JWT_SECRET" $jwtSecret }
Set-UserEnv "JWT_EXPIRATION_MS" $jwtExp
Set-UserEnv "APP_CORS_ALLOWED_ORIGINS" $cors
Set-UserEnv "APP_OAUTH2_ENABLED" $oauthEnabled
Set-UserEnv "APP_OAUTH2_SUCCESS_REDIRECT" $oauthRedirect
if ($googleId) { Set-UserEnv "GOOGLE_CLIENT_ID" $googleId }
if ($googleSecret) { Set-UserEnv "GOOGLE_CLIENT_SECRET" $googleSecret }
Set-UserEnv "APP_RECOMENDACIONES_MODO" $recModo
Set-UserEnv "APP_RECOMENDACIONES_MAX_ITEMS" "3"
Set-UserEnv "APP_RECOMENDACIONES_GEMINI_MODELO" "gemini-2.5-flash-lite"
Set-UserEnv "APP_RECOMENDACIONES_GEMINI_TIMEOUT_MS" "8000"
if ($geminiKey) {
    Set-UserEnv "GEMINI_API_KEY" $geminiKey
    Set-UserEnv "GOOGLE_GENERATIVE_AI_API_KEY" $geminiKey
}
Set-UserEnv "APP_MODELO_ESTRATEGIA" "onnx"

Write-Host "OK. Reabre la terminal/IDE." -ForegroundColor Green
