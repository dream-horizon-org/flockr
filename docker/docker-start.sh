#!/bin/bash
set -e

echo "🐳 Starting Flockr Docker Environment..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if .env file exists, if not copy from env.docker
if [ ! -f .env ]; then
    echo "📝 Creating .env file from env.docker template..."
    cp env.docker .env
    echo "✅ .env file created. You can customize it if needed."
    echo ""
fi

# Build the application image
echo "🔨 Building Flockr application image..."
docker-compose build

# Start all services
echo "🚀 Starting all services..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
echo ""

# Wait for services to be healthy
max_attempts=60
attempt=0

while [ $attempt -lt $max_attempts ]; do
    attempt=$((attempt + 1))
    
    # Check PostgreSQL
    if docker-compose exec -T postgres pg_isready -U flockr_user -d flockr > /dev/null 2>&1; then
        echo "✅ PostgreSQL is ready"
        break
    fi
    
    echo "⏳ Waiting for PostgreSQL... ($attempt/$max_attempts)"
    sleep 2
done

# Wait a bit for other services
sleep 10

echo ""
echo "🎉 Flockr is now running!"
echo ""
echo "📊 Service URLs:"
echo "  • Flockr Admin API:    http://localhost:8080"
echo "  • Swagger UI:          http://localhost:8080/swagger-ui/"
echo "  • Flink Dashboard:     http://localhost:8081"
echo "  • Spark Master UI:     http://localhost:8082"
echo "  • Spark Worker UI:     http://localhost:8083"
echo ""
echo "🔍 Health Check:"
echo "  • API Health:          curl http://localhost:8080/health"
echo ""
echo "📝 Useful Commands:"
echo "  • View logs:           docker-compose logs -f"
echo "  • View app logs:       docker-compose logs -f flockr-admin"
echo "  • Stop services:       docker-compose down"
echo "  • Restart services:    docker-compose restart"
echo "  • View status:         docker-compose ps"
echo ""
echo "📚 Documentation:       See DOCKER.md for complete guide"
echo ""

