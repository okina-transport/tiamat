package org.rutebanken.tiamat.netex.id;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Small connection pool dedicated to {@link GaplessIdGeneratorService}.
 * <p>
 * ID generation is called from inside business transactions that already hold a connection from the main pool.
 * Using the main pool for it means each importing thread needs two connections at once, and when the main pool
 * is exhausted, ID generation (and the Hazelcast entity lock it holds) blocks every other writer.
 * A dedicated pool keeps ID generation independent from the load on the main pool.
 * <p>
 * Deliberately NOT exposed as a {@link javax.sql.DataSource} bean: declaring a second DataSource bean
 * would make Spring Boot skip the auto-configuration of the main one.
 */
@Component
public class IdGeneratorConnectionPool {

    private static final Logger logger = LoggerFactory.getLogger(IdGeneratorConnectionPool.class);

    private final HikariDataSource dataSource;

    public IdGeneratorConnectionPool(DataSourceProperties dataSourceProperties,
                                     @Value("${id.generator.datasource.maximum-pool-size:3}") int maximumPoolSize,
                                     @Value("${id.generator.datasource.connection-timeout-ms:10000}") long connectionTimeoutMs,
                                     @Value("${spring.datasource.hikari.maxLifetime:600000}") long maxLifetimeMs) {
        this.dataSource = dataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        this.dataSource.setPoolName("IdGeneratorPool");
        this.dataSource.setMaximumPoolSize(maximumPoolSize);
        this.dataSource.setMinimumIdle(1);
        this.dataSource.setConnectionTimeout(connectionTimeoutMs);
        this.dataSource.setMaxLifetime(maxLifetimeMs);
        // Idle timeout must stay below maxLifetime, otherwise Hikari disables it
        this.dataSource.setIdleTimeout(maxLifetimeMs > 0 ? maxLifetimeMs / 2 : 300_000);
        logger.info("Created dedicated connection pool for ID generation with maximum pool size {}", maximumPoolSize);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @PreDestroy
    public void close() {
        dataSource.close();
    }
}
