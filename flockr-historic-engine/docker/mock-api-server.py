#!/usr/bin/env python3
"""
Mock API Server for Testing Flocker Historic Engine
Receives cohort update requests and logs them
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import json
import sys
from datetime import datetime

app = Flask(__name__)
CORS(app)

# Store received data
received_data = []
request_count = 0

@app.route('/flockr/users/map-cohorts', methods=['POST'])
def receive_cohorts():
    global received_data, request_count
    try:
        data = request.get_json()
        headers = dict(request.headers)
        
        # Log request
        request_count += 1
        record_count = len(data) if isinstance(data, list) else 1
        received_data.extend(data if isinstance(data, list) else [data])
        
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(f"[{timestamp}] Request #{request_count}: Received {record_count} record(s)")
        print(f"  Total records stored: {len(received_data)}")
        print(f"  Headers: {headers.get('x-project-key', 'N/A')}")
        
        if record_count <= 5:
            print(f"  Sample data: {json.dumps(data[:5] if isinstance(data, list) else [data], indent=2)}")
        
        return jsonify({
            'status': 'success',
            'received': record_count,
            'total': len(received_data),
            'timestamp': timestamp
        }), 200
    except Exception as e:
        print(f"Error processing request: {e}", file=sys.stderr)
        return jsonify({'status': 'error', 'message': str(e)}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'healthy',
        'total_requests': request_count,
        'total_records': len(received_data)
    }), 200

@app.route('/received-data', methods=['GET'])
def get_data():
    limit = request.args.get('limit', default=100, type=int)
    return jsonify({
        'count': len(received_data),
        'total_requests': request_count,
        'data': received_data[:limit]
    }), 200

@app.route('/clear-data', methods=['POST'])
def clear_data():
    global received_data, request_count
    count = len(received_data)
    received_data = []
    request_count = 0
    return jsonify({
        'status': 'success',
        'cleared': count
    }), 200

@app.route('/', methods=['GET'])
def index():
    return jsonify({
        'service': 'Mock API Server for Flocker Historic Engine',
        'endpoints': {
            'POST /flockr/users/map-cohorts': 'Receive cohort updates',
            'GET /health': 'Health check',
            'GET /received-data': 'Get all received data',
            'POST /clear-data': 'Clear stored data'
        },
        'stats': {
            'total_requests': request_count,
            'total_records': len(received_data)
        }
    }), 200

if __name__ == '__main__':
    print("Starting Mock API Server on port 8080...")
    print("Endpoints:")
    print("  POST http://localhost:8080/flockr/users/map-cohorts")
    print("  GET  http://localhost:8080/health")
    print("  GET  http://localhost:8080/received-data")
    print("  POST http://localhost:8080/clear-data")
    app.run(host='0.0.0.0', port=8080, debug=True)

