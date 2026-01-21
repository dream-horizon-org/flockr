#!/bin/bash

###############################################################################
# Flockr Encryption Key Generator
#
# This script generates secure 256-bit AES encryption keys for the Flockr
# application's double-layer credential encryption system.
#
# Usage:
#   ./generate-encryption-keys.sh [--output-file FILE]
#
# Options:
#   --output-file FILE    Write keys to specified file (default: console only)
#   -h, --help           Display this help message
#
# Generated Keys:
#   1. ENCRYPTION_KEY - Shared with frontend (Layer 1 encryption)
#   2. BACKEND_STORAGE_ENCRYPTION_KEY - Backend-only (Layer 2 encryption)
#
# Requirements:
#   - OpenSSL (for generating random bytes)
#   - Base64 encoding utility (standard on most Unix systems)
###############################################################################

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
KEY_SIZE_BYTES=32  # 256 bits
OUTPUT_FILE=""

###############################################################################
# Functions
###############################################################################

# Display usage information
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Generate secure 256-bit AES encryption keys for Flockr application.

OPTIONS:
    --output-file FILE    Write keys to specified file (default: console only)
    -h, --help           Display this help message

EXAMPLES:
    # Generate keys and display on console
    $0

    # Generate keys and save to env file
    $0 --output-file .env

    # Generate keys and append to existing env file
    $0 --output-file env.docker

EOF
    exit 0
}

# Print error message and exit
error() {
    echo -e "${RED}ERROR: $1${NC}" >&2
    exit 1
}

# Print info message
info() {
    echo -e "${BLUE}$1${NC}"
}

# Print success message
success() {
    echo -e "${GREEN}$1${NC}"
}

# Print warning message
warning() {
    echo -e "${YELLOW}$1${NC}"
}

# Check if required commands are available
check_requirements() {
    if ! command -v openssl &> /dev/null; then
        error "openssl is not installed. Please install it first."
    fi

    if ! command -v base64 &> /dev/null; then
        error "base64 utility is not available."
    fi
}

# Generate a secure 256-bit random key and encode it in Base64
generate_key() {
    local key
    # Generate 32 random bytes and encode as Base64
    # Use /dev/urandom for better performance (still cryptographically secure)
    key=$(openssl rand -base64 32)
    echo "$key"
}

# Print the banner
print_banner() {
    echo "================================================================================"
    info "                   Flockr Encryption Key Generator"
    echo "================================================================================"
    echo ""
}

# Print the footer with instructions
print_footer() {
    local encryption_key="$1"
    local backend_storage_key="$2"

    echo ""
    echo "================================================================================"
    info "                            Instructions"
    echo "================================================================================"
    echo ""
    info "1. Copy the keys above to your environment configuration"
    info "2. For Docker: Add to env.docker or .env file"
    info "3. For local development: Set as environment variables"
    echo ""
    echo "   ${CYAN}export ENCRYPTION_KEY=\"${encryption_key}\"${NC}"
    echo "   ${CYAN}export BACKEND_STORAGE_ENCRYPTION_KEY=\"${backend_storage_key}\"${NC}"
    echo ""
    info "4. Share ENCRYPTION_KEY with frontend team"
    info "5. Keep BACKEND_STORAGE_ENCRYPTION_KEY secret (backend-only)"
    echo ""
    warning "SECURITY NOTES:"
    echo "   - Store keys securely (e.g., AWS Secrets Manager, HashiCorp Vault)"
    echo "   - Never commit keys to version control"
    echo "   - Rotate keys periodically for enhanced security"
    echo "   - Use different keys for different environments (dev, staging, prod)"
    echo "================================================================================"
}

# Write keys to file
write_to_file() {
    local file="$1"
    local encryption_key="$2"
    local backend_storage_key="$3"

    # Check if file exists and prompt for confirmation
    if [[ -f "$file" ]]; then
        warning "File '$file' already exists."
        read -p "Do you want to append to it? (y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            info "Operation cancelled."
            exit 0
        fi
        echo "" >> "$file"  # Add blank line before appending
    fi

    # Write keys to file
    {
        echo "# Generated encryption keys - $(date '+%Y-%m-%d %H:%M:%S')"
        echo "# Shared with frontend - Layer 1 encryption"
        echo "ENCRYPTION_KEY=\"${encryption_key}\""
        echo ""
        echo "# Backend-only - Layer 2 encryption"
        echo "BACKEND_STORAGE_ENCRYPTION_KEY=\"${backend_storage_key}\""
    } >> "$file"

    success "Keys written to: $file"
}

###############################################################################
# Main Script
###############################################################################

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --output-file)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            error "Unknown option: $1. Use -h for help."
            ;;
    esac
done

# Check requirements
check_requirements

# Print banner
print_banner

info "Generating secure 256-bit AES encryption keys..."
echo ""

# Generate keys
ENCRYPTION_KEY=$(generate_key)
BACKEND_STORAGE_KEY=$(generate_key)

# Display keys
echo "${GREEN}ENCRYPTION_KEY (shared with frontend - Layer 1):${NC}"
echo "--------------------------------------------------------------------------------"
echo "$ENCRYPTION_KEY"
echo ""

echo "${GREEN}BACKEND_STORAGE_ENCRYPTION_KEY (backend-only - Layer 2):${NC}"
echo "--------------------------------------------------------------------------------"
echo "$BACKEND_STORAGE_KEY"

# Write to file if specified
if [[ -n "$OUTPUT_FILE" ]]; then
    echo ""
    write_to_file "$OUTPUT_FILE" "$ENCRYPTION_KEY" "$BACKEND_STORAGE_KEY"
fi

# Print footer
print_footer "$ENCRYPTION_KEY" "$BACKEND_STORAGE_KEY"

success "Key generation completed successfully!"

