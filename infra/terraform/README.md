# Infraestructura AWS — ms-documental-dms

Provisiona **S3** (documentos) y **DynamoDB** (metadatos + auditoría) para el microservicio.

## Recursos

| Recurso | Nombre default |
|---|---|
| S3 bucket | configurable (`s3_bucket_name`) |
| DynamoDB documents | `dms-documents` |
| DynamoDB audit | `dms-audit-logs` |

## Uso

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
# Editar s3_bucket_name (único globalmente)

terraform init
terraform plan
terraform apply
```

## Variables de entorno en EC2 / ECS (post-apply)

```bash
export SPRING_PROFILES_ACTIVE=prod
export AWS_USE_IAM=true
export AWS_BUCKET_NAME=<output s3_bucket_name>
export DYNAMODB_DOCUMENTS_TABLE=<output documents_table_name>
export DYNAMODB_AUDIT_TABLE=<output audit_table_name>
```

Asignar al rol IAM de la instancia la política JSON del output `iam_policy_document`.

## LocalStack (dev)

En desarrollo use `docker-compose up localstack` y los scripts en `localstack-init/` — no requiere Terraform.
