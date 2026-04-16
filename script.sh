#!/bin/bash
trap 'kill 0' SIGINT

echo "🚀 Avvio Backend..."
cd backend/omniprice && ./mvnw spring-boot:run &

echo "🚀 Avvio Frontend..."
cd frontend && npm run dev &

wait