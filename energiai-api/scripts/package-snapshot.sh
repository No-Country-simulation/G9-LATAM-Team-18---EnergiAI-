#!/usr/bin/env bash
# =============================================================
# Empaqueta un snapshot listo para copiar a la VM OCI.
# Uso (en tu laptop, desde energiai-api/):
#   ./scripts/package-snapshot.sh
# Genera: dist/energiai-oci-snapshot/  (jar + scripts + env de ejemplo)
# =============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

JAVA_HOME="${JAVA_HOME:-/usr/lib64/jvm/java-21-openjdk-21}"
if [[ -x "$JAVA_HOME/bin/java" ]]; then
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "==> Compilando energiai-api-0.0.1-SNAPSHOT.jar (skipTests)..."
mvn -B -q clean package -DskipTests

JAR="$(ls -1 target/energiai-api-*-SNAPSHOT.jar | head -1)"
if [[ ! -f "$JAR" ]]; then
  echo "ERROR: no se encontro el jar snapshot en target/" >&2
  exit 1
fi

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT="$ROOT/dist/energiai-oci-snapshot"
rm -rf "$OUT"
mkdir -p "$OUT"

cp -f "$JAR" "$OUT/energiai-api-0.0.1-SNAPSHOT.jar"
cp -f scripts/run-oci.sh "$OUT/run-oci.sh"
cp -f scripts/env.oci.example "$OUT/env.oci.example"
chmod +x "$OUT/run-oci.sh"

cat > "$OUT/README-OCI.txt" <<EOF
EnergiAI API — snapshot OCI ($STAMP)
====================================

Contenido
---------
  energiai-api-0.0.1-SNAPSHOT.jar   aplicacion Spring Boot (Java 21)
  run-oci.sh                        arranque con perfil prod + env
  env.oci.example                   plantilla de variables (copiar a env.oci)

En la VM (Ubuntu A1.Flex / localhost)
------------------------------------
1. Requisitos: Java 21 JRE (openjdk-21-jre-headless) y PostgreSQL en :5432
   (ver setup_vm.sh del monorepo).

2. Copiar esta carpeta a la VM, por ejemplo:
     scp -r energiai-oci-snapshot opc@<IP_PUBLICA>:~/

3. Configurar secretos (NO versionar env.oci):
     cd ~/energiai-oci-snapshot
     cp env.oci.example env.oci
     nano env.oci   # completar password BD, JWT, OAuth si aplica

4. Ejecutar:
     ./run-oci.sh

5. Probar en la VM:
     curl -s http://localhost:8080/swagger-ui.html | head
     # o tunnel: ssh -L 8080:localhost:8080 opc@<IP>

Notas
-----
- Perfil Spring: prod (BD jdbc:postgresql://localhost:5432/miapp).
- El jar incluye nativos ONNX linux-x64 y linux-aarch64 (A1.Flex OK).
- OAuth Google: si usas login por browser via IP publica, agrega en Google
  Console el redirect:
    http://<IP_PUBLICA>:8080/login/oauth2/code/google
  y ajusta APP_OAUTH2_SUCCESS_REDIRECT en env.oci.
  Para prueba solo en la VM / tunnel SSH, localhost alcanza.
EOF

echo "==> Listo: $OUT"
ls -lh "$OUT"
echo
echo "Copiar a la VM, por ejemplo:"
echo "  scp -r \"$OUT\" opc@<IP_PUBLICA>:~/"
