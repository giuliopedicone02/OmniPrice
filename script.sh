#!/bin/bash
set -e
trap 'echo ""; echo "Arresto in corso..."; kill 0' SIGINT SIGTERM

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== OmniPrice - Avvio ==="
echo ""

# Backend
echo "[Backend] Avvio Spring Boot su http://localhost:8080/api ..."
(cd "$ROOT_DIR/backend/omniprice" && ./mvnw spring-boot:run) &
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

# Frontend
echo ""
echo "[Frontend] Avvio Vue su http://localhost:5173 ..."
(cd "$ROOT_DIR/frontend" && npm run dev) &
FRONTEND_PID=$!

echo ""
echo "=== Tutto avviato ==="
echo "  Backend:  http://localhost:8080/api"
echo "  Frontend: http://localhost:5173"
echo "  H2 Console: http://localhost:8080/api/h2-console"
echo ""
echo "Premi Ctrl+C per fermare tutto."

wait
