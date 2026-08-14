#!/usr/bin/env bash
# =============================================================
# Adelgaza la VM OCI (Ubuntu) liberando RAM de servicios opcionales
# y elimina Docker si Postgres ya corre nativo (recomendado en A1.Flex).
#
# La API (env.oci / SPRING_DATASOURCE_*) sigue apuntando a
# jdbc:postgresql://localhost:5432/miapp — este script NO cambia eso.
#
# Uso (en la VM, con sudo):
#   bash tune-oci-vm.sh                 # SO liviano + quita Docker (si es seguro)
#   bash tune-oci-vm.sh --dry-run
#   bash tune-oci-vm.sh --status
#   bash tune-oci-vm.sh --keep-docker   # no toca Docker
#   bash tune-oci-vm.sh --force-remove-docker  # quita Docker aunque PG esté en contenedor
#
# NO toca: ssh, networking, Postgres nativo, ni variables de la API.
# =============================================================
set -euo pipefail

DRY_RUN=0
STATUS_ONLY=0
KEEP_DOCKER=0
FORCE_REMOVE_DOCKER=0
for arg in "$@"; do
  case "$arg" in
    --dry-run|-n)              DRY_RUN=1 ;;
    --status|-s)               STATUS_ONLY=1 ;;
    --keep-docker)             KEEP_DOCKER=1 ;;
    --force-remove-docker)     FORCE_REMOVE_DOCKER=1 ;;
    -h|--help)
      sed -n '2,18p' "$0"
      exit 0
      ;;
  esac
done

run() {
  if [[ "$DRY_RUN" -eq 1 ]]; then
    echo "  [dry-run] $*"
  else
    echo "  → $*"
    "$@"
  fi
}

run_soft() {
  if [[ "$DRY_RUN" -eq 1 ]]; then
    echo "  [dry-run] $*  (ignore errors)"
  else
    echo "  → $*  (ignore errors)"
    "$@" 2>/dev/null || true
  fi
}

mem_snapshot() {
  echo ""
  echo "── Memoria ──"
  free -h || true
  echo ""
  echo "── Top procesos (RSS) ──"
  ps -eo rss,comm --sort=-rss | awk 'NR==1{next} {printf "%6.1f MB  %s\n", $1/1024, $2}' | head -12
}

pg_native_active() {
  systemctl is-active --quiet postgresql 2>/dev/null \
    || systemctl is-active --quiet postgresql@* 2>/dev/null \
    || pgrep -x postgres >/dev/null 2>&1
}

pg_docker_container() {
  command -v docker >/dev/null 2>&1 \
    && docker ps --format '{{.Names}}' 2>/dev/null | grep -qx postgres
}

echo "============================================"
echo " tune-oci-vm — liberar RAM en OCI"
echo "============================================"

if [[ "$(id -u)" -ne 0 && "$DRY_RUN" -eq 0 && "$STATUS_ONLY" -eq 0 ]]; then
  echo "ERROR: corre con sudo (o usá --dry-run / --status)."
  exit 1
fi

mem_snapshot

NATIVE_PG=0
DOCKER_PG=0
pg_native_active && NATIVE_PG=1
pg_docker_container && DOCKER_PG=1

echo ""
echo "── Postgres (la API usa localhost:5432 / env.oci) ──"
if [[ "$NATIVE_PG" -eq 1 ]]; then
  echo "  Postgres nativo: ACTIVO (OK — no se toca)"
else
  echo "  Postgres nativo: no detectado como servicio activo"
fi
if [[ "$DOCKER_PG" -eq 1 ]]; then
  echo "  Contenedor docker 'postgres': ACTIVO"
else
  echo "  Contenedor docker 'postgres': no"
fi
echo "  Datasource API (sin cambios): jdbc:postgresql://localhost:5432/miapp"

if [[ "$STATUS_ONLY" -eq 1 ]]; then
  echo ""
  echo "── Servicios candidatos ──"
  for s in snapd.service ModemManager.service bluetooth.service \
           cups.service cups-browsed.service avahi-daemon.service \
           apport.service whoopsie.service multipathd.service \
           packagekit.service docker.service containerd.service postgresql.service; do
    state=$(systemctl is-enabled "$s" 2>/dev/null || echo "absent")
    active=$(systemctl is-active "$s" 2>/dev/null || echo "absent")
    printf "  %-28s enabled=%-10s active=%s\n" "$s" "$state" "$active"
  done
  echo ""
  echo "── Docker ──"
  if command -v docker >/dev/null 2>&1; then
    docker info 2>/dev/null | awk '/Server Version|Containers/{print "  "$0}' || echo "  docker presente"
    docker ps -a --format '  {{.Names}}\t{{.Status}}\t{{.Image}}' 2>/dev/null || true
  else
    echo "  docker no instalado / no en PATH"
  fi
  exit 0
fi

CANDIDATES=(
  ModemManager.service
  bluetooth.service
  cups.service
  cups-browsed.service
  avahi-daemon.service
  apport.service
  whoopsie.service
  multipathd.service
  packagekit.service
)

echo ""
echo "[1/5] Detener y deshabilitar servicios opcionales..."
for svc in "${CANDIDATES[@]}"; do
  if systemctl list-unit-files "$svc" 2>/dev/null | grep -q "$svc"; then
    if systemctl is-active --quiet "$svc" 2>/dev/null; then
      run systemctl stop "$svc"
    fi
    if systemctl is-enabled --quiet "$svc" 2>/dev/null; then
      run systemctl disable "$svc"
    else
      echo "  · $svc ya deshabilitado o masked"
    fi
  else
    echo "  · $svc no instalado — skip"
  fi
done

echo ""
echo "[2/5] snapd (opcional)..."
if systemctl list-unit-files snapd.service 2>/dev/null | grep -q snapd.service; then
  if [[ "${TUNE_DISABLE_SNAPD:-0}" == "1" ]]; then
    run_soft systemctl stop snapd.socket snapd.service
    run_soft systemctl disable snapd.socket snapd.service
    echo "  snapd detenido (TUNE_DISABLE_SNAPD=1)."
  else
    echo "  snapd presente. Para deshabilitarlo: sudo TUNE_DISABLE_SNAPD=1 bash $0"
  fi
else
  echo "  snapd no instalado — ok"
fi

echo ""
echo "[3/5] Limitar journald..."
JOURNAL_DROPIN="/etc/systemd/journald.conf.d/99-energiai-slim.conf"
if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "  [dry-run] escribiría $JOURNAL_DROPIN"
else
  mkdir -p /etc/systemd/journald.conf.d
  cat > "$JOURNAL_DROPIN" <<'EOF'
[Journal]
SystemMaxUse=50M
RuntimeMaxUse=30M
MaxRetentionSec=7day
EOF
  systemctl restart systemd-journald
  echo "  journald: SystemMaxUse=50M RuntimeMaxUse=30M"
fi

echo ""
echo "[4/5] Eliminar Docker (sin tocar Postgres nativo ni env de la API)..."
SKIP_DOCKER_REMOVAL=0
if [[ "$KEEP_DOCKER" -eq 1 ]]; then
  echo "  --keep-docker: se omite."
  SKIP_DOCKER_REMOVAL=1
elif [[ "$DOCKER_PG" -eq 1 && "$NATIVE_PG" -eq 0 && "$FORCE_REMOVE_DOCKER" -eq 0 ]]; then
  echo "  BLOQUEADO: la BD parece correr SOLO en el contenedor 'postgres'."
  echo "  Quitar Docker cortaría localhost:5432 y rompería la API (SPRING_DATASOURCE_*)."
  echo "  Opciones:"
  echo "    1) Migrá a Postgres nativo (mismo puerto/usuario/BD) y volvé a correr este script."
  echo "    2) Forzá igual (peligroso): sudo bash $0 --force-remove-docker"
  echo "    3) Solo adelgazar SO:     sudo bash $0 --keep-docker"
  SKIP_DOCKER_REMOVAL=1
elif [[ "$DOCKER_PG" -eq 1 && "$NATIVE_PG" -eq 1 && "$FORCE_REMOVE_DOCKER" -eq 0 ]]; then
  echo "  Hay Postgres nativo Y contenedor 'postgres'."
  echo "  Por seguridad no borro Docker (el contenedor podría ser el que usa la API)."
  echo "  Si confirmás que la API usa el servicio nativo en :5432:"
  echo "    sudo bash $0 --force-remove-docker"
  echo "  O dejá Docker: sudo bash $0 --keep-docker"
  SKIP_DOCKER_REMOVAL=1
fi

if [[ "$SKIP_DOCKER_REMOVAL" -eq 0 ]]; then
  if [[ "$FORCE_REMOVE_DOCKER" -eq 1 ]]; then
    echo "  --force-remove-docker: se procede aunque haya riesgo."
  fi
  if [[ "$NATIVE_PG" -eq 1 ]]; then
    echo "  Postgres nativo activo → se asume que la API sigue con el mismo env.oci."
  fi

  if command -v docker >/dev/null 2>&1; then
    if [[ "$DRY_RUN" -eq 1 ]]; then
      echo "  [dry-run] docker stop/rm de contenedores"
    else
      ids=$(docker ps -aq 2>/dev/null || true)
      if [[ -n "${ids}" ]]; then
        # shellcheck disable=SC2086
        docker stop ${ids} 2>/dev/null || true
        # shellcheck disable=SC2086
        docker rm ${ids} 2>/dev/null || true
      else
        echo "  · sin contenedores"
      fi
    fi
  else
    echo "  · binario docker no en PATH (igual se intenta purge)"
  fi

  run_soft systemctl stop docker.socket docker.service containerd.service
  run_soft systemctl disable docker.socket docker.service containerd.service

  PKGS=()
  for p in docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin \
           docker.io docker-compose docker-compose-v2 docker-doc podman-docker; do
    if dpkg -l "$p" 2>/dev/null | grep -q '^ii'; then
      PKGS+=("$p")
    fi
  done

  if [[ ${#PKGS[@]} -gt 0 ]]; then
    echo "  Paquetes a purgar: ${PKGS[*]}"
    if [[ "$DRY_RUN" -eq 1 ]]; then
      echo "  [dry-run] apt-get purge -y ${PKGS[*]}"
    else
      export DEBIAN_FRONTEND=noninteractive
      apt-get purge -y "${PKGS[@]}"
      apt-get autoremove -y --purge
    fi
  else
    echo "  · no hay paquetes docker conocidos vía dpkg"
  fi

  if [[ "$DRY_RUN" -eq 1 ]]; then
    echo "  [dry-run] opcional TUNE_PURGE_DOCKER_LIB=1 → rm -rf /var/lib/docker"
  else
    if [[ "${TUNE_PURGE_DOCKER_LIB:-0}" == "1" ]]; then
      rm -rf /var/lib/docker /var/lib/containerd
      echo "  /var/lib/docker borrado (TUNE_PURGE_DOCKER_LIB=1)."
    else
      echo "  /var/lib/docker conservado. Para borrarlo: TUNE_PURGE_DOCKER_LIB=1"
    fi
  fi
  echo "  Docker eliminado. Postgres nativo y SPRING_DATASOURCE_* se mantienen."
fi

echo ""
echo "[5/5] Verificación / recordatorio API..."
cat <<'EOF'
  La API NO requiere cambios de entorno si Postgres sigue en localhost:5432:

    SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/miapp
    SPRING_DATASOURCE_USERNAME=…   # el que ya tenés en env.oci / .profile
    SPRING_DATASOURCE_PASSWORD=…

  Arranque (sin Docker):

    source ~/Documents/energiai-oci-snapshot/env.oci   # o tu ruta real
    ./run-oci.sh

  Comprobá la BD:

    sudo -u postgres psql -c '\l'          # o el usuario de tu instalación
    # o:  psql "postgresql://USER@localhost:5432/miapp" -c 'select 1'
EOF

if [[ "$NATIVE_PG" -eq 1 ]]; then
  echo "  Estado: Postgres nativo ya estaba activo — no hace falta reinstalarlo."
fi

echo ""
echo "============================================"
echo " Listo."
if [[ "$DRY_RUN" -eq 1 ]]; then
  echo " (dry-run: no se aplicó nada)"
fi
mem_snapshot
echo "============================================"
