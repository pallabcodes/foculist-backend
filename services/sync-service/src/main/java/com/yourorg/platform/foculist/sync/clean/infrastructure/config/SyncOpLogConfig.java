package com.yourorg.platform.foculist.sync.clean.infrastructure.config;

import com.yourorg.platform.foculist.sync.clean.domain.port.SyncRealtimeOpLogRepositoryPort;
import com.yourorg.platform.foculist.sync.clean.infrastructure.oplog.DynamoDbSyncRealtimeOpLogRepositoryAdapter;
import com.yourorg.platform.foculist.sync.clean.infrastructure.oplog.NoopSyncRealtimeOpLogRepositoryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class SyncOpLogConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.sync.op-log", name = "mode", havingValue = "dynamodb")
    public SyncRealtimeOpLogRepositoryPort dynamoDbOpLog(
            DynamoDbClient dynamoDbClient,
            @Value("${app.sync.op-log.table:foculist_sync_op_log}") String tableName
    ) {
        return new DynamoDbSyncRealtimeOpLogRepositoryAdapter(dynamoDbClient, tableName);
    }

    @Bean
    @ConditionalOnMissingBean(SyncRealtimeOpLogRepositoryPort.class)
    public SyncRealtimeOpLogRepositoryPort noopOpLog() {
        return new NoopSyncRealtimeOpLogRepositoryAdapter();
    }
}
