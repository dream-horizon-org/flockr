#!/bin/bash
set -e

echo "🗑️  Cleaning up Flockr Docker Environment..."
echo ""

# Stop and remove containers, networks, volumes
docker-compose down -v

# Remove built images
echo "🧹 Removing Flockr images..."
docker images | grep flockr | awk '{print $3}' | xargs -r docker rmi -f

echo "✅ Cleanup complete!"
echo ""
echo "💡 To start fresh, run: ./docker-start.sh"
echo ""

