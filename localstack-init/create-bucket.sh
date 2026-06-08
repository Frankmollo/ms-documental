#!/bin/bash
awslocal s3 mb s3://lostres-dms-bucket 2>/dev/null || true

awslocal s3api put-bucket-cors --bucket lostres-dms-bucket --cors-configuration '{
  "CORSRules": [{
    "AllowedOrigins": ["http://localhost:4200", "http://127.0.0.1:4200"],
    "AllowedMethods": ["GET", "PUT", "POST", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }]
}' 2>/dev/null || true
