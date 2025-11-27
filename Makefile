# Makefile for Flockr Docker Management
.PHONY: help build up down restart status logs clean test

.DEFAULT_GOAL := help

help: ## Show this help message
	@echo "Flockr Docker Management"
	@echo "========================"
	@echo ""
	@echo "📚 Full documentation: See DOCKER.md"
	@echo ""
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: ## Build Docker images
	@echo "🔨 Building Docker images..."
	docker-compose build

up: ## Start all services
	@echo "🚀 Starting all services..."
	@./docker-start.sh

down: ## Stop all services
	@echo "🛑 Stopping all services..."
	@./docker-stop.sh

restart: ## Restart all services
	@echo "🔄 Restarting all services..."
	docker-compose restart

status: ## Check status of all services
	@./docker-status.sh

logs: ## View logs from all services
	docker-compose logs -f

logs-app: ## View application logs only
	docker-compose logs -f flockr-admin

clean: ## Remove all containers, volumes, and images
	@echo "🗑️  Cleaning up..."
	@./docker-clean.sh

test: ## Run application tests
	@echo "🧪 Running tests..."
	mvn clean test

test-integration: ## Run integration tests
	@echo "🧪 Running integration tests..."
	mvn clean verify

rebuild: ## Rebuild and restart the application
	@echo "🔨 Rebuilding application..."
	docker-compose up -d --build flockr-admin

shell-app: ## Open shell in application container
	docker-compose exec flockr-admin sh

shell-db: ## Open PostgreSQL shell
	docker-compose exec postgres psql -U flockr_user -d flockr

backup-db: ## Backup database
	@echo "💾 Backing up database..."
	docker-compose exec -T postgres pg_dump -U flockr_user flockr > backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "✅ Backup created: backup_$$(date +%Y%m%d_%H%M%S).sql"

restore-db: ## Restore database from backup (usage: make restore-db FILE=backup.sql)
	@if [ -z "$(FILE)" ]; then \
		echo "❌ Error: Please specify FILE parameter (e.g., make restore-db FILE=backup.sql)"; \
		exit 1; \
	fi
	@echo "📥 Restoring database from $(FILE)..."
	docker-compose exec -T postgres psql -U flockr_user -d flockr < $(FILE)
	@echo "✅ Database restored"

ps: ## List running containers
	docker-compose ps

stats: ## Show container resource usage
	docker stats --no-stream $$(docker-compose ps -q)

health: ## Check health of all services
	@echo "🏥 Checking service health..."
	@curl -sf http://localhost:8080/health && echo "✅ Flockr Admin: Healthy" || echo "❌ Flockr Admin: Unhealthy"
	@curl -sf http://localhost:8081/ && echo "✅ Flink: Healthy" || echo "❌ Flink: Unhealthy"
	@curl -sf http://localhost:8082/ && echo "✅ Spark: Healthy" || echo "❌ Spark: Unhealthy"

dev: ## Start in development mode with debug port
	@echo "🛠️  Starting in development mode..."
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

prod-build: ## Build production-ready image
	@echo "🏭 Building production image..."
	docker-compose build --no-cache

format: ## Format Java code
	mvn com.spotify.fmt:fmt-maven-plugin:format

lint: ## Check code formatting
	mvn com.spotify.fmt:fmt-maven-plugin:check
