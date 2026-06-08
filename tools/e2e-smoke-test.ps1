# Smoke test E2E: ms-documental-dms (DynamoDB + S3 via LocalStack)
# Uso: .\tools\e2e-smoke-test.ps1 [-BaseUrl "http://localhost:8080"]

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$headers = @{
    "X-User-Id" = "e2e-test-user"
    "Content-Type" = "application/json"
}

function Assert-Ok($response, $step) {
    if (-not $response) {
        throw "Paso fallido: $step"
    }
    Write-Host "  OK: $step" -ForegroundColor Green
}

Write-Host "`n=== E2E Smoke Test DMS ===" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl`n"

# 1. Health
Write-Host "[1/7] Health check..."
$health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get
if ($health.status -ne "UP") { throw "Health no UP: $($health | ConvertTo-Json -Compress)" }
Assert-Ok $true "actuator/health UP"

# 2. Upload URL
Write-Host "[2/7] Solicitar upload-url..."
$uploadUri = '{0}/api/documents/upload-url?fileName=test-e2e.pdf&contentType=application/pdf&meterId=M-E2E-001' -f $BaseUrl
$uploadUrlResp = Invoke-RestMethod -Uri $uploadUri `
    -Headers @{ "X-User-Id" = "e2e-test-user" } -Method Get
Assert-Ok $uploadUrlResp.s3Key "upload-url (documentId=$($uploadUrlResp.documentId))"

# 3. PUT a S3 (LocalStack)
Write-Host "[3/7] Subir archivo a S3..."
$body = "%PDF-1.4`ne2e dynamodb test $(Get-Date -Format o)"
Invoke-RestMethod -Uri $uploadUrlResp.url -Method Put -Body $body -ContentType "application/pdf" | Out-Null
Assert-Ok $true "PUT S3 pre-firmado"

# 4. Confirm upload
Write-Host "[4/7] Confirmar subida..."
$confirmBody = @{
    fileName = "test-e2e.pdf"
    s3Key = $uploadUrlResp.s3Key
    meterId = "M-E2E-001"
    contentType = "application/pdf"
    sizeBytes = [System.Text.Encoding]::UTF8.GetByteCount($body)
} | ConvertTo-Json

$doc = Invoke-RestMethod -Uri "$BaseUrl/api/documents/confirm-upload" -Method Post `
    -Headers $headers -Body $confirmBody
if ($doc.status -ne "AVAILABLE") { throw "Estado esperado AVAILABLE, got $($doc.status)" }
Assert-Ok $true "confirm-upload → AVAILABLE"

$docId = $doc.id

# 5. List documents
Write-Host "[5/7] Listar documentos..."
$listUri = '{0}/api/documents?page=0&size=10&status=AVAILABLE&meterId=M-E2E' -f $BaseUrl
$list = Invoke-RestMethod -Uri $listUri `
    -Headers @{ "X-User-Id" = "e2e-test-user" } -Method Get
$found = $list.content | Where-Object { $_.id -eq $docId }
if (-not $found) { throw "Documento no encontrado en listado" }
Assert-Ok $true "list documents (total=$($list.totalElements))"

# 6. Get by ID + download URL
Write-Host "[6/7] Consultar documento y URL de descarga..."
$byId = Invoke-RestMethod -Uri "$BaseUrl/api/documents/$docId" `
    -Headers @{ "X-User-Id" = "e2e-test-user" } -Method Get
$dl = Invoke-RestMethod -Uri "$BaseUrl/api/documents/$docId/download-url" `
    -Headers @{ "X-User-Id" = "e2e-test-user" } -Method Get
Assert-Ok $dl.url "download-url generada"

# 7. Audit logs
Write-Host "[7/7] Consultar auditoría..."
Start-Sleep -Seconds 2
$auditUri = '{0}/api/audit/documents/{1}?page=0&size=10' -f $BaseUrl, $docId
$audit = Invoke-RestMethod -Uri $auditUri -Method Get
if ($audit.totalElements -lt 1) { throw "Sin registros de auditoría" }
Assert-Ok $true "audit logs (total=$($audit.totalElements))"

Write-Host "`n=== E2E COMPLETADO ===" -ForegroundColor Green
Write-Host "Document ID: $docId"
Write-Host "Frontend proxy: http://localhost:4200 → /api/dms → $BaseUrl/api"
Write-Host "Levantar Angular: cd ../frontendWeb && npm start`n"
