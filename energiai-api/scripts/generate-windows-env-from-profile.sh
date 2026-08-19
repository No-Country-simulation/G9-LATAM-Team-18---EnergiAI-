#!/usr/bin/env bash
# Genera un script PowerShell automatico para Windows con los valores del ~/.profile.
#
# Uso (Linux, en energiai-api):
#   ./scripts/generate-windows-env-from-profile.sh
#
# Salida (NO versionar; tiene secretos):
#   scripts/setup-env-windows.generated.ps1
#
# En Windows: doble clic en scripts/setup-env-windows.cmd
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROFILE="${ENERGIAI_PROFILE_FILE:-$HOME/.profile}"
OUT="$ROOT/scripts/setup-env-windows.generated.ps1"
WRITER="$ROOT/scripts/_write_windows_env_ps1.py"

if [[ ! -f "$PROFILE" ]]; then
  echo "No existe $PROFILE" >&2
  exit 1
fi

# shellcheck disable=SC1090
set -a
# shellcheck source=/dev/null
source "$PROFILE"
set +a

: "${SPRING_PROFILES_ACTIVE:=dev}"
: "${ENERGIAI_DB_HOST:=localhost}"
: "${ENERGIAI_DB_PORT:=5432}"
: "${ENERGIAI_DB_NAME:=miapp}"
: "${SPRING_DATASOURCE_USERNAME:=oracleone18}"
: "${JWT_EXPIRATION_MS:=86400000}"
: "${APP_CORS_ALLOWED_ORIGINS:=http://localhost:5173,http://localhost:3000,http://localhost:8080}"
: "${APP_OAUTH2_ENABLED:=false}"
: "${APP_OAUTH2_SUCCESS_REDIRECT:=http://localhost:8080/oauth-callback.html}"
: "${APP_RECOMENDACIONES_MODO:=hibrido}"
: "${APP_RECOMENDACIONES_MAX_ITEMS:=3}"
: "${APP_RECOMENDACIONES_GEMINI_MODELO:=gemini-2.5-flash-lite}"
: "${APP_RECOMENDACIONES_GEMINI_TIMEOUT_MS:=8000}"
: "${APP_MODELO_ESTRATEGIA:=onnx}"

if [[ -z "${SPRING_DATASOURCE_URL:-}" ]]; then
  SPRING_DATASOURCE_URL="jdbc:postgresql://${ENERGIAI_DB_HOST}:${ENERGIAI_DB_PORT}/${ENERGIAI_DB_NAME}"
fi

if [[ -z "${GEMINI_API_KEY:-}" && -n "${GOOGLE_GENERATIVE_AI_API_KEY:-}" ]]; then
  GEMINI_API_KEY="$GOOGLE_GENERATIVE_AI_API_KEY"
fi
if [[ -n "${GEMINI_API_KEY:-}" && -z "${GOOGLE_GENERATIVE_AI_API_KEY:-}" ]]; then
  GOOGLE_GENERATIVE_AI_API_KEY="$GEMINI_API_KEY"
fi

missing=()
[[ -z "${SPRING_DATASOURCE_PASSWORD:-}" ]] && missing+=("SPRING_DATASOURCE_PASSWORD")
[[ -z "${JWT_SECRET:-}" ]] && missing+=("JWT_SECRET")
if ((${#missing[@]})); then
  echo "Faltan en $PROFILE: ${missing[*]}" >&2
  exit 1
fi

export OUT
export ENERGIAI_DB_HOST ENERGIAI_DB_PORT ENERGIAI_DB_NAME
export POSTGRES_DB="${POSTGRES_DB:-$ENERGIAI_DB_NAME}"
export POSTGRES_USER="${POSTGRES_USER:-$SPRING_DATASOURCE_USERNAME}"
export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-$SPRING_DATASOURCE_PASSWORD}"
export SPRING_PROFILES_ACTIVE SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD
export JWT_SECRET JWT_EXPIRATION_MS APP_CORS_ALLOWED_ORIGINS
export APP_OAUTH2_ENABLED APP_OAUTH2_SUCCESS_REDIRECT
export GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-}"
export GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-}"
export FACEBOOK_CLIENT_ID="${FACEBOOK_CLIENT_ID:-}"
export FACEBOOK_CLIENT_SECRET="${FACEBOOK_CLIENT_SECRET:-}"
export APP_RECOMENDACIONES_MODO APP_RECOMENDACIONES_MAX_ITEMS
export APP_RECOMENDACIONES_GEMINI_MODELO APP_RECOMENDACIONES_GEMINI_TIMEOUT_MS
export GEMINI_API_KEY="${GEMINI_API_KEY:-}"
export GOOGLE_GENERATIVE_AI_API_KEY="${GOOGLE_GENERATIVE_AI_API_KEY:-}"
export APP_MODELO_ESTRATEGIA

exec python3 "$WRITER"
