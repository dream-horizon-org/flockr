# 🐳 Flockr Docker Guide

Complete guide for running Flockr with Docker and Docker Compose.

## Table of Contents

- [Quick Start](#quick-start)
- [Prerequisites](#prerequisites)
- [Services & Architecture](#services--architecture)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running](#running)
- [Management](#management)
- [Production Considerations](#production-considerations)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

```bash
# Clone and start
git clone https://github.com/yourusername/flockr.git
cd flockr
./docker/docker-start.sh
```

Access services:
- **Flockr Admin API**: http://localhost:8250
- **Flockr Users API**: http://localhost:8260
- **Swagger UI**: http://localhost:8250/swagger-ui/

## Prerequisites

- **Docker** 20.10+ ([Install](https://docs.docker.com/get-docker/))
- **Docker Compose** v2.0+ ([Install](https://docs.docker.com/compose/install/))
- At least **4GB RAM** allocated to Docker
- At least **10GB** disk space

Verify installation:
```bash
docker --version
docker compose version
```

## Services & Architecture

### Stack Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│  Flockr Admin (8250)           Flockr Users (8260)                  │
│  REST API + Business Logic     User Cohort Management               │
└─────────────────────────────────────────────────────────────────────┘
          │                                │
          ▼                                ▼
   ┌──────────┐                     ┌───────────┐
   │PostgreSQL│                     │ Aerospike │
   │  :8230   │                     │   :8200   │
   └──────────┘                     └───────────┘
```

### Services

| Service | Version | Port(s) | Description |
|---------|---------|---------|-------------|
| **Flockr Admin** | 1.0 | 8250 | Admin application (Vert.x 4.4.9, Java 17) |
| **Flockr Users** | 1.0 | 8260 | User cohort service (Vert.x 4.4.9, Java 17) |
| **PostgreSQL** | 16 | 8230 | Primary database with auto-initialization |
| **Aerospike** | 6.4 | 8200-8202 | High-performance NoSQL for user data |

### Key Features

- **Multi-stage Docker build** - Optimized image size with Alpine JRE
- **Health checks** - All services monitored
- **Auto-initialization** - Database schema loaded on startup
- **Non-root execution** - Enhanced security
- **Resource management** - Configurable limits and reservations

## Installation

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/flockr.git
cd flockr
```

### 2. Environment Configuration

The default configuration in `env.docker` works out of the box.

**Generate Encryption Keys (Required for credential security):**

```bash
# Generate keys and save to env.docker
make generate-keys-file

# Or manually
./docker/generate-encryption-keys.sh --output-file env.docker
```

Key environment variables in `env.docker`:
```bash
POSTGRES_USER=flockr_user           # Database username
POSTGRES_PASSWORD=flockr_password   # Database password
ENCRYPTION_KEY=<generated>          # Frontend encryption key
BACKEND_STORAGE_ENCRYPTION_KEY=<generated>  # Backend storage encryption key
JAVA_OPTS=-Xms512m -Xmx1024m       # JVM settings
```

> **Note**: See [ENCRYPTION.md](ENCRYPTION.md) for details on the double-layer credential encryption system.

### 3. Build and Start

```bash
./docker/docker-start.sh
```

This script will:
- Build the application images
- Start all services
- Wait for health checks
- Display service URLs

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_USER` | flockr_user | Database username |
| `POSTGRES_PASSWORD` | flockr_password | Database password |
| `POSTGRES_HOST` | flockr-postgres | Database hostname |
| `JAVA_OPTS` | -Xms512m -Xmx1024m | JVM options |

### Custom Overrides

Create `docker-compose.override.yml` for local customizations:

```yaml
services:
  flockr-admin:
    environment:
      - JAVA_OPTS=-Xms1g -Xmx2g
    ports:
      - "9250:8080"  # Use different port
```

## Running

### Using Shell Scripts (Recommended)

```bash
# Start all services
./docker/docker-start.sh

# Check status and health
./docker/docker-status.sh

# Stop services
./docker/docker-stop.sh

# Clean everything (removes volumes!)
./docker/docker-clean.sh
```

### Using Make (If installed)

```bash
make generate-keys  # Generate encryption keys
make up             # Start services
make status         # Check status
make logs           # View all logs
make logs-app       # View app logs only
make down           # Stop services
make clean          # Clean up everything
make help           # See all commands
```

### Using Docker Compose Directly

```bash
# Start in background
docker compose up -d

# View logs
docker compose logs -f
docker compose logs -f flockr-admin

# Check status
docker compose ps

# Stop services
docker compose down

# Stop and remove volumes
docker compose down -v
```

## Management

### View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f flockr-admin
docker compose logs -f flockr-users
docker compose logs -f flockr-postgres

# Last 100 lines
docker compose logs --tail=100 flockr-admin
```

### Execute Commands

```bash
# PostgreSQL shell
docker compose exec flockr-postgres psql -U flockr_user -d flockr

# Application shell
docker compose exec flockr-admin sh

# Aerospike shell
docker compose exec flockr-aerospike aql

# Run SQL script
docker compose exec -T flockr-postgres psql -U flockr_user -d flockr < script.sql
```

### Database Operations

```bash
# Backup database
docker compose exec -T flockr-postgres pg_dump -U flockr_user flockr > backup.sql

# Restore database
docker compose exec -T flockr-postgres psql -U flockr_user -d flockr < backup.sql

# Connect to database
docker compose exec flockr-postgres psql -U flockr_user -d flockr
```

### Rebuild Application

After code changes:

```bash
# Rebuild and restart
docker compose up -d --build flockr-admin flockr-users

# Or rebuild without cache
docker compose build --no-cache flockr-admin flockr-users
docker compose up -d flockr-admin flockr-users
```

## Production Considerations

### Production Checklist

- [ ] Use external managed database (not containerized)
- [ ] Configure secrets management (not environment files)
- [ ] Enable TLS/SSL
- [ ] Set resource limits in `docker-compose.yml`
- [ ] Configure centralized logging
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Regular backup strategy
- [ ] Use orchestration (Kubernetes) for production scale

### Resource Configuration

Add resource limits to `docker-compose.yml`:

```yaml
services:
  flockr-admin:
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 4G
        reservations:
          cpus: '2'
          memory: 2G
```

## Troubleshooting

### Services Won't Start

```bash
# Check logs
docker compose logs

# Check specific service
docker compose logs flockr-admin

# Restart service
docker compose restart flockr-admin

# Clean start
./docker/docker-clean.sh
./docker/docker-start.sh
```

### Port Already in Use

Find what's using the port:
```bash
lsof -i :8250  # macOS/Linux
netstat -ano | findstr :8250  # Windows
```

Change port in `docker-compose.override.yml`:
```yaml
services:
  flockr-admin:
    ports:
      - "9250:8080"
```

### Database Connection Issues

```bash
# Check PostgreSQL status
docker compose ps flockr-postgres

# Check PostgreSQL logs
docker compose logs flockr-postgres

# Test connection from app container
docker compose exec flockr-admin sh -c 'nc -zv flockr-postgres 5432'

# Reset database
docker compose down -v
docker compose up -d
```

### Aerospike Connection Issues

```bash
# Check Aerospike status
docker compose ps flockr-aerospike

# Check Aerospike logs
docker compose logs flockr-aerospike

# Test connection
docker compose exec flockr-aerospike asinfo -v status
```

### Application Won't Start

```bash
# Check health
curl http://localhost:8250/healthcheck
curl http://localhost:8260/healthcheck

# Check logs
docker compose logs flockr-admin
docker compose logs flockr-users

# Check Java process
docker compose exec flockr-admin ps aux

# Restart with fresh build
docker compose up -d --build --force-recreate flockr-admin flockr-users
```

### Out of Memory

**Increase Docker Memory:**
- Docker Desktop → Settings → Resources → Memory
- Increase to at least 4GB

**Reduce Service Memory:**
Edit `env.docker`:
```bash
JAVA_OPTS=-Xms256m -Xmx512m
```

### Network Issues

```bash
# Recreate network
docker compose down
docker network prune
docker compose up -d
```

### Clean Slate

If nothing works:
```bash
# Stop everything
docker compose down -v

# Remove images
docker rmi $(docker images 'flockr*' -q)

# Clean Docker
docker system prune -a --volumes

# Start fresh
./docker/docker-start.sh
```

## Performance Tuning

### JVM Tuning

```bash
# In env.docker file
JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError
```

### PostgreSQL Tuning

Mount custom config in `docker-compose.yml`:
```yaml
services:
  flockr-postgres:
    volumes:
      - ./postgresql.conf:/etc/postgresql/postgresql.conf
    command: postgres -c config_file=/etc/postgresql/postgresql.conf
```

### Resource Monitoring

```bash
# Real-time stats
docker stats

# Specific services
docker stats $(docker compose ps -q)

# Script-friendly output
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"
```

## Volumes and Data

### Volume Management

```bash
# List volumes
docker volume ls

# Inspect volume
docker volume inspect flockr_postgres_data

# Backup volume
docker run --rm -v flockr_postgres_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/postgres_backup.tar.gz -C /data .

# Restore volume
docker run --rm -v flockr_postgres_data:/data -v $(pwd):/backup \
  alpine tar xzf /backup/postgres_backup.tar.gz -C /data
```

### Persistent Data Locations

- `postgres_data` - PostgreSQL database files
- `aerospike_data` - Aerospike database files

## Health Checks

### Check All Services

```bash
# Using status script
./docker/docker-status.sh

# Manual checks
curl http://localhost:8250/healthcheck       # Flockr Admin
curl http://localhost:8260/healthcheck       # Flockr Users
docker compose exec flockr-postgres pg_isready    # PostgreSQL
docker compose exec flockr-aerospike asinfo -v status  # Aerospike
```

### Health Endpoints

| Service | Health Check |
|---------|-------------|
| Flockr Admin | `GET /healthcheck` (port 8250) |
| Flockr Users | `GET /healthcheck` (port 8260) |
| PostgreSQL | `pg_isready` command |
| Aerospike | `asinfo -v status` command |

## CI/CD Integration

The project includes GitHub Actions workflow at `.github/workflows/docker-build.yml`:

- Builds Docker images on push/PR
- Runs health checks
- Performs security scanning with Trivy
- Uploads results to GitHub Security tab

### Running Locally

```bash
# Build like CI does
docker build -t flockr-admin:test -f flockr-admin/Dockerfile .
docker build -t flockr-users:test -f flockr-users/Dockerfile .

# Test like CI does
docker compose up -d
sleep 30
curl -f http://localhost:8250/healthcheck
curl -f http://localhost:8260/healthcheck
docker compose down -v
```

## Architecture Notes

### Data Flow

```
Client Request → [Flockr Admin] ←→ [Flockr Users]
                      ↓                  ↓
      ┌───────────────┼──────────────────┼───────────────┐
      ↓               ↓                  ↓               ↓
[PostgreSQL]                        [Aerospike]
   (Store)                           (Users)
      ↓                                  ↓
   Response ← [Business Logic] ←────────┘
```

### Internal Networking

All services communicate via `flockr-network` bridge network.

**Internal hostnames:**
- `flockr-admin` - Admin Application
- `flockr-users` - Users Application
- `flockr-postgres` - Database
- `flockr-aerospike` - Aerospike Database

## Additional Resources

- **Main README**: [README.md](README.md)
- **Encryption Guide**: [ENCRYPTION.md](ENCRYPTION.md) - Double-layer credential encryption
- **API Documentation**: http://localhost:8250/swagger-ui/
- **Docker Docs**: https://docs.docker.com/
- **Compose Docs**: https://docs.docker.com/compose/

## Support

- **Issues**: https://github.com/yourusername/flockr/issues
- **Discussions**: https://github.com/yourusername/flockr/discussions

---

**Built with ❤️ using Docker, Vert.x, and Aerospike**
