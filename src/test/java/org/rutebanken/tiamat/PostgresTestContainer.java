package org.rutebanken.tiamat;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public final class PostgresTestContainer {

    private static final String DATABASE_NAME = "tiamat_test";

    public static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:13-3.1").asCompatibleSubstituteFor("postgres"))
            .withUsername("tiamat")
            .withPassword("tiamat")
            .withReuse(true);

    static {
        INSTANCE.start();
        createDatabase();
        Flyway.configure()
                .dataSource(getJdbcUrl(), INSTANCE.getUsername(), INSTANCE.getPassword())
                .load()
                .migrate();
    }

    public static String getJdbcUrl() {
        return "jdbc:postgresql://" + INSTANCE.getHost() + ":" + INSTANCE.getMappedPort(5432) + "/" + DATABASE_NAME;
    }

    private static void createDatabase() {
        try (Connection connection = DriverManager.getConnection(INSTANCE.getJdbcUrl(), INSTANCE.getUsername(), INSTANCE.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + DATABASE_NAME);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test database on test container", e);
        }
    }

    private PostgresTestContainer() {
    }
}
