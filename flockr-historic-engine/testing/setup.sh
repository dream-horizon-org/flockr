#!/bin/bash

# Setup script for testing environment
# This script starts all required services (Kafka, Spark, Mock API)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================="
echo "Flockr Historic Engine - Testing Setup"
echo "========================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Error: docker-compose is not installed. Please install it and try again."
    exit 1
fi

# Determine docker-compose command
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

echo "📦 Starting services..."
echo ""

# Create necessary directories
mkdir -p api-logs
chmod +x mock-api-server.py

# Start services
$DOCKER_COMPOSE up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 10

# Check service health
echo ""
echo "🔍 Checking service status..."
echo ""

# Check Zookeeper
if docker exec zookeeper nc -z localhost 2181 > /dev/null 2>&1; then
    echo "✅ Zookeeper is healthy"
else
    echo "⚠️  Zookeeper may still be starting..."
fi

# Check Kafka
if docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
    echo "✅ Kafka is healthy"
else
    echo "⚠️  Kafka may still be starting..."
fi

# Check Mock API
if curl -f http://localhost:8000/health > /dev/null 2>&1; then
    echo "✅ Mock API Server is healthy"
else
    echo "⚠️  Mock API Server may still be starting..."
fi

# Check Spark Master
if curl -f http://localhost:8080 > /dev/null 2>&1; then
    echo "✅ Spark Master is healthy"
else
    echo "⚠️  Spark Master may still be starting..."
fi

echo ""
echo "========================================="
echo "Services are starting up!"
echo "========================================="
echo ""
echo "Service URLs:"
echo "  - Spark Master UI:     http://localhost:8080"
echo "  - Spark Worker UI:     http://localhost:8081"
echo "  - Mock API Server:     http://localhost:8000/health"
echo "  - Kafka Broker:        localhost:9092"
echo ""
echo "To view logs:"
echo "  $DOCKER_COMPOSE logs -f [service-name]"
echo ""
echo "To stop services:"
echo "  $DOCKER_COMPOSE down"
echo ""
echo "To check service status:"
echo "  $DOCKER_COMPOSE ps"
echo ""

