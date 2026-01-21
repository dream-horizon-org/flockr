# =============================================================================
# Makefile for Flockr Multi-Module Docker Management
# =============================================================================
.PHONY: help build up down restart status logs clean test generate-keys
.DEFAULT_GOAL := help

# -----------------------------------------------------------------------------
# Help
# -----------------------------------------------------------------------------
help: ## Show this help message
	@echo "Flockr Docker Management"
	@echo "========================"
	@echo ""
	@echo "📚 Full documentation: See DOCKER.md"
	@echo ""
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

# -----------------------------------------------------------------------------
# Build Commands
# -----------------------------------------------------------------------------
build: ## Build all Docker images
	@echo "🔨 Building all Docker images..."
	docker-compose build

build-admin: ## Build flockr-admin image only
	@echo "🔨 Building flockr-admin..."
	docker-compose build flockr-admin

build-users: ## Build flockr-users image only
	@echo "🔨 Building flockr-users..."
	docker-compose build flockr-users

rebuild: ## Rebuild all images without cache
	@echo "🔨 Rebuilding all images..."
	docker-compose build --no-cache

rebuild-admin: ## Rebuild flockr-admin without cache
	@echo "🔨 Rebuilding flockr-admin..."
	docker-compose build --no-cache flockr-admin

rebuild-users: ## Rebuild flockr-users without cache
	@echo "🔨 Rebuilding flockr-users..."
	docker-compose build --no-cache flockr-users

# -----------------------------------------------------------------------------
# Service Management
# -----------------------------------------------------------------------------
up: ## Start all services
	@echo "🚀 Starting all services..."
	docker-compose up -d

up-admin: ## Start flockr-admin with dependencies
	@echo "🚀 Starting flockr-admin..."
	docker-compose up -d flockr-postgres flockr-admin

up-users: ## Start flockr-users with dependencies
	@echo "🚀 Starting flockr-users..."
	docker-compose up -d flockr-aerospike flockr-users

up-infra: ## Start infrastructure only (postgres, aerospike)
	@echo "🚀 Starting infrastructure..."
	docker-compose up -d flockr-postgres flockr-aerospike

down: ## Stop all services
	@echo "🛑 Stopping all services..."
	docker-compose down

down-admin: ## Stop flockr-admin only
	@echo "🛑 Stopping flockr-admin..."
	docker-compose stop flockr-admin

down-users: ## Stop flockr-users only
	@echo "🛑 Stopping flockr-users..."
	docker-compose stop flockr-users

restart: ## Restart all services
	@echo "🔄 Restarting all services..."
	docker-compose restart

restart-admin: ## Restart flockr-admin
	@echo "🔄 Restarting flockr-admin..."
	docker-compose restart flockr-admin

restart-users: ## Restart flockr-users
	@echo "🔄 Restarting flockr-users..."
	docker-compose restart flockr-users

# -----------------------------------------------------------------------------
# Status & Logs
# -----------------------------------------------------------------------------
status: ## Check status of all services
	@echo "📊 Service Status:"
	@docker-compose ps

ps: ## List running containers
	docker-compose ps

logs: ## View logs from all services
	docker-compose logs -f

logs-admin: ## View flockr-admin logs
	docker-compose logs -f flockr-admin

logs-users: ## View flockr-users logs
	docker-compose logs -f flockr-users

logs-infra: ## View infrastructure logs
	docker-compose logs -f flockr-postgres flockr-aerospike

stats: ## Show container resource usage
	docker stats --no-stream $$(docker-compose ps -q)

# -----------------------------------------------------------------------------
# Health Checks
# -----------------------------------------------------------------------------
health: ## Check health of all services
	@echo "🏥 Checking service health..."
	@echo ""
	@echo "Applications:"
	@curl -sf http://localhost:8080/healthcheck >/dev/null 2>&1 && echo "  ✅ flockr-admin (8080): Healthy" || echo "  ❌ flockr-admin (8080): Unhealthy"
	@curl -sf http://localhost:8082/healthcheck >/dev/null 2>&1 && echo "  ✅ flockr-users (8082): Healthy" || echo "  ❌ flockr-users (8082): Unhealthy"
	@echo ""
	@echo "Infrastructure:"
	@docker-compose exec -T flockr-postgres pg_isready -U flockr_user >/dev/null 2>&1 && echo "  ✅ PostgreSQL (5432): Healthy" || echo "  ❌ PostgreSQL (5432): Unhealthy"
	@docker-compose exec -T flockr-aerospike asinfo -v status >/dev/null 2>&1 && echo "  ✅ Aerospike (3000): Healthy" || echo "  ❌ Aerospike (3000): Unhealthy"

# -----------------------------------------------------------------------------
# Shell Access
# -----------------------------------------------------------------------------
shell-admin: ## Open shell in flockr-admin container
	docker-compose exec flockr-admin sh

shell-users: ## Open shell in flockr-users container
	docker-compose exec flockr-users sh

shell-db: ## Open PostgreSQL shell
	docker-compose exec flockr-postgres psql -U flockr_user -d flockr

shell-aerospike: ## Open Aerospike shell (aql)
	docker-compose exec flockr-aerospike aql

# -----------------------------------------------------------------------------
# Database Operations
# -----------------------------------------------------------------------------
backup-db: ## Backup PostgreSQL database
	@echo "💾 Backing up database..."
	@mkdir -p backups
	docker-compose exec -T flockr-postgres pg_dump -U flockr_user flockr > backups/backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "✅ Backup created in backups/"

restore-db: ## Restore database (usage: make restore-db FILE=backup.sql)
	@if [ -z "$(FILE)" ]; then \
		echo "❌ Error: Please specify FILE parameter (e.g., make restore-db FILE=backups/backup.sql)"; \
		exit 1; \
	fi
	@echo "📥 Restoring database from $(FILE)..."
	docker-compose exec -T flockr-postgres psql -U flockr_user -d flockr < $(FILE)
	@echo "✅ Database restored"

# -----------------------------------------------------------------------------
# Cleanup
# -----------------------------------------------------------------------------
clean: ## Remove all containers and volumes
	@echo "🗑️  Cleaning up containers and volumes..."
	docker-compose down -v --remove-orphans

clean-images: ## Remove all project images
	@echo "🗑️  Removing project images..."
	docker-compose down --rmi local

clean-all: ## Remove everything (containers, volumes, images, networks)
	@echo "🗑️  Full cleanup..."
	docker-compose down -v --rmi all --remove-orphans
	docker system prune -f

# -----------------------------------------------------------------------------
# Development
# -----------------------------------------------------------------------------
generate-keys: ## Generate encryption keys
	@echo "🔑 Generating encryption keys..."
	./docker/generate-encryption-keys.sh

generate-keys-file: ## Generate and save keys to env.docker
	@echo "🔑 Generating encryption keys and saving to env.docker..."
	./docker/generate-encryption-keys.sh --output-file env.docker

dev: ## Start in development mode (all services)
	@echo "🛠️  Starting development environment..."
	docker-compose up -d
	@echo ""
	@echo "Services available at:"
	@echo "  • flockr-admin:  http://localhost:8080"
	@echo "  • flockr-users:  http://localhost:8082"

dev-admin: ## Start flockr-admin in dev mode with hot reload
	@echo "🛠️  Starting flockr-admin dev environment..."
	docker-compose up -d flockr-postgres
	@echo "Run locally: mvn compile exec:java -pl flockr-admin"

dev-users: ## Start flockr-users in dev mode with hot reload
	@echo "🛠️  Starting flockr-users dev environment..."
	docker-compose up -d flockr-aerospike
	@echo "Run locally: mvn compile exec:java -pl flockr-users"

# -----------------------------------------------------------------------------
# Testing & Code Quality
# -----------------------------------------------------------------------------
test: ## Run all tests
	@echo "🧪 Running tests..."
	mvn clean test

test-admin: ## Run flockr-admin tests
	@echo "🧪 Running flockr-admin tests..."
	mvn clean test -pl flockr-admin

test-users: ## Run flockr-users tests
	@echo "🧪 Running flockr-users tests..."
	mvn clean test -pl flockr-users

test-integration: ## Run integration tests
	@echo "🧪 Running integration tests..."
	mvn clean verify

format: ## Format Java code
	mvn com.spotify.fmt:fmt-maven-plugin:format

lint: ## Check code formatting
	mvn com.spotify.fmt:fmt-maven-plugin:check

# -----------------------------------------------------------------------------
# Maven Build
# -----------------------------------------------------------------------------
mvn-build: ## Build all modules with Maven
	mvn clean package -DskipTests

mvn-build-admin: ## Build flockr-admin with Maven
	mvn clean package -DskipTests -pl flockr-admin -am

mvn-build-users: ## Build flockr-users with Maven
	mvn clean package -DskipTests -pl flockr-users -am
