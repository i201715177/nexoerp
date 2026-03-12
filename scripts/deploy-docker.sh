#!/usr/bin/env bash
# Build imagen Docker y (opcional) levantar con docker-compose
set -e
cd "$(dirname "$0")/.."
echo "Building Docker image..."
docker build -t nexoerp:latest .
echo "Image nexoerp:latest built."
if [ "$1" = "up" ]; then
  echo "Starting stack..."
  docker compose up -d
  echo "Stack up. App: http://localhost:8080 (o 80 si usas nginx)"
fi
