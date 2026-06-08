#!/bin/bash
echo "Inicializando tablas DynamoDB..."

awslocal dynamodb create-table \
  --table-name dms-documents \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
    AttributeName=s3Key,AttributeType=S \
    AttributeName=status,AttributeType=S \
    AttributeName=uploadedAt,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --global-secondary-indexes \
    '[
      {
        "IndexName": "s3-key-index",
        "KeySchema": [{"AttributeName": "s3Key", "KeyType": "HASH"}],
        "Projection": {"ProjectionType": "ALL"}
      },
      {
        "IndexName": "status-uploaded-index",
        "KeySchema": [
          {"AttributeName": "status", "KeyType": "HASH"},
          {"AttributeName": "uploadedAt", "KeyType": "RANGE"}
        ],
        "Projection": {"ProjectionType": "ALL"}
      }
    ]' \
  --billing-mode PAY_PER_REQUEST \
  2>/dev/null || true

awslocal dynamodb create-table \
  --table-name dms-audit-logs \
  --attribute-definitions \
    AttributeName=documentId,AttributeType=S \
    AttributeName=sortKey,AttributeType=S \
  --key-schema \
    AttributeName=documentId,KeyType=HASH \
    AttributeName=sortKey,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  2>/dev/null || true

echo "Tablas DynamoDB listas: dms-documents, dms-audit-logs."
