#!/bin/bash
set -e

echo "🛑 Stopping Flockr Docker Environment..."
echo ""

# Stop all services
docker-compose down

echo "✅ All services stopped."
echo ""
echo "💡 To remove all data volumes as well, run:"
echo "   docker-compose down -v"
echo ""

