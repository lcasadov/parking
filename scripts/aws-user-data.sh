#!/bin/bash
# =====================================================================
# aws-user-data.sh — Provisiona un host Amazon Linux 2023 para correr la
# pila `parking` con docker compose (Etapa B: AWS EC2, HTTP).
#
# Uso: pegar este contenido en el campo "User data" al lanzar la EC2
# (Amazon Linux 2023), o pasarlo con:
#   aws ec2 run-instances ... --user-data file://scripts/aws-user-data.sh
#
# Qué hace:
#   - Instala Docker + el plugin `docker compose` v2.
#   - Habilita y arranca el servicio Docker.
#   - Añade el usuario `ec2-user` al grupo docker (usar `docker` sin sudo).
#   - Instala git (para clonar el repo).
# NO despliega la app: eso se hace por SSH (ver docs/deploy-aws.md §6),
# porque requiere crear el fichero `.env` con secretos que no van en user-data.
# =====================================================================
set -euxo pipefail

# 1) Actualizar paquetes e instalar Docker + git.
dnf update -y
dnf install -y docker git

# 2) Habilitar y arrancar Docker.
systemctl enable --now docker

# 3) Permitir a ec2-user usar docker sin sudo (efectivo tras reconectar SSH).
usermod -aG docker ec2-user

# 4) Instalar el plugin `docker compose` v2 (subcomando `docker compose`).
#    Amazon Linux 2023 no lo trae en el paquete `docker`; se instala como
#    CLI plugin en el directorio de plugins de Docker.
DOCKER_CLI_PLUGINS=/usr/libexec/docker/cli-plugins
mkdir -p "${DOCKER_CLI_PLUGINS}"
COMPOSE_VERSION="v2.29.7"
ARCH="$(uname -m)"   # x86_64 o aarch64
curl -fsSL \
  "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-${ARCH}" \
  -o "${DOCKER_CLI_PLUGINS}/docker-compose"
chmod +x "${DOCKER_CLI_PLUGINS}/docker-compose"

# 5) Verificación (queda en /var/log/cloud-init-output.log).
docker --version
docker compose version
