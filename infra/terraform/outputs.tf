output "s3_bucket_name" {
  value       = aws_s3_bucket.dms_documents.id
  description = "Bucket S3 de documentos"
}

output "s3_bucket_arn" {
  value       = aws_s3_bucket.dms_documents.arn
  description = "ARN del bucket S3"
}

output "documents_table_name" {
  value       = aws_dynamodb_table.documents.name
  description = "Nombre tabla DynamoDB documentos"
}

output "documents_table_arn" {
  value       = aws_dynamodb_table.documents.arn
  description = "ARN tabla DynamoDB documentos"
}

output "audit_table_name" {
  value       = aws_dynamodb_table.audit_logs.name
  description = "Nombre tabla DynamoDB auditoría"
}

output "audit_table_arn" {
  value       = aws_dynamodb_table.audit_logs.arn
  description = "ARN tabla DynamoDB auditoría"
}

output "iam_policy_document" {
  description = "Política IAM mínima para el microservicio ms-documental-dms"
  value = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "DmsS3Access"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket",
          "s3:GetBucketLocation",
          "s3:HeadBucket"
        ]
        Resource = [
          aws_s3_bucket.dms_documents.arn,
          "${aws_s3_bucket.dms_documents.arn}/*"
        ]
      },
      {
        Sid    = "DmsDynamoDbAccess"
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:Query",
          "dynamodb:Scan",
          "dynamodb:DescribeTable"
        ]
        Resource = [
          aws_dynamodb_table.documents.arn,
          "${aws_dynamodb_table.documents.arn}/index/*",
          aws_dynamodb_table.audit_logs.arn
        ]
      }
    ]
  })
}
