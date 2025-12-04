#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "🗑️  Cleaning up Flockr Docker Environment..."
echo ""

# Stop and remove containers, networks, volumes
docker compose down -v

# Remove built images
echo "🧹 Removing Flockr images..."
docker images | grep -E "flockr" | awk '{print $3}' | xargs -r docker rmi -f 2>/dev/null || true

# Remove any dangling images
echo "🧹 Removing dangling images..."
docker image prune -f

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "💡 To start fresh, run: ./docker/docker-start.sh"
echo ""
