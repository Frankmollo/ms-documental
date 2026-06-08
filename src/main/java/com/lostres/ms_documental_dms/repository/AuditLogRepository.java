package com.lostres.ms_documental_dms.repository;

import com.lostres.ms_documental_dms.model.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuditLogRepository {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.audit-table:dms-audit-logs}")
    private String tableName;

    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            auditLog.setId(UUID.randomUUID());
        }
        if (auditLog.getTimestamp() == null) {
            auditLog.setTimestamp(LocalDateTime.now());
        }

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(DynamoDbMapper.auditToItem(auditLog))
                .build());
        return auditLog;
    }

    public Page<AuditLog> findByDocumentId(UUID documentId, Pageable pageable) {
        List<AuditLog> logs = new ArrayList<>();
        Map<String, AttributeValue> lastKey = null;

        do {
            QueryRequest.Builder builder = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("documentId = :documentId")
                    .expressionAttributeValues(Map.of(
                            ":documentId", AttributeValue.builder().s(documentId.toString()).build()
                    ));
            if (lastKey != null) {
                builder.exclusiveStartKey(lastKey);
            }

            var response = dynamoDbClient.query(builder.build());
            response.items().forEach(item -> logs.add(DynamoDbMapper.itemToAudit(item)));
            lastKey = response.hasLastEvaluatedKey() ? response.lastEvaluatedKey() : null;
        } while (lastKey != null && !lastKey.isEmpty());

        logs.sort(Comparator.comparing(AuditLog::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), logs.size());
        List<AuditLog> pageContent = start >= logs.size() ? List.of() : logs.subList(start, end);
        return new PageImpl<>(pageContent, pageable, logs.size());
    }
}
