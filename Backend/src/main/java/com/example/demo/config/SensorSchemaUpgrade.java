package com.example.demo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** ddl-auto=update does not expand existing H2 enum columns. Preserve old readings. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SensorSchemaUpgrade implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public SensorSchemaUpgrade(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (var connection = jdbc.getDataSource().getConnection()) {
            if (!"H2".equals(connection.getMetaData().getDatabaseProductName())) return;
        }
        var types = jdbc.queryForList("SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND TABLE_NAME = 'SENSORS' AND COLUMN_NAME = 'TYPE'", String.class);
        if (types.stream().anyMatch(type -> "ENUM".equalsIgnoreCase(type))) {
            jdbc.execute("ALTER TABLE sensors ALTER COLUMN type "
                    + "ENUM('HUMIDITY','LIGHT','MOTION','TEMPERATURE') NOT NULL");
        }
    }
}
