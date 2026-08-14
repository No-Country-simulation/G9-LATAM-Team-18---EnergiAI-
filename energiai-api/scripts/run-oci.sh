#!/usr/bin/env bash
# =============================================================
# Arranca energiai-api en la VM OCI (perfil prod, BD localhost).
#
# Uso:
#   cd ~/energiai-oci-snapshot   # o donde hayas copiado el paquete
#   cp env.oci.example env.oci   # primera vez
#   nano env.oci                 # completar secretos
#   ./run-oci.sh
#
# Variables: se cargan desde ./env.oci (mismo directorio que este script).
# =============================================================
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

ENV_FILE="${ENERGIAI_ENV_FILE:-$DIR/env.oci}"
JAR="${ENERGIAI_JAR:-$DIR/energiai-api-0.0.1-SNAPSHOT.jar}"

if [[ ! -f "$JAR" ]]; then
  echo "ERROR: no esta el jar: $JAR" >&2
  echo "Copiá energiai-api-0.0.1-SNAPSHOT.jar junto a este script." >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: falta $ENV_FILE" >&2
  echo "  cp env.oci.example env.oci && nano env.oci" >&2
  exit 1
fi

# shellcheck disable=SC1090
set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

# Defaults seguros para la VM (localhost) si no vienen en env.oci
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export SERVER_PORT="${SERVER_PORT:-8080}"
export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/miapp}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-oracleone18}"
export JWT_EXPIRATION_MS="${JWT_EXPIRATION_MS:-86400000}"
export APP_MODELO_ESTRATEGIA="${APP_MODELO_ESTRATEGIA:-onnx}"
export APP_RECOMENDACIONES_MODO="${APP_RECOMENDACIONES_MODO:-hibrido}"
export APP_RECOMENDACIONES_MAX_ITEMS="${APP_RECOMENDACIONES_MAX_ITEMS:-3}"
export APP_RECOMENDACIONES_GEMINI_MODELO="${APP_RECOMENDACIONES_GEMINI_MODELO:-gemini-2.5-flash-lite}"
export APP_RECOMENDACIONES_GEMINI_TIMEOUT_MS="${APP_RECOMENDACIONES_GEMINI_TIMEOUT_MS:-8000}"
export APP_OAUTH2_ENABLED="${APP_OAUTH2_ENABLED:-false}"
export APP_OAUTH2_SUCCESS_REDIRECT="${APP_OAUTH2_SUCCESS_REDIRECT:-http://localhost:8080/oauth-callback.html}"
export APP_CORS_ALLOWED_ORIGINS="${APP_CORS_ALLOWED_ORIGINS:-http://localhost:5173,http://localhost:3000,http://localhost:8080}"

if [[ -z "${SPRING_DATASOURCE_PASSWORD:-}" || "${SPRING_DATASOURCE_PASSWORD}" == "cambia_esta_password" ]]; then
  echo "ERROR: definí SPRING_DATASOURCE_PASSWORD real en $ENV_FILE" >&2
  exit 1
fi

if [[ -z "${JWT_SECRET:-}" || "${#JWT_SECRET}" -lt 32 ]]; then
  echo "ERROR: JWT_SECRET debe tener al menos 32 caracteres en $ENV_FILE" >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: java no esta en PATH. Instalá openjdk-21-jre-headless." >&2
  exit 1
fi

JAVA_VER="$(java -version 2>&1 | head -1 || true)"
echo "==> Java: $JAVA_VER"
echo "==> Perfil: $SPRING_PROFILES_ACTIVE"
echo "==> Datasource: $SPRING_DATASOURCE_URL (user=$SPRING_DATASOURCE_USERNAME)"
echo "==> Puerto: $SERVER_PORT"
echo "==> OAuth2 browser: $APP_OAUTH2_ENABLED"
echo "==> Recomendaciones: modo=$APP_RECOMENDACIONES_MODO modelo=$APP_RECOMENDACIONES_GEMINI_MODELO gemini_key=${GEMINI_API_KEY:+si}"
echo "==> JAVA_OPTS: ${JAVA_OPTS:-(default JVM)}"
echo "==> Jar: $JAR"
echo "==> Arrancando..."

# JAVA_OPTS tipico en VM chica (lo setea deploy-oci.sh en env.oci):
#   -Xms64m -Xmx200m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} -jar "$JAR" \
  --spring.profiles.active="$SPRING_PROFILES_ACTIVE" \
  --server.port="$SERVER_PORT"
