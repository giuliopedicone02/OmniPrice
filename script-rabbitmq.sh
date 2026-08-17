#!/bin/bash
set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Pulizia all'uscita (Ctrl+C)
cleanup() {
  echo ""
  echo "Arresto in corso di Frontend e Backend..."
  trap - SIGINT SIGTERM # evita loop di trap
  kill 0 2>/dev/null || true
}
trap cleanup SIGINT SIGTERM EXIT

echo "=============================================="
echo "   OmniPrice - Avvio Completo con RabbitMQ    "
echo "=============================================="
echo ""

# 1. Avvio RabbitMQ tramite Docker Compose
echo "[RabbitMQ] Avvio container RabbitMQ..."
docker compose up -d

echo "[RabbitMQ] Attendo che RabbitMQ sia pronto..."
for i in $(seq 1 30); do
  if docker compose exec -T rabbitmq rabbitmq-diagnostics -q ping 2>/dev/null; then
    echo "[RabbitMQ] Pronto!"
    break
  fi
  sleep 1
done

# 2. Avvio Backend Spring Boot con RabbitMQ abilitato
echo ""
echo "[Backend] Avvio Spring Boot con omniprice.rabbitmq.enabled=true..."
(cd "$ROOT_DIR/backend/omniprice" && ./mvnw spring-boot:run -Dspring-boot.run.arguments=--omniprice.rabbitmq.enabled=true) &
BACKEND_PID=$!

# Attende che il backend sia pronto (max 60s)
echo "[Backend] Attendo che sia pronto..."
for i in $(seq 1 30); do
  if curl -s -o /dev/null http://localhost:8080/api/auth/login 2>/dev/null; then
    echo "[Backend] Pronto!"
    break
  fi
  sleep 2
done

# 3. Avvio Frontend Vue 3
echo ""
echo "[Frontend] Avvio Vue su http://localhost:5173 ..."
(cd "$ROOT_DIR/frontend" && npm run dev) &
FRONTEND_PID=$!

echo ""
echo "=============================================="
echo "   TUTTI I SERVIZI SONO ATTIVI E PRONTI!      "
echo "=============================================="
echo "  🖥️  Frontend:             http://localhost:5173"
echo "  🍃 Backend API:           http://localhost:8080/api"
echo "  🗄️  H2 Console:            http://localhost:8080/api/h2-console"
echo "  🐇 RabbitMQ Management:   http://localhost:15672  (guest / guest)"
echo "=============================================="
echo ""
echo "Premi Ctrl+C per fermare il backend e il frontend."
echo "(Per fermare anche RabbitMQ: docker compose down)"
echo ""

wait
