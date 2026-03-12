#!/usr/bin/env bash
# Build NexoERP - Linux/macOS
set -e
cd "$(dirname "$0")/.."
if [ ! -f "./mvnw" ]; then
  echo "No se encuentra mvnw. Ejecute desde la raíz del proyecto."
  exit 1
fi
./mvnw clean package -DskipTests -q
echo "Build OK: target/nexoerp-1.0.0.jar"
