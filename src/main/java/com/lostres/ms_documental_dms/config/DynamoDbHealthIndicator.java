package com.lostres.ms_documental_dms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;

@Component
@RequiredArgsConstructor
public class DynamoDbHealthIndicator implements HealthIndicator {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.documents-table:dms-documents}")
    private String documentsTable;

    @Override
    public Health health() {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder()
                    .tableName(documentsTable)
                    .build());
            return Health.up().withDetail("dynamodb", "tabla " + documentsTable + " accesible").build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("dynamodb", "tabla no accesible: " + ex.getMessage())
                    .build();
        }
    }
}
