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
- [Development Mode](#development-mode)
- [Production Mode](#production-mode)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

```bash
# Clone and start
git clone https://github.com/yourusername/flockr.git
cd flockr
./docker-start.sh
```

Access services:
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui/
- **Flink Dashboard**: http://localhost:8081
- **Spark Master**: http://localhost:8082
- **Spark Worker**: http://localhost:8083

## Prerequisites

- **Docker** 20.10+ ([Install](https://docs.docker.com/get-docker/))
- **Docker Compose** v2.0+ ([Install](https://docs.docker.com/compose/install/))
- At least **4GB RAM** allocated to Docker
- At least **10GB** disk space

Verify installation:
```bash
docker --version
docker-compose --version
```

## Services & Architecture

### Stack Overview

```
┌─────────────────────────────────────────────────────────┐
│                   Flockr Admin (8080)                    │
│              REST API + Business Logic                   │
└─────────────────────────────────────────────────────────┘
          │         │         │         │
          ▼         ▼         ▼         ▼
   ┌──────────┐ ┌─────┐ ┌───────┐ ┌──────────┐
   │PostgreSQL│ │Flink│ │ Spark │ │Aerospike │
   │   :5432  │ │:8081│ │ :8082 │ │  :3000   │
   └──────────┘ └─────┘ └───────┘ └──────────┘
```

### Services

| Service | Version | Port(s) | Description |
|---------|---------|---------|-------------|
| **Flockr Admin** | 1.0 | 8080 | Main application (Vert.x 4.4.9, Java 17) |
| **PostgreSQL** | 16 | 5432 | Primary database with auto-initialization |
| **Flink JobManager** | 1.17 | 8081 | Stream processing coordinator |
| **Flink TaskManager** | 1.17 | - | Stream processing workers (scalable) |
| **Spark Master** | 3.5 | 7077, 8082 | Distributed computing master |
| **Spark Worker** | 3.5 | 8083 | Distributed computing worker (scalable) |
| **Aerospike** | latest | 3000-3003 | Distributed in-memory cache |

### Key Features

- **Multi-stage Docker build** - Optimized image size with Alpine JRE
- **Health checks** - All services monitored
- **Auto-initialization** - Database schema loaded on startup
- **Horizontal scaling** - Scale Flink and Spark workers
- **Non-root execution** - Enhanced security
- **Resource management** - Configurable limits and reservations

## Installation

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/flockr.git
cd flockr
```

### 2. Environment Setup (Optional)

The default configuration works out of the box. To customize:

```bash
cp env.docker .env
# Edit .env with your settings
```

Key environment variables:
```bash
POSTGRES_USER=flockr_user           # Database username
POSTGRES_PASSWORD=flockr_password   # Database password
JAVA_OPTS=-Xms512m -Xmx1024m       # JVM settings
```

### 3. Build and Start

```bash
./docker-start.sh
```

This script will:
- Create `.env` if it doesn't exist
- Build the application image
- Start all services
- Wait for health checks
- Display service URLs

## Configuration

### File Structure

```
flockr/
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Main services
├── docker-compose.dev.yml           # Dev overrides (debug + PGAdmin)
├── docker-compose.prod.yml          # Prod overrides (resource limits)
├── .dockerignore                    # Build exclusions
├── env.docker                       # Environment template
├── docker/
│   ├── aerospike/aerospike.conf    # Aerospike server config
│   └── config/                      # Service configurations
│       ├── aerospike.conf
│       ├── flink.conf
│       └── postgres.conf
└── docker-*.sh                      # Management scripts
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_USER` | flockr_user | Database username |
| `POSTGRES_PASSWORD` | flockr_password | Database password |
| `POSTGRES_HOST` | postgres | Database hostname |
| `FLINK_HOST` | flink-jobmanager | Flink hostname |
| `SPARK_MASTER_URL` | spark://spark-master:7077 | Spark master URL |
| `AEROSPIKE_HOST` | aerospike | Cache hostname |
| `JAVA_OPTS` | -Xms512m -Xmx1024m | JVM options |

### Custom Overrides

Create `docker-compose.override.yml` for local customizations:

```yaml
version: '3.8'
services:
  flockr-admin:
    environment:
      - JAVA_OPTS=-Xms1g -Xmx2g
    ports:
      - "8081:8080"  # Use different port
```

## Running

### Using Shell Scripts (Recommended)

```bash
# Start all services
./docker-start.sh

# Check status and health
./docker-status.sh

# Stop services
./docker-stop.sh

# Clean everything (removes volumes!)
./docker-clean.sh
```

### Using Make (If installed)

```bash
make up          # Start services
make status      # Check status
make logs        # View all logs
make logs-app    # View app logs only
make down        # Stop services
make clean       # Clean up everything
make help        # See all commands
```

### Using Docker Compose Directly

```bash
# Start in background
docker-compose up -d

# View logs
docker-compose logs -f
docker-compose logs -f flockr-admin

# Check status
docker-compose ps

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Scaling Services

```bash
# Scale Flink TaskManagers
docker-compose up -d --scale flink-taskmanager=3

# Scale Spark Workers
docker-compose up -d --scale spark-worker=2
```

## Management

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f flockr-admin
docker-compose logs -f postgres

# Last 100 lines
docker-compose logs --tail=100 flockr-admin
```

### Execute Commands

```bash
# PostgreSQL shell
docker-compose exec postgres psql -U flockr_user -d flockr

# Application shell
docker-compose exec flockr-admin sh

# Run SQL script
docker-compose exec -T postgres psql -U flockr_user -d flockr < script.sql
```

### Database Operations

```bash
# Backup database
docker-compose exec -T postgres pg_dump -U flockr_user flockr > backup.sql

# Restore database
docker-compose exec -T postgres psql -U flockr_user -d flockr < backup.sql

# Connect to database
docker-compose exec postgres psql -U flockr_user -d flockr
```

### Rebuild Application

After code changes:

```bash
# Rebuild and restart
docker-compose up -d --build flockr-admin

# Or rebuild without cache
docker-compose build --no-cache flockr-admin
docker-compose up -d flockr-admin
```

## Development Mode

Development mode includes:
- **Java Remote Debugging** - Port 5005
- **PGAdmin** - Database management UI at http://localhost:5050
- **Hot Reload Support** - Mount local resources
- **Verbose Logging**

### Start Dev Mode

```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

### PGAdmin Access

- URL: http://localhost:5050
- Email: `admin@flockr.local`
- Password: `admin`

Add server in PGAdmin:
- Host: `postgres`
- Port: `5432`
- Database: `flockr`
- Username: `flockr_user`
- Password: `flockr_password`

### Remote Debugging

Configure your IDE to connect to `localhost:5005`:

**IntelliJ IDEA:**
1. Run → Edit Configurations
2. Add Remote JVM Debug
3. Host: `localhost`, Port: `5005`
4. Start debugging

**VS Code (launch.json):**
```json
{
  "type": "java",
  "name": "Debug Flockr",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005
}
```

## Production Mode

Production mode includes:
- **Resource Limits** - CPU and memory constraints
- **Auto-restart Policies** - Restart on failure
- **Production JVM Settings** - Optimized heap and GC
- **No Debug Tools** - Minimal attack surface

### Start Production Mode

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Resource Configuration

Edit `docker-compose.prod.yml` to adjust resources:

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

### Production Checklist

- [ ] Use external managed database (not containerized)
- [ ] Configure secrets management (not `.env` files)
- [ ] Enable TLS/SSL
- [ ] Set resource limits
- [ ] Configure centralized logging
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Regular backup strategy
- [ ] Use orchestration (Kubernetes) for production scale

## Troubleshooting

### Services Won't Start

```bash
# Check logs
docker-compose logs

# Check specific service
docker-compose logs flockr-admin

# Restart service
docker-compose restart flockr-admin

# Clean start
./docker-clean.sh
./docker-start.sh
```

### Port Already in Use

Find what's using the port:
```bash
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows
```

Change port in `docker-compose.override.yml`:
```yaml
services:
  flockr-admin:
    ports:
      - "8081:8080"
```

### Database Connection Issues

```bash
# Check PostgreSQL status
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Test connection from app container
docker-compose exec flockr-admin sh -c 'nc -zv postgres 5432'

# Reset database
docker-compose down -v
docker-compose up -d
```

### Application Won't Start

```bash
# Check health
curl http://localhost:8080/health

# Check logs
docker-compose logs flockr-admin

# Check Java process
docker-compose exec flockr-admin ps aux

# Restart with fresh build
docker-compose up -d --build --force-recreate flockr-admin
```

### Out of Memory

**Increase Docker Memory:**
- Docker Desktop → Settings → Resources → Memory
- Increase to at least 4GB

**Reduce Service Memory:**
Edit `.env`:
```bash
JAVA_OPTS=-Xms256m -Xmx512m
```

Or in `docker-compose.yml`:
```yaml
services:
  spark-worker:
    environment:
      - SPARK_WORKER_MEMORY=512M
```

### Network Issues

```bash
# Recreate network
docker-compose down
docker network prune
docker-compose up -d
```

### Clean Slate

If nothing works:
```bash
# Stop everything
docker-compose down -v

# Remove images
docker rmi $(docker images 'flockr*' -q)

# Clean Docker
docker system prune -a --volumes

# Start fresh
./docker-start.sh
```

## Performance Tuning

### JVM Tuning

```bash
# In .env file
JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError
```

### PostgreSQL Tuning

Mount custom config in `docker-compose.yml`:
```yaml
services:
  postgres:
    volumes:
      - ./postgresql.conf:/etc/postgresql/postgresql.conf
    command: postgres -c config_file=/etc/postgresql/postgresql.conf
```

### Resource Monitoring

```bash
# Real-time stats
docker stats

# Specific services
docker stats $(docker-compose ps -q)

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
- `aerospike_data` - Aerospike cache data
- `spark_master_data` - Spark master metadata
- `spark_worker_data` - Spark worker data

## Health Checks

### Check All Services

```bash
# Using status script
./docker-status.sh

# Manual checks
curl http://localhost:8080/health          # Flockr Admin
curl http://localhost:8081/                # Flink
curl http://localhost:8082/                # Spark Master
docker-compose exec postgres pg_isready    # PostgreSQL
```

### Health Endpoints

| Service | Health Check |
|---------|-------------|
| Flockr Admin | `GET /health` |
| Flink | `GET /` (returns HTML) |
| Spark | `GET /` (returns HTML) |
| PostgreSQL | `pg_isready` command |

## CI/CD Integration

The project includes GitHub Actions workflow at `.github/workflows/docker-build.yml`:

- Builds Docker images on push/PR
- Runs health checks
- Performs security scanning with Trivy
- Uploads results to GitHub Security tab

### Running Locally

```bash
# Build like CI does
docker build -t flockr-admin:test .

# Test like CI does
docker-compose up -d
sleep 30
curl -f http://localhost:8080/health
docker-compose down -v
```

## Architecture Notes

### Data Flow

```
Client Request → [Flockr Admin]
                      ↓
      ┌───────────────┼───────────────┐
      ↓               ↓               ↓
[PostgreSQL]      [Aerospike]     [Flink]
   (Store)         (Cache)      (Process)
      ↓               ↓               ↓
   Response ← [Business Logic] ← [Spark]
```

### Internal Networking

All services communicate via `flockr-network` bridge network.

**Internal hostnames:**
- `flockr-admin` - Application
- `postgres` - Database
- `flink-jobmanager` - Flink coordinator
- `flink-taskmanager` - Flink workers
- `spark-master` - Spark master
- `spark-worker` - Spark workers
- `aerospike` - Cache

## Additional Resources

- **Main README**: [README.md](README.md)
- **API Documentation**: http://localhost:8080/swagger-ui/
- **Docker Docs**: https://docs.docker.com/
- **Compose Docs**: https://docs.docker.com/compose/

## Support

- **Issues**: https://github.com/yourusername/flockr/issues
- **Discussions**: https://github.com/yourusername/flockr/discussions

---

**Built with ❤️ using Docker, Vert.x, Flink, and Spark**
