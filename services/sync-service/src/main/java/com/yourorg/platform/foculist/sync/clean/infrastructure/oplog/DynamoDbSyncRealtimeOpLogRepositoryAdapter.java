package com.yourorg.platform.foculist.sync.clean.infrastructure.oplog;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncRealtimeOpLogEntry;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncRealtimeOpLogRepositoryPort;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class DynamoDbSyncRealtimeOpLogRepositoryAdapter implements SyncRealtimeOpLogRepositoryPort {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbSyncRealtimeOpLogRepositoryAdapter(
            DynamoDbClient dynamoDbClient,
            @Value("${app.sync.op-log.table:foculist_sync_op_log}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public void append(SyncRealtimeOpLogEntry entry) {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(java.util.Map.of(
                        "pk", AttributeValue.fromS(entry.tenantId() + "#" + entry.projectId()),
                        "sk",
                        AttributeValue.fromS(entry.occurredAt().truncatedTo(ChronoUnit.MILLIS) + "#" + entry.id()),
                        "tenantId", AttributeValue.fromS(entry.tenantId()),
                        "projectId", AttributeValue.fromS(entry.projectId()),
                        "deviceId", AttributeValue.fromS(entry.deviceId()),
                        "destination", AttributeValue.fromS(entry.destination()),
                        "payload", AttributeValue.fromS(entry.payload()),
                        "occurredAt", AttributeValue.fromS(entry.occurredAt().toString()),
                        "expiresAtEpoch", AttributeValue.fromN(String.valueOf(entry.expiresAt().getEpochSecond()))))
                .build();
        dynamoDbClient.putItem(request);
    }
}
