package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.assertThat;

class SensorSchemaUpgradeTest {
    @Test
    void expandsLegacyEnumPreservingExistingSensorAndAllowsMotionQueries() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:legacy-enum;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE sensors (id INT PRIMARY KEY, type ENUM('HUMIDITY','LIGHT','TEMPERATURE') NOT NULL)");
        jdbc.execute("INSERT INTO sensors VALUES (1, 'TEMPERATURE')");
        var upgrade = new SensorSchemaUpgrade(jdbc);
        upgrade.run(null);
        upgrade.run(null);
        jdbc.execute("INSERT INTO sensors VALUES (2, 'MOTION')");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sensors WHERE type = 'MOTION'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT type FROM sensors WHERE id = 1", String.class)).isEqualTo("TEMPERATURE");
    }
}
