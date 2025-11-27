#!/bin/bash

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
docker-compose ps

echo ""
echo "🔍 Service Health Checks:"
echo ""

# Check PostgreSQL
if docker-compose exec -T postgres pg_isready -U flockr_user -d flockr > /dev/null 2>&1; then
    echo "✅ PostgreSQL - Healthy"
else
    echo "❌ PostgreSQL - Unhealthy"
fi

# Check Flockr API
if curl -sf http://localhost:8080/health > /dev/null 2>&1; then
    echo "✅ Flockr Admin API - Healthy"
else
    echo "❌ Flockr Admin API - Unhealthy"
fi

# Check Flink
if curl -sf http://localhost:8081/ > /dev/null 2>&1; then
    echo "✅ Flink JobManager - Healthy"
else
    echo "❌ Flink JobManager - Unhealthy"
fi

# Check Spark Master
if curl -sf http://localhost:8082/ > /dev/null 2>&1; then
    echo "✅ Spark Master - Healthy"
else
    echo "❌ Spark Master - Unhealthy"
fi

echo ""
echo "📊 Resource Usage:"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" $(docker-compose ps -q)

echo ""
echo "💡 To view logs: docker-compose logs -f [service-name]"
echo ""

