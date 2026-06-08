#!/bin/bash
echo "Inicializando LocalStack S3..."
awslocal s3 mb s3://lostres-dms-bucket 2>/dev/null || true

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
awslocal s3api put-bucket-cors \
  --bucket lostres-dms-bucket \
  --cors-configuration "file://${SCRIPT_DIR}/cors.json"

echo "Bucket 'lostres-dms-bucket' listo con CORS."
