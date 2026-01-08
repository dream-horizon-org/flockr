#!/bin/bash

# Teardown script for testing environment
# This script stops all services and cleans up

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================="
echo "Flockr Historic Engine - Teardown"
echo "========================================="
echo ""

# Determine docker-compose command
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

echo "🛑 Stopping services..."
$DOCKER_COMPOSE down

echo ""
echo "✅ All services stopped and removed"
echo ""

# Optional: Clean up volumes (uncomment if needed)
# echo "🧹 Cleaning up volumes..."
# $DOCKER_COMPOSE down -v

echo "Done!"

