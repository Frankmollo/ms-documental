variable "aws_region" {
  description = "Región AWS"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Entorno (dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "s3_bucket_name" {
  description = "Nombre del bucket S3 para documentos"
  type        = string
}

variable "documents_table_name" {
  description = "Tabla DynamoDB de documentos"
  type        = string
  default     = "dms-documents"
}

variable "audit_table_name" {
  description = "Tabla DynamoDB de auditoría"
  type        = string
  default     = "dms-audit-logs"
}

variable "cors_allowed_origins" {
  description = "Orígenes permitidos para CORS en S3"
  type        = list(string)
  default     = ["https://app.lostres.com"]
}

variable "enable_point_in_time_recovery" {
  description = "Habilitar PITR en tablas DynamoDB"
  type        = bool
  default     = true
}
