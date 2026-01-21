#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "📊 Flockr Docker Services Status"
echo "================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running."
    exit 1
fi

# Show container status
echo "🐳 Container Status:"
docker compose ps

echo ""
echo "🔍 Service Health Checks:"
echo ""

# Check PostgreSQL
if docker compose exec -T flockr-postgres pg_isready -U flockr_user -d flockr > /dev/null 2>&1; then
    echo "✅ PostgreSQL          - Healthy (port 8230)"
else
    echo "❌ PostgreSQL          - Unhealthy"
fi

# Check Aerospike
if docker compose exec -T flockr-aerospike asinfo -v status > /dev/null 2>&1; then
    echo "✅ Aerospike           - Healthy (port 8200)"
else
    echo "❌ Aerospike           - Unhealthy"
fi

# Check Flockr Admin API
if curl -sf http://localhost:8250/healthcheck > /dev/null 2>&1; then
    echo "✅ Flockr Admin API    - Healthy (port 8250)"
else
    echo "❌ Flockr Admin API    - Unhealthy"
fi

# Check Flockr Users API
if curl -sf http://localhost:8260/healthcheck > /dev/null 2>&1; then
    echo "✅ Flockr Users API    - Healthy (port 8260)"
else
    echo "❌ Flockr Users API    - Unhealthy"
fi

# Check Spark Master
if curl -sf http://localhost:8210 > /dev/null 2>&1; then
    echo "✅ Spark Master        - Healthy (port 8210)"
else
    echo "❌ Spark Master        - Unhealthy"
fi

# Check Spark Worker
if curl -sf http://localhost:8220 > /dev/null 2>&1; then
    echo "✅ Spark Worker        - Healthy (port 8220)"
else
    echo "❌ Spark Worker        - Unhealthy"
fi

echo ""
echo "📊 Resource Usage:"
containers=$(docker compose ps -q 2>/dev/null)
if [ -n "$containers" ]; then
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" $containers
else
    echo "   No running containers found."
fi

echo ""
echo "📊 Service URLs:"
echo "  • Flockr Admin API:    http://localhost:8250"
echo "  • Flockr Users API:    http://localhost:8260"
echo "  • Spark Master Web UI: http://localhost:8210"
echo "  • Spark Worker Web UI: http://localhost:8220"
echo ""
echo "💡 To view logs: docker compose logs -f [service-name]"
echo ""
