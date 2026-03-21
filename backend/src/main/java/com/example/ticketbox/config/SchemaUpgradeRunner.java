package com.example.ticketbox.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Runs DDL migrations at startup using a raw JDBC connection.
 * Must NOT use @Transactional — Oracle DDL triggers implicit commit
 * and will corrupt any surrounding Spring-managed transaction.
 */
@Component
@Order(0) // Before DataMigrationRunner (Order 1)
@RequiredArgsConstructor
@Slf4j
public class SchemaUpgradeRunner implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        executeDDL("ALTER TABLE USERS MODIFY PASSWORD NULL",
                   "USERS.PASSWORD → nullable (Google OAuth support)");
        executeDDL("ALTER TABLE USERS ADD PROVIDER VARCHAR2(20) DEFAULT 'LOCAL' NOT NULL",
                   "USERS.PROVIDER column added");
    }

    private void executeDDL(String sql, String description) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Schema upgrade: {}", description);
        } catch (Exception e) {
            log.debug("Schema upgrade skipped (already applied): {} — {}", description, e.getMessage());
        }
    }
}
