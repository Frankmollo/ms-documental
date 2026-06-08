package com.lostres.ms_documental_dms.repository;

import com.lostres.ms_documental_dms.model.Document;
import com.lostres.ms_documental_dms.model.DocumentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentRepository {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.documents-table:dms-documents}")
    private String tableName;

    public Document save(Document document) {
        if (document.getId() == null) {
            document.setId(UUID.randomUUID());
        }
        if (document.getUploadedAt() == null) {
            document.setUploadedAt(LocalDateTime.now());
        }
        if (document.getStatus() == null) {
            document.setStatus(DocumentStatus.PENDING);
        }

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(DynamoDbMapper.documentToItem(document))
                .build());
        return document;
    }

    public void saveAll(List<Document> documents) {
        documents.forEach(this::save);
    }

    public Optional<Document> findById(UUID id) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("id", AttributeValue.builder().s(id.toString()).build()))
                .build());
        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(DynamoDbMapper.itemToDocument(response.item()));
    }

    public Optional<Document> findByS3KeyAndDeletedAtIsNull(String s3Key) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(DynamoDbMapper.GSI_S3_KEY)
                .keyConditionExpression("s3Key = :s3Key")
                .expressionAttributeValues(Map.of(
                        ":s3Key", AttributeValue.builder().s(s3Key).build()
                ))
                .build());

        return response.items().stream()
                .map(DynamoDbMapper::itemToDocument)
                .filter(doc -> doc.getDeletedAt() == null)
                .findFirst();
    }

    public Page<Document> findByDeletedAtIsNullAndStatus(DocumentStatus status, Pageable pageable) {
        return queryByStatus(status, null, null, pageable);
    }

    public Page<Document> findByDeletedAtIsNullAndMeterIdContainingIgnoreCaseAndStatus(
            String meterId, DocumentStatus status, Pageable pageable) {
        return queryByStatus(status, meterId, null, pageable);
    }

    public Page<Document> findByDeletedAtIsNullAndTecnicoAsignadoContainingIgnoreCaseAndStatus(
            String tecnicoAsignado, DocumentStatus status, Pageable pageable) {
        return queryByStatus(status, null, tecnicoAsignado, pageable);
    }

    public Page<Document> findByDeletedAtIsNullAndMeterIdContainingIgnoreCaseAndTecnicoAsignadoContainingIgnoreCaseAndStatus(
            String meterId, String tecnicoAsignado, DocumentStatus status, Pageable pageable) {
        return queryByStatus(status, meterId, tecnicoAsignado, pageable);
    }

    public List<Document> findByDeletedAtIsNullAndStatusAndUploadedAtBefore(
            DocumentStatus status, LocalDateTime before) {
        String cutoff = DynamoDbMapper.formatDateTime(before);

        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(DynamoDbMapper.GSI_STATUS_UPLOADED)
                .keyConditionExpression("#status = :status AND uploadedAt < :cutoff")
                .expressionAttributeNames(Map.of("#status", "status"))
                .expressionAttributeValues(Map.of(
                        ":status", AttributeValue.builder().s(status.name()).build(),
                        ":cutoff", AttributeValue.builder().s(cutoff).build()
                ))
                .build());

        List<Document> results = new ArrayList<>();
        for (Map<String, AttributeValue> item : response.items()) {
            Document doc = DynamoDbMapper.itemToDocument(item);
            if (doc.getDeletedAt() == null) {
                results.add(doc);
            }
        }
        return results;
    }

    private Page<Document> queryByStatus(
            DocumentStatus status, String meterIdFilter, String tecnicoFilter, Pageable pageable) {
        List<Document> matched = new ArrayList<>();
        Map<String, AttributeValue> lastKey = null;

        do {
            QueryRequest.Builder builder = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName(DynamoDbMapper.GSI_STATUS_UPLOADED)
                    .keyConditionExpression("#status = :status")
                    .expressionAttributeNames(Map.of("#status", "status"))
                    .expressionAttributeValues(Map.of(
                            ":status", AttributeValue.builder().s(status.name()).build()
                    ));
            if (lastKey != null) {
                builder.exclusiveStartKey(lastKey);
            }

            var response = dynamoDbClient.query(builder.build());
            for (Map<String, AttributeValue> item : response.items()) {
                Document doc = DynamoDbMapper.itemToDocument(item);
                if (doc.getDeletedAt() != null) {
                    continue;
                }
                if (meterIdFilter != null && !containsIgnoreCase(doc.getMeterId(), meterIdFilter)) {
                    continue;
                }
                if (tecnicoFilter != null && !containsIgnoreCase(doc.getTecnicoAsignado(), tecnicoFilter)) {
                    continue;
                }
                matched.add(doc);
            }
            lastKey = response.hasLastEvaluatedKey() ? response.lastEvaluatedKey() : null;
        } while (lastKey != null && !lastKey.isEmpty());

        matched.sort(Comparator.comparing(Document::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), matched.size());
        List<Document> pageContent = start >= matched.size() ? List.of() : matched.subList(start, end);
        return new PageImpl<>(pageContent, pageable, matched.size());
    }

    private boolean containsIgnoreCase(String value, String filter) {
        if (value == null) {
            return false;
        }
        return value.toLowerCase().contains(filter.toLowerCase());
    }
}
