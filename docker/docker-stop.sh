#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "🛑 Stopping Flockr Docker Environment..."
echo ""

# Stop all services
docker compose down

echo "✅ All services stopped."
echo ""
echo "💡 To remove all data volumes as well, run:"
echo "   docker compose down -v"
echo ""
