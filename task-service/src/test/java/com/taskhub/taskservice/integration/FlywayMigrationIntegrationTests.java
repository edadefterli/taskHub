package com.taskhub.taskservice.integration;

import com.taskhub.taskservice.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FlywayMigrationIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_createAllBaselineTables_when_flywayMigrates() {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class);

        assertThat(tables).containsExactlyInAnyOrder(
                "users", "projects", "tasks", "tags", "task_tags", "flyway_schema_history");
    }

    @Test
    void should_includeRoleColumn_on_usersTable() {
        List<String> columns = jdbcTemplate.queryForList(
                "select column_name from information_schema.columns where table_name = 'users'",
                String.class);

        assertThat(columns).contains("id", "email", "password_hash", "role");
    }

    @Test
    void should_enforceForeignKey_from_tasksToProjects() {
        Integer fkCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.table_constraints "
                        + "where table_name = 'tasks' and constraint_type = 'FOREIGN KEY'",
                Integer.class);

        assertThat(fkCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void should_enforceCompositePrimaryKey_on_taskTags() {
        Integer pkColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.key_column_usage "
                        + "where table_name = 'task_tags' "
                        + "and constraint_name = (select constraint_name from information_schema.table_constraints "
                        + "  where table_name = 'task_tags' and constraint_type = 'PRIMARY KEY')",
                Integer.class);

        assertThat(pkColumnCount).isEqualTo(2);
    }
}
