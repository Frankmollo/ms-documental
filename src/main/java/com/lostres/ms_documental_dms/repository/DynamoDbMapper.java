package com.lostres.ms_documental_dms.repository;

import com.lostres.ms_documental_dms.model.AuditAction;
import com.lostres.ms_documental_dms.model.AuditLog;
import com.lostres.ms_documental_dms.model.Document;
import com.lostres.ms_documental_dms.model.DocumentStatus;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class DynamoDbMapper {

    static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    static final String GSI_S3_KEY = "s3-key-index";
    static final String GSI_STATUS_UPLOADED = "status-uploaded-index";

    private DynamoDbMapper() {
    }

    static Map<String, AttributeValue> documentToItem(Document doc) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(doc.getId().toString()).build());
        item.put("fileName", AttributeValue.builder().s(doc.getFileName()).build());
        item.put("s3Key", AttributeValue.builder().s(doc.getS3Key()).build());
        putOptionalString(item, "meterId", doc.getMeterId());
        putOptionalString(item, "tecnicoAsignado", doc.getTecnicoAsignado());
        item.put("status", AttributeValue.builder().s(doc.getStatus().name()).build());
        putOptionalString(item, "contentType", doc.getContentType());
        if (doc.getSizeBytes() != null) {
            item.put("sizeBytes", AttributeValue.builder().n(doc.getSizeBytes().toString()).build());
        }
        putOptionalString(item, "uploadedBy", doc.getUploadedBy());
        item.put("uploadedAt", AttributeValue.builder().s(formatDateTime(doc.getUploadedAt())).build());
        if (doc.getDeletedAt() != null) {
            item.put("deletedAt", AttributeValue.builder().s(formatDateTime(doc.getDeletedAt())).build());
        }
        return item;
    }

    static Document itemToDocument(Map<String, AttributeValue> item) {
        return Document.builder()
                .id(UUID.fromString(item.get("id").s()))
                .fileName(item.get("fileName").s())
                .s3Key(item.get("s3Key").s())
                .meterId(getOptionalString(item, "meterId"))
                .tecnicoAsignado(getOptionalString(item, "tecnicoAsignado"))
                .status(DocumentStatus.valueOf(item.get("status").s()))
                .contentType(getOptionalString(item, "contentType"))
                .sizeBytes(getOptionalLong(item, "sizeBytes"))
                .uploadedBy(getOptionalString(item, "uploadedBy"))
                .uploadedAt(parseDateTime(item.get("uploadedAt").s()))
                .deletedAt(getOptionalDateTime(item, "deletedAt"))
                .build();
    }

    static Map<String, AttributeValue> auditToItem(AuditLog log) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("documentId", AttributeValue.builder().s(log.getDocumentId().toString()).build());
        item.put("sortKey", AttributeValue.builder().s(buildAuditSortKey(log)).build());
        item.put("id", AttributeValue.builder().s(log.getId().toString()).build());
        item.put("action", AttributeValue.builder().s(log.getAction().name()).build());
        putOptionalString(item, "performedBy", log.getPerformedBy());
        item.put("timestamp", AttributeValue.builder().s(formatDateTime(log.getTimestamp())).build());
        putOptionalString(item, "ipAddress", log.getIpAddress());
        return item;
    }

    static AuditLog itemToAudit(Map<String, AttributeValue> item) {
        return AuditLog.builder()
                .id(UUID.fromString(item.get("id").s()))
                .documentId(UUID.fromString(item.get("documentId").s()))
                .action(AuditAction.valueOf(item.get("action").s()))
                .performedBy(getOptionalString(item, "performedBy"))
                .timestamp(parseDateTime(item.get("timestamp").s()))
                .ipAddress(getOptionalString(item, "ipAddress"))
                .build();
    }

    static String buildAuditSortKey(AuditLog log) {
        return formatDateTime(log.getTimestamp()) + "#" + log.getId();
    }

    static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(ISO);
    }

    static LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value, ISO);
    }

    private static void putOptionalString(Map<String, AttributeValue> item, String key, String value) {
        if (value != null && !value.isBlank()) {
            item.put(key, AttributeValue.builder().s(value).build());
        }
    }

    private static String getOptionalString(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return value != null ? value.s() : null;
    }

    private static Long getOptionalLong(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return value != null ? Long.parseLong(value.n()) : null;
    }

    private static LocalDateTime getOptionalDateTime(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return value != null ? parseDateTime(value.s()) : null;
    }
}
