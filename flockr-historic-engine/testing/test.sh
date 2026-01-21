#!/bin/bash

# Unified test script for Flocker Historic Engine
# This script handles setup, credential replacement, test execution, and teardown

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# Configuration
JAR_NAME="flockr-historic-engine-0.1.0-SNAPSHOT.jar"
IMAGE_NAME="flockr-historic-engine"
IMAGE_TAG="test"
TEST_CONFIG_FILE="$SCRIPT_DIR/test-config.json"
SECRETS_FILE="$SCRIPT_DIR/.test-secrets"
ENCRYPTION_KEY_FILE="$SCRIPT_DIR/.encryption-key"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Function to decrypt credentials (if using encryption)
decrypt_value() {
    local encrypted_value="$1"
    local encryption_key="${ENCRYPTION_KEY:-test-encryption-key-2024}"
    
    if [[ "$encrypted_value" == encrypted:* ]]; then
        local encrypted_part="${encrypted_value#encrypted:}"
        echo "$encrypted_part" | openssl enc -aes-256-cbc -d -a -pbkdf2 -iter 10000 -salt -pass pass:"$encryption_key" 2>/dev/null || \
        echo "$encrypted_part" | openssl enc -aes-256-cbc -d -a -salt -pass pass:"$encryption_key" 2>/dev/null
    else
        echo "$encrypted_value"
    fi
}

# Function to replace placeholders with actual credentials
replace_credentials() {
    local config_content="$1"
    
    # Load secrets from file if it exists
    if [ -f "$SECRETS_FILE" ]; then
        source "$SECRETS_FILE"
    fi
    
    # Replace placeholders with environment variables or secrets file values
    config_content=$(echo "$config_content" | sed "s|test_access_key|${TEST_ACCESS_KEY:-test_access_key}|g")
    config_content=$(echo "$config_content" | sed "s|test_secret_key|${TEST_SECRET_KEY:-test_secret_key}|g")
    config_content=$(echo "$config_content" | sed "s|test_session_token|${TEST_SESSION_TOKEN:-test_session_token}|g")
    
    # Handle encrypted values if present
    while IFS= read -r line; do
        if [[ "$line" =~ encrypted: ]]; then
            decrypted=$(decrypt_value "$line")
            config_content=$(echo "$config_content" | sed "s|$line|$decrypted|g")
        fi
    done <<< "$config_content"
    
    echo "$config_content"
}

# Function to setup services
setup_services() {
    log_info "Setting up test services..."
    cd "$SCRIPT_DIR"
    
    # Check if Docker is running
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    
    # Determine docker-compose command
    if docker compose version &> /dev/null; then
        DOCKER_COMPOSE="docker compose"
    else
        DOCKER_COMPOSE="docker-compose"
    fi
    
    # Create necessary directories
    mkdir -p api-logs
    chmod +x mock-api-server.py 2>/dev/null || true
    
    # Start services
    $DOCKER_COMPOSE up -d
    
    log_info "Waiting for services to be healthy..."
    sleep 10
    
    # Check service health
    log_info "Checking service status..."
    
    local services_healthy=true
    
    if docker exec zookeeper nc -z localhost 2181 > /dev/null 2>&1; then
        log_success "Zookeeper is healthy"
    else
        log_warning "Zookeeper may still be starting..."
        services_healthy=false
    fi
    
    if docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        log_success "Kafka is healthy"
    else
        log_warning "Kafka may still be starting..."
        services_healthy=false
    fi
    
    if curl -f http://localhost:8000/health > /dev/null 2>&1; then
        log_success "Mock API Server is healthy"
    else
        log_warning "Mock API Server may still be starting..."
        services_healthy=false
    fi
    
    if curl -f http://localhost:8080 > /dev/null 2>&1; then
        log_success "Spark Master is healthy"
    else
        log_warning "Spark Master may still be starting..."
        services_healthy=false
    fi
    
    if [ "$services_healthy" = false ]; then
        log_warning "Some services may not be fully ready. Continuing anyway..."
    fi
    
    cd "$PROJECT_ROOT"
}

# Function to teardown services
teardown_services() {
    log_info "Tearing down test services..."
    cd "$SCRIPT_DIR"
    
    if docker compose version &> /dev/null; then
        DOCKER_COMPOSE="docker compose"
    else
        DOCKER_COMPOSE="docker-compose"
    fi
    
    $DOCKER_COMPOSE down
    
    log_success "All services stopped and removed"
    cd "$PROJECT_ROOT"
}

# Function to build JAR and Docker image
build_artifacts() {
    log_info "Building JAR..."
    mvn clean package -DskipTests
    
    if [ ! -f "target/$JAR_NAME" ]; then
        log_error "JAR file not found at target/$JAR_NAME"
        exit 1
    fi
    
    log_success "JAR built successfully"
    
    log_info "Building Docker image..."
    docker build -t "$IMAGE_NAME:$IMAGE_TAG" .
    log_success "Docker image built successfully"
}

# Function to create basic sanity test config
create_sanity_test_config() {
    cat <<EOF
{
  "audienceName": "sanity-test-audience",
  "action": "append",
  "expireAt": 1735689599,
  "source": {
    "type": "S3",
    "config": {
      "bucket": "test-bucket",
      "key": "test-data.csv",
      "format": "CSV"
    }
  },
  "destinationJson": [
    {
      "type": "API",
      "config": {
        "url": "http://host.docker.internal:8000/api/audience/update",
        "rateLimitPerSecond": 10,
        "batchSize": 100,
        "timeoutSeconds": 30,
        "contentType": "application/json"
      }
    }
  ]
}
EOF
}

# Function to run the test
run_test() {
    log_info "Preparing test configuration..."
    
    # Check if sanity test is requested (passed as argument)
    if [ "${1:-false}" = "true" ]; then
        log_info "Running basic sanity test..."
        # Use built-in sanity test config
        TEST_CONFIG=$(create_sanity_test_config)
    elif [ ! -f "$TEST_CONFIG_FILE" ]; then
        log_error "Test config file not found at $TEST_CONFIG_FILE"
        log_info "Use --sanity-test for a basic test, or create $TEST_CONFIG_FILE"
        exit 1
    else
        # Read and process config from file
        TEST_CONFIG=$(cat "$TEST_CONFIG_FILE")
        TEST_CONFIG=$(replace_credentials "$TEST_CONFIG")
    fi
    
    log_info "Running test job..."
    echo ""
    echo "Test Configuration:"
    echo "$TEST_CONFIG" | jq '.' 2>/dev/null || echo "$TEST_CONFIG"
    echo ""
    
    # Determine network name
    NETWORK_NAME="testing_testing-network"
    
    # Check if network exists, if not use host network
    if docker network inspect "$NETWORK_NAME" > /dev/null 2>&1; then
        log_info "Using Docker network: $NETWORK_NAME"
        NETWORK_ARG="--network $NETWORK_NAME"
        # When on the same network, use service names
        TEST_CONFIG=$(echo "$TEST_CONFIG" | sed 's/localhost:8000/mock-api:8080/g')
        TEST_CONFIG=$(echo "$TEST_CONFIG" | sed 's/host.docker.internal:8000/mock-api:8080/g')
        TEST_CONFIG=$(echo "$TEST_CONFIG" | sed 's/localhost:9092/kafka:9093/g')
    else
        log_info "Network not found, using host network mode"
        NETWORK_ARG="--network host"
        # When using host network, use localhost
        TEST_CONFIG=$(echo "$TEST_CONFIG" | sed 's/kafka:9093/localhost:9092/g')
        TEST_CONFIG=$(echo "$TEST_CONFIG" | sed 's/mock-api:8080/localhost:8000/g')
    fi
    
    # Run the Docker container
    docker run --rm \
      $NETWORK_ARG \
      -e JAVA_OPTS="-Xmx2g -Xms1g -Dspark.master=local[*]" \
      "$IMAGE_NAME:$IMAGE_TAG" \
      "$TEST_CONFIG"
    
    log_success "Test completed!"
}

# Main script logic
main() {
    echo "========================================="
    echo "Flockr Historic Engine - Unified Test"
    echo "========================================="
    echo ""
    
    # Parse command line arguments
    SETUP_ONLY=false
    TEARDOWN_ONLY=false
    SKIP_SETUP=false
    SKIP_TEARDOWN=false
    SKIP_BUILD=false
    SANITY_TEST=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --setup-only)
                SETUP_ONLY=true
                shift
                ;;
            --teardown-only)
                TEARDOWN_ONLY=true
                shift
                ;;
            --skip-setup)
                SKIP_SETUP=true
                shift
                ;;
            --skip-teardown)
                SKIP_TEARDOWN=true
                shift
                ;;
            --skip-build)
                SKIP_BUILD=true
                shift
                ;;
            --sanity-test)
                SANITY_TEST=true
                shift
                ;;
            --help|-h)
                echo "Usage: $0 [OPTIONS]"
                echo ""
                echo "Options:"
                echo "  --setup-only      Only setup services, don't run tests"
                echo "  --teardown-only   Only teardown services"
                echo "  --skip-setup      Skip service setup"
                echo "  --skip-teardown   Skip service teardown"
                echo "  --skip-build      Skip JAR and Docker image build"
                echo "  --sanity-test     Run basic sanity test (no config file needed)"
                echo "  --help, -h        Show this help message"
                echo ""
                echo "Environment Variables:"
                echo "  TEST_ACCESS_KEY       AWS access key (replaces test_access_key)"
                echo "  TEST_SECRET_KEY       AWS secret key (replaces test_secret_key)"
                echo "  TEST_SESSION_TOKEN    AWS session token (replaces test_session_token)"
                echo "  ENCRYPTION_KEY        Encryption key for decrypting credentials"
                echo ""
                echo "Secrets File:"
                echo "  Create $SECRETS_FILE with:"
                echo "    export TEST_ACCESS_KEY='your-key'"
                echo "    export TEST_SECRET_KEY='your-secret'"
                echo "    export TEST_SESSION_TOKEN='your-token'"
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done
    
    # Handle special modes
    if [ "$SETUP_ONLY" = true ]; then
        setup_services
        exit 0
    fi
    
    if [ "$TEARDOWN_ONLY" = true ]; then
        teardown_services
        exit 0
    fi
    
    # Normal test flow
    if [ "$SKIP_SETUP" = false ]; then
        setup_services
    fi
    
    if [ "$SKIP_BUILD" = false ]; then
        build_artifacts
    fi
    
    run_test "$SANITY_TEST"
    
    if [ "$SKIP_TEARDOWN" = false ]; then
        echo ""
        read -p "Keep services running? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            teardown_services
        else
            log_info "Services are still running. Use '$0 --teardown-only' to stop them."
        fi
    fi
    
    echo ""
    log_info "Service URLs:"
    echo "  - Spark Master UI:     http://localhost:8080"
    echo "  - Spark Worker UI:     http://localhost:8081"
    echo "  - Mock API Server:     http://localhost:8000/health"
    echo "  - Kafka Broker:        localhost:9092"
    echo ""
}

# Run main function
main "$@"

