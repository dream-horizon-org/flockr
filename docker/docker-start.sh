#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "🐳 Starting Flockr Docker Environment..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Build the application images
echo "🔨 Building Flockr application images..."
docker compose build

# Start all services
echo "🚀 Starting all services..."
docker compose up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
echo ""

# Wait for services to be healthy
max_attempts=60
attempt=0

while [ $attempt -lt $max_attempts ]; do
    attempt=$((attempt + 1))
    
    # Check PostgreSQL
    if docker compose exec -T flockr-postgres pg_isready -U flockr_user -d flockr > /dev/null 2>&1; then
        echo "✅ PostgreSQL is ready"
        break
    fi
    
    echo "⏳ Waiting for PostgreSQL... ($attempt/$max_attempts)"
    sleep 2
done

# Wait for Aerospike
attempt=0
while [ $attempt -lt $max_attempts ]; do
    attempt=$((attempt + 1))
    
    if docker compose exec -T flockr-aerospike asinfo -v status > /dev/null 2>&1; then
        echo "✅ Aerospike is ready"
        break
    fi
    
    echo "⏳ Waiting for Aerospike... ($attempt/$max_attempts)"
    sleep 2
done

# Wait a bit for other services to fully initialize
sleep 10

echo ""
echo "🎉 Flockr is now running!"
echo ""
echo "📊 Service URLs:"
echo "  • Flockr Admin API:    http://localhost:8250"
echo "  • Flockr Admin Swagger: http://localhost:8250/swagger-ui/"
echo "  • Flockr Users API:    http://localhost:8260"
echo "  • Flink Dashboard:     http://localhost:8240"
echo "  • Spark Master UI:     http://localhost:8210"
echo "  • Spark Worker UI:     http://localhost:8220"
echo "  • PostgreSQL:          localhost:8230"
echo "  • Aerospike:           localhost:8200"
echo ""
echo "🔍 Health Checks:"
echo "  • Admin Health:        curl http://localhost:8250/healthcheck"
echo "  • Users Health:        curl http://localhost:8260/healthcheck"
echo ""
echo "📝 Useful Commands:"
echo "  • View logs:           docker compose logs -f"
echo "  • View admin logs:     docker compose logs -f flockr-admin"
echo "  • View users logs:     docker compose logs -f flockr-users"
echo "  • Stop services:       docker compose down"
echo "  • Restart services:    docker compose restart"
echo "  • View status:         docker compose ps"
echo ""
echo "📚 Documentation:        See DOCKER.md for complete guide"
echo ""
