# ms-documental-dms (Microservicio de Gestión Documental)

Microservicio **Spring Boot 4** / **Java 17** para gestionar subida, descarga y metadatos de documentos usando **Amazon S3** (pre-signed URLs) y **Amazon DynamoDB**. Implementa los casos de uso **CU-15 a CU-18** del informe tecnológico de Compañía Eléctrica LOS TRES.

## Casos de uso implementados

| CU | Descripción | Endpoint(s) |
|---|---|---|
| **CU-15** | Listar documentos almacenados | `GET /api/documents` |
| **CU-15** | Consultar documento por ID | `GET /api/documents/{id}` |
| **CU-16** | URL de descarga temporal S3 | `GET /api/documents/{id}/download-url` |
| **CU-17** | Auditoría acceso, carga y edición | `GET /api/audit/documents/{documentId}` |
| **CU-17** | Editar metadatos | `PATCH /api/documents/{id}` |
| **CU-18** | Subida móvil por medidor | `GET /upload-url?meterId=` + `POST /confirm-upload` |

### Acciones de auditoría registradas

`UPLOAD_REQUEST`, `UPLOAD_CONFIRMED`, `DOWNLOAD`, `VIEW`, `EDIT`, `DELETE`

## Características

- URLs pre-firmadas para subida/descarga directa en S3
- Validación de existencia del archivo en S3 al confirmar subida
- Spring Security con API key (`X-Api-Key`) — obligatorio en perfil `prod`
- Soporte **AWS IAM roles** (`AWS_USE_IAM=true`) para despliegue en EC2
- `meterId` obligatorio en producción (CU-18 operación móvil)
- Tipos: PDF, Word, Excel, imágenes, planos DWG
- Rate limiting, health checks (DynamoDB + S3), Docker

## Ejecución

```bash
# Stack completo (LocalStack S3 + DynamoDB + app)
docker-compose up -d --build

# Solo infraestructura + app local
docker-compose up -d localstack
./mvnw spring-boot:run
```

### Paso 1 — Smoke test E2E (API)

```powershell
.\tools\e2e-smoke-test.ps1
```

### Paso 1 — Frontend Angular

```bash
# Terminal 1: DMS + LocalStack
docker-compose up -d localstack
./mvnw spring-boot:run

# Terminal 2: Angular (proxy /api/dms → :8080)
cd ../frontendWeb
npm start
# http://localhost:4200/dashboard/documentos
```

### Paso 2 — Migrar datos PostgreSQL → DynamoDB

Si tenía datos en el Postgres legacy (`dms-postgres`):

```powershell
.\tools\migrate-pg-to-dynamodb.ps1 -DryRun   # preview
.\tools\migrate-pg-to-dynamodb.ps1           # ejecutar
```

### Paso 3 — Infraestructura AWS (Terraform)

Ver [`infra/terraform/README.md`](infra/terraform/README.md).

Swagger: `http://localhost:8080/swagger-ui.html`

## Variables de entorno

Ver [`.env.example`](.env.example). Principales:

| Variable | Dev | Prod |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` |
| `SECURITY_ENABLED` | `false` | `true` |
| `ENFORCE_API_KEY` | `false` | `true` |
| `AWS_USE_IAM` | `false` | `true` |
| `REQUIRE_METER_ID` | `false` | `true` |
| `AWS_ENDPOINT_URL` | `http://localhost:4566` | (vacío) |

## Endpoints

| Método | Ruta | CU |
|---|---|---|
| `GET` | `/api/documents/upload-url` | CU-18 |
| `POST` | `/api/documents/confirm-upload` | CU-18 |
| `GET` | `/api/documents` | CU-15 |
| `GET` | `/api/documents/{id}` | CU-15 / CU-17 (VIEW) |
| `PATCH` | `/api/documents/{id}` | CU-17 (EDIT) |
| `GET` | `/api/documents/{id}/download-url` | CU-16 |
| `DELETE` | `/api/documents/{id}` | — |
| `GET` | `/api/audit/documents/{documentId}` | CU-17 |
| `GET` | `/actuator/health` | — |

### Headers

| Header | Uso |
|---|---|
| `X-User-Id` | Usuario (obligatorio en operaciones protegidas) |
| `X-Forwarded-For` | IP para auditoría |
| `X-Api-Key` | Obligatorio en prod |

## Flujo de subida (CU-18)

1. `GET /api/documents/upload-url?fileName=foto.jpg&contentType=image/jpeg&meterId=M-001`
2. `PUT` del archivo a la URL pre-firmada
3. `POST /api/documents/confirm-upload`:

```json
{
  "fileName": "foto.jpg",
  "s3Key": "<s3Key>",
  "meterId": "M-001",
  "contentType": "image/jpeg",
  "sizeBytes": 12345
}
```

## Edición de metadatos (CU-17)

```json
PATCH /api/documents/{id}
{
  "fileName": "contrato_revisado.pdf",
  "meterId": "M-002"
}
```

## Despliegue AWS (EC2 + S3 + DynamoDB + IAM)

En EC2 con rol IAM asignado al bucket S3 y tablas DynamoDB:

```bash
export SPRING_PROFILES_ACTIVE=prod
export AWS_USE_IAM=true
export SERVICE_API_KEY=<clave-segura>
export DYNAMODB_DOCUMENTS_TABLE=dms-documents
export DYNAMODB_AUDIT_TABLE=dms-audit-logs
# Sin AWS_ACCESS_KEY ni AWS_SECRET_KEY
```

## Códigos de error

| HTTP | Situación |
|---|---|
| 400 | Validación, MIME, tamaño, meterId faltante |
| 401 | API key inválida |
| 404 | Documento no encontrado |
| 409 | Estado inválido |
| 429 | Rate limit |
