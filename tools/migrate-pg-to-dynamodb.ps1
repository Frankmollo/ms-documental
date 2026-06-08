# Migracion PostgreSQL -> DynamoDB (ms-documental-dms)
# Requiere contenedor dms-postgres (legacy) y LocalStack con tablas DynamoDB.
#
# Uso:
#   .\tools\migrate-pg-to-dynamodb.ps1
#   .\tools\migrate-pg-to-dynamodb.ps1 -DryRun

param(
    [string]$PgContainer = "dms-postgres",
    [string]$PgDb = "dms_db",
    [string]$PgUser = "postgres",
    [string]$LocalstackContainer = "dms-localstack",
    [string]$DocumentsTable = "dms-documents",
    [string]$AuditTable = "dms-audit-logs",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Format-DynamoDate([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value)) { return $null }
    return ([datetime]$value).ToString("yyyy-MM-dd'T'HH:mm:ss.fff")
}

function Put-ItemJson([string]$Table, [string]$Json) {
    if ($DryRun) {
        Write-Host "[DRY-RUN] $Table"
        return
    }
    docker exec $LocalstackContainer awslocal dynamodb put-item --table-name $Table --item $Json | Out-Null
}

Write-Host ""
Write-Host "=== Migracion PostgreSQL -> DynamoDB ===" -ForegroundColor Cyan

$pgRunning = docker ps --filter "name=$PgContainer" --format "{{.Names}}"
if (-not $pgRunning) {
    throw "Contenedor '$PgContainer' no esta en ejecucion."
}

$docRows = docker exec $PgContainer psql -U $PgUser -d $PgDb -t -A -F "`t" -c `
    "SELECT id, file_name, s3_key, COALESCE(meter_id,''), COALESCE(tecnico_asignado,''), status, COALESCE(content_type,''), COALESCE(size_bytes::text,''), COALESCE(uploaded_by,''), uploaded_at, COALESCE(deleted_at::text,'') FROM documents;"

$auditRows = docker exec $PgContainer psql -U $PgUser -d $PgDb -t -A -F "`t" -c `
    "SELECT id, document_id, action, COALESCE(performed_by,''), timestamp, COALESCE(ip_address,'') FROM audit_logs;"

$docCount = 0
foreach ($line in ($docRows -split "`n")) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $c = $line -split "`t"
    if ($c.Count -lt 11) { continue }

    $uploadedAt = Format-DynamoDate $c[9]
    $item = @{
        id         = @{ S = $c[0].Trim() }
        fileName   = @{ S = $c[1] }
        s3Key      = @{ S = $c[2] }
        status     = @{ S = $c[5] }
        uploadedAt = @{ S = $uploadedAt }
    }
    if ($c[3]) { $item.meterId = @{ S = $c[3] } }
    if ($c[4]) { $item.tecnicoAsignado = @{ S = $c[4] } }
    if ($c[6]) { $item.contentType = @{ S = $c[6] } }
    if ($c[7]) { $item.sizeBytes = @{ N = $c[7] } }
    if ($c[8]) { $item.uploadedBy = @{ S = $c[8] } }
    if ($c[10]) { $item.deletedAt = @{ S = (Format-DynamoDate $c[10]) } }

    Put-ItemJson $DocumentsTable ($item | ConvertTo-Json -Compress -Depth 5)
    $docCount++
}

$auditCount = 0
foreach ($line in ($auditRows -split "`n")) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $c = $line -split "`t"
    if ($c.Count -lt 6) { continue }

    $ts = Format-DynamoDate $c[4]
    $item = @{
        documentId = @{ S = $c[1].Trim() }
        sortKey    = @{ S = "$ts#$($c[0].Trim())" }
        id         = @{ S = $c[0].Trim() }
        action     = @{ S = $c[2] }
        timestamp  = @{ S = $ts }
    }
    if ($c[3]) { $item.performedBy = @{ S = $c[3] } }
    if ($c[5]) { $item.ipAddress = @{ S = $c[5] } }

    Put-ItemJson $AuditTable ($item | ConvertTo-Json -Compress -Depth 5)
    $auditCount++
}

Write-Host "Documentos migrados: $docCount" -ForegroundColor Green
Write-Host "Auditoria migrada:   $auditCount" -ForegroundColor Green
if ($DryRun) { Write-Host "Modo dry-run: no se escribio en DynamoDB." -ForegroundColor Yellow }
