#!/usr/bin/env bash
# =============================================================
# Deploy EnergiAI snapshot → VM OCI
#
# Desde tu laptop (energiai-api/):
#   ./scripts/deploy-oci.sh
#
# - Empaqueta el jar si hace falta
# - Copia a ~/Documents/energiai-oci-snapshot en la VM
# - Genera env.oci + load-env.sh con valores de TU ~/.profile
#   (OAuth Google ON, BD localhost en la VM, JWT, etc.)
# - NO versiona secretos: los lee en runtime desde ~/.profile
#
# Overrides opcionales:
#   ENERGIAI_SSH_HOST   (default: 146.181.33.44)
#   ENERGIAI_SSH_USER   (default: ubuntu)
#   ENERGIAI_SSH_KEY    (default: ../ssh-key-2026-07-22.key)
#   ENERGIAI_HEAP_MB    (default: 200)
# =============================================================
set -euo pipefail

ok()  { echo "  [OK] $*"; }
step(){ echo; echo "==> $*"; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MONOREPO="$(cd "$ROOT/.." && pwd)"
cd "$ROOT"

SSH_HOST="${ENERGIAI_SSH_HOST:-146.181.33.44}"
SSH_USER="${ENERGIAI_SSH_USER:-ubuntu}"
SSH_KEY="${ENERGIAI_SSH_KEY:-$MONOREPO/ssh-key-2026-07-22.key}"
REMOTE_DIR='Documents/energiai-oci-snapshot'
HEAP_MB="${ENERGIAI_HEAP_MB:-200}"
PROFILE_FILE="${ENERGIAI_PROFILE_FILE:-$HOME/.profile}"

step "1/7 Validando herramientas locales"
command -v scp >/dev/null
command -v ssh >/dev/null
command -v python3 >/dev/null
ok "ssh/scp/python3 disponibles"

if [[ ! -f "$SSH_KEY" ]]; then
  echo "ERROR: no encuentro la clave SSH: $SSH_KEY" >&2
  exit 1
fi
chmod 600 "$SSH_KEY" 2>/dev/null || true
ok "Clave SSH: $SSH_KEY"

SSH=(ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new -o IdentitiesOnly=yes)
SCP=(scp -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new -o IdentitiesOnly=yes)
REMOTE="${SSH_USER}@${SSH_HOST}"

step "2/7 Cargando variables desde $PROFILE_FILE"
if [[ ! -f "$PROFILE_FILE" ]]; then
  echo "ERROR: no existe $PROFILE_FILE" >&2
  exit 1
fi
# shellcheck disable=SC1090
set -a
# shellcheck source=/dev/null
source "$PROFILE_FILE"
set +a

: "${SPRING_DATASOURCE_USERNAME:?Falta SPRING_DATASOURCE_USERNAME en .profile}"
: "${SPRING_DATASOURCE_PASSWORD:?Falta SPRING_DATASOURCE_PASSWORD en .profile}"
: "${JWT_SECRET:?Falta JWT_SECRET en .profile}"
: "${GOOGLE_CLIENT_ID:?Falta GOOGLE_CLIENT_ID en .profile}"
: "${GOOGLE_CLIENT_SECRET:?Falta GOOGLE_CLIENT_SECRET en .profile}"

if [[ "${#JWT_SECRET}" -lt 32 ]]; then
  echo "ERROR: JWT_SECRET tiene menos de 32 caracteres" >&2
  exit 1
fi
ok "Usuario BD: $SPRING_DATASOURCE_USERNAME"
ok "JWT_SECRET: presente (${#JWT_SECRET} chars)"
ok "GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID:0:24}..."
ok "APP_OAUTH2_ENABLED (profile): ${APP_OAUTH2_ENABLED:-unset} → en VM se forzará true"
ok "Gemini: modelo=${APP_RECOMENDACIONES_GEMINI_MODELO:-gemini-2.5-flash-lite} key=${GEMINI_API_KEY:+presente}"

step "3/7 Asegurando paquete snapshot local"
if [[ ! -f dist/energiai-oci-snapshot/energiai-api-0.0.1-SNAPSHOT.jar ]]; then
  echo "  (no hay dist/; compilando con package-snapshot.sh...)"
  ./scripts/package-snapshot.sh
else
  ok "Paquete ya existe en dist/energiai-oci-snapshot/"
fi
PKG="$ROOT/dist/energiai-oci-snapshot"
[[ -f "$PKG/energiai-api-0.0.1-SNAPSHOT.jar" ]]
[[ -f "$PKG/run-oci.sh" ]]
ok "Jar + run-oci.sh listos"

step "4/7 Generando env.oci (secretos desde .profile; BD=localhost en la VM)"
TMP_ENV="$(mktemp)"
TMP_LOAD="$(mktemp)"
cleanup() { rm -f "$TMP_ENV" "$TMP_LOAD"; }
trap cleanup EXIT

# Escapa valores para un archivo source-able (comillas simples seguras).
export _E_USER="$SPRING_DATASOURCE_USERNAME"
export _E_PASS="$SPRING_DATASOURCE_PASSWORD"
export _E_JWT="$JWT_SECRET"
export _E_GID="$GOOGLE_CLIENT_ID"
export _E_GSEC="$GOOGLE_CLIENT_SECRET"
export _E_HOST="$SSH_HOST"
export _E_HEAP="$HEAP_MB"
export _E_GEMINI="${GEMINI_API_KEY:-}"
export _E_REC_MODO="${APP_RECOMENDACIONES_MODO:-hibrido}"
export _E_REC_MAX="${APP_RECOMENDACIONES_MAX_ITEMS:-3}"
export _E_REC_MODEL="${APP_RECOMENDACIONES_GEMINI_MODELO:-gemini-2.5-flash-lite}"
export _E_REC_TO="${APP_RECOMENDACIONES_GEMINI_TIMEOUT_MS:-8000}"

python3 - <<'PY' >"$TMP_ENV"
import os

def q(v: str) -> str:
    return "'" + v.replace("'", "'\"'\"'") + "'"

host = os.environ["_E_HOST"]
lines = [
    "# Generado por scripts/deploy-oci.sh — NO versionar",
    "# BD en la VM = localhost (prod). OAuth Google activado.",
    "",
    "SPRING_PROFILES_ACTIVE=prod",
    "SERVER_PORT=8080",
    "",
    "SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/miapp'",
    f"SPRING_DATASOURCE_USERNAME={q(os.environ['_E_USER'])}",
    f"SPRING_DATASOURCE_PASSWORD={q(os.environ['_E_PASS'])}",
    "",
    f"JWT_SECRET={q(os.environ['_E_JWT'])}",
    "JWT_EXPIRATION_MS=86400000",
    "",
    "APP_MODELO_ESTRATEGIA=onnx",
    "",
    f"APP_RECOMENDACIONES_MODO={q(os.environ.get('_E_REC_MODO', 'hibrido'))}",
    f"APP_RECOMENDACIONES_MAX_ITEMS={q(os.environ.get('_E_REC_MAX', '3'))}",
    f"APP_RECOMENDACIONES_GEMINI_MODELO={q(os.environ.get('_E_REC_MODEL', 'gemini-2.5-flash-lite'))}",
    f"APP_RECOMENDACIONES_GEMINI_TIMEOUT_MS={q(os.environ.get('_E_REC_TO', '8000'))}",
    f"GEMINI_API_KEY={q(os.environ.get('_E_GEMINI', ''))}",
    "",
    f"APP_CORS_ALLOWED_ORIGINS='https://energi-ai.netlify.app,http://localhost:5173,http://localhost:3000,http://localhost:8080,http://{host}:8080'",
    "",
    "APP_OAUTH2_ENABLED=true",
    f"GOOGLE_CLIENT_ID={q(os.environ['_E_GID'])}",
    f"GOOGLE_CLIENT_SECRET={q(os.environ['_E_GSEC'])}",
    # Browser vía IP publica de la VM (Google redirect + callback deben coincidir)
    f"APP_OAUTH2_SUCCESS_REDIRECT='http://{host}:8080/oauth-callback.html'",
    f"# Dev/laptop (tunnel): APP_OAUTH2_SUCCESS_REDIRECT='http://localhost:8080/oauth-callback.html'",
    "",
    f"JAVA_OPTS='-Xms64m -Xmx{os.environ['_E_HEAP']}m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC'",
]
print("\n".join(lines))
PY

cat >"$TMP_LOAD" <<'EOF'
#!/usr/bin/env bash
# Carga variables de EnergiAI en la shell actual.
# Uso:  source ./load-env.sh
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
set -a
source "$DIR/env.oci"
set +a
echo "[OK] Entorno EnergiAI cargado desde $DIR/env.oci (perfil=$SPRING_PROFILES_ACTIVE, oauth=$APP_OAUTH2_ENABLED)"
EOF

ok "env.oci generado en temporal (no queda en el repo)"
ok "load-env.sh generado"

step "5/7 Creando directorio remoto ~/${REMOTE_DIR}"
"${SSH[@]}" "$REMOTE" "mkdir -p \"\$HOME/${REMOTE_DIR}\" && echo OK_DIR"
ok "Directorio remoto listo: ~/${REMOTE_DIR}"

step "6/7 Copiando jar, scripts y entorno a la VM"
"${SCP[@]}" -r \
  "$PKG/energiai-api-0.0.1-SNAPSHOT.jar" \
  "$PKG/run-oci.sh" \
  "$PKG/env.oci.example" \
  "$PKG/README-OCI.txt" \
  "$REMOTE:\$HOME/${REMOTE_DIR}/"
ok "Artefactos del paquete copiados"

"${SCP[@]}" "$TMP_ENV" "$REMOTE:\$HOME/${REMOTE_DIR}/env.oci"
"${SCP[@]}" "$TMP_LOAD" "$REMOTE:\$HOME/${REMOTE_DIR}/load-env.sh"
ok "env.oci + load-env.sh copiados"

"${SSH[@]}" "$REMOTE" "chmod +x \"\$HOME/${REMOTE_DIR}/run-oci.sh\" \"\$HOME/${REMOTE_DIR}/load-env.sh\" && chmod 600 \"\$HOME/${REMOTE_DIR}/env.oci\" && ls -lh \"\$HOME/${REMOTE_DIR}\""
ok "Permisos aplicados (env.oci → 600)"

step "7/7 Verificando Java en la VM y source remoto de prueba"
"${SSH[@]}" "$REMOTE" "java -version 2>&1 | head -1"
ok "Java detectado en la VM"

"${SSH[@]}" "$REMOTE" "bash -lc 'cd \"\$HOME/${REMOTE_DIR}\" && source ./load-env.sh && test \"\$APP_OAUTH2_ENABLED\" = true && test -n \"\$GOOGLE_CLIENT_ID\" && test -n \"\$JWT_SECRET\" && echo OAUTH_JWT_OK'"
ok "source ./load-env.sh OK (OAuth + JWT presentes en la VM)"

echo
echo "============================================================"
echo " DEPLOY OCI COMPLETADO CON ÉXITO"
echo "============================================================"
echo " Destino: ${REMOTE}:~/${REMOTE_DIR}"
echo
echo " En la VM, cargá el entorno y arrancá el jar así:"
echo
echo "   cd ~/${REMOTE_DIR}"
echo "   source ./load-env.sh"
echo "   java -Xms64m -Xmx${HEAP_MB}m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC \\"
echo "     -jar energiai-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod"
echo
echo " Equivalente (usa JAVA_OPTS de env.oci):"
echo "   cd ~/${REMOTE_DIR} && ./run-oci.sh"
echo
echo " Nota heap: ${HEAP_MB}m pedidos. Si ves OutOfMemoryError con ONNX/Spring,"
echo " subí a 256m:  ENERGIAI_HEAP_MB=256 ./scripts/deploy-oci.sh"
echo " o editá JAVA_OPTS en ~/${REMOTE_DIR}/env.oci"
echo
echo " OAuth Google (deploy / IP publica):"
echo "   Login:    http://${SSH_HOST}:8080/oauth-login.html"
echo "   Callback: http://${SSH_HOST}:8080/oauth-callback.html"
echo "   En Google Cloud → Authorized redirect URIs:"
echo "     http://${SSH_HOST}:8080/login/oauth2/code/google"
echo
echo " Dev (laptop / .profile): JDBC remoto ${SSH_HOST}, OAuth callback localhost."
echo " Deploy (env.oci en VM):   JDBC localhost, OAuth callback IP publica."
echo "============================================================"
