#!/usr/bin/env python3
"""
Mock API Server for testing Flocker Historic Engine.

This server simulates the audience update API endpoint and logs all requests.
It accepts POST requests to /api/audience/update with JSON payloads.
"""

import json
import logging
import os
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse

# Configure logging
log_dir = os.path.join(os.path.dirname(__file__), 'api-logs')
os.makedirs(log_dir, exist_ok=True)

log_file = os.path.join(log_dir, f'api-{datetime.now().strftime("%Y%m%d-%H%M%S")}.log')
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(log_file),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger(__name__)


class MockAPIHandler(BaseHTTPRequestHandler):
    """HTTP request handler for mock API server."""

    def do_GET(self):
        """Handle GET requests (health check)."""
        if self.path == '/health':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            response = {'status': 'healthy', 'service': 'mock-api-server'}
            self.wfile.write(json.dumps(response).encode())
            logger.info("Health check requested")
        else:
            self.send_response(404)
            self.end_headers()
            logger.warning(f"GET request to unknown path: {self.path}")

    def do_POST(self):
        """Handle POST requests to /api/audience/update."""
        if self.path == '/api/audience/update':
            try:
                content_length = int(self.headers.get('Content-Length', 0))
                body = self.rfile.read(content_length)
                
                # Parse JSON payload
                try:
                    payload = json.loads(body.decode('utf-8'))
                except json.JSONDecodeError as e:
                    logger.error(f"Invalid JSON: {e}")
                    self.send_response(400)
                    self.send_header('Content-Type', 'application/json')
                    self.end_headers()
                    error_response = {'error': 'Invalid JSON', 'message': str(e)}
                    self.wfile.write(json.dumps(error_response).encode())
                    return

                # Log the request
                logger.info(f"Received audience update request:")
                logger.info(f"  Headers: {dict(self.headers)}")
                logger.info(f"  Payload: {json.dumps(payload, indent=2)}")
                
                # Count requests
                if isinstance(payload, list):
                    request_count = len(payload)
                    logger.info(f"  Total requests in batch: {request_count}")
                else:
                    request_count = 1
                    logger.info(f"  Single request received")

                # Simulate successful response
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                response = {
                    'status': 'success',
                    'message': 'Audience update processed',
                    'requests_processed': request_count,
                    'timestamp': datetime.now().isoformat()
                }
                self.wfile.write(json.dumps(response).encode())
                logger.info(f"Sent success response for {request_count} request(s)")

            except Exception as e:
                logger.error(f"Error processing request: {e}", exc_info=True)
                self.send_response(500)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                error_response = {'error': 'Internal server error', 'message': str(e)}
                self.wfile.write(json.dumps(error_response).encode())
        else:
            self.send_response(404)
            self.end_headers()
            logger.warning(f"POST request to unknown path: {self.path}")

    def log_message(self, format, *args):
        """Override to use our logger instead of default."""
        logger.debug(f"{self.address_string()} - {format % args}")


def run_server(port=8080):
    """Start the mock API server."""
    server_address = ('', port)
    httpd = HTTPServer(server_address, MockAPIHandler)
    logger.info(f"Mock API Server starting on port {port}")
    logger.info(f"Health check: http://localhost:{port}/health")
    logger.info(f"Audience update endpoint: http://localhost:{port}/api/audience/update")
    logger.info(f"Logs will be written to: {log_file}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logger.info("Shutting down server...")
        httpd.shutdown()


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 8080))
    run_server(port)

