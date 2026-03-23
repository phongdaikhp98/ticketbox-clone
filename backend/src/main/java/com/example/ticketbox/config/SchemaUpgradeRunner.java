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

        // Feature: Event Reminder Email
        executeDDL("CREATE TABLE REMINDER_LOGS (ID NUMBER NOT NULL, TICKET_ID NUMBER NOT NULL, EVENT_ID NUMBER NOT NULL, SENT_AT TIMESTAMP NOT NULL, CONSTRAINT PK_REMINDER_LOGS PRIMARY KEY (ID), CONSTRAINT FK_RL_TICKET FOREIGN KEY (TICKET_ID) REFERENCES TICKETS(ID), CONSTRAINT FK_RL_EVENT FOREIGN KEY (EVENT_ID) REFERENCES EVENTS(ID), CONSTRAINT UQ_RL_TICKET UNIQUE (TICKET_ID))",
                   "REMINDER_LOGS table");
        executeDDL("CREATE SEQUENCE REMINDER_LOG_SEQ START WITH 1 INCREMENT BY 1 NOCACHE",
                   "REMINDER_LOG_SEQ sequence");

        // Feature: Organizer Approval Flow
        executeDDL("CREATE TABLE ORGANIZER_APPLICATIONS (ID NUMBER NOT NULL, USER_ID NUMBER NOT NULL, ORG_NAME VARCHAR2(255) NOT NULL, TAX_NUMBER VARCHAR2(20) NOT NULL, CONTACT_PHONE VARCHAR2(20) NOT NULL, REASON CLOB, STATUS VARCHAR2(20) DEFAULT 'PENDING' NOT NULL, REVIEWED_BY NUMBER, REVIEW_NOTE VARCHAR2(1000), SUBMITTED_AT TIMESTAMP NOT NULL, REVIEWED_AT TIMESTAMP, CREATED_DATE TIMESTAMP NOT NULL, UPDATED_DATE TIMESTAMP, CONSTRAINT PK_ORG_APP PRIMARY KEY (ID), CONSTRAINT FK_OA_USER FOREIGN KEY (USER_ID) REFERENCES USERS(ID))",
                   "ORGANIZER_APPLICATIONS table");
        executeDDL("CREATE SEQUENCE ORGANIZER_APP_SEQ START WITH 1 INCREMENT BY 1 NOCACHE",
                   "ORGANIZER_APP_SEQ sequence");
        executeDDL("CREATE INDEX IDX_ORG_APP_USER_STATUS ON ORGANIZER_APPLICATIONS(USER_ID, STATUS)",
                   "IDX_ORG_APP_USER_STATUS index");

        // Feature: Refund Flow
        executeDDL("ALTER TABLE ORDERS ADD VNPAY_TRANSACTION_DATE VARCHAR2(14)",
                   "ORDERS.VNPAY_TRANSACTION_DATE column added");
        executeDDL("CREATE TABLE REFUND_REQUESTS (" +
                   "ID NUMBER NOT NULL, " +
                   "ORDER_ID NUMBER NOT NULL, " +
                   "AMOUNT NUMBER(14,2) NOT NULL, " +
                   "STATUS VARCHAR2(20) DEFAULT 'PENDING' NOT NULL, " +
                   "VNPAY_REQUEST_ID VARCHAR2(100), " +
                   "VNPAY_RESPONSE_CODE VARCHAR2(10), " +
                   "VNPAY_RESPONSE_MESSAGE VARCHAR2(500), " +
                   "CREATED_DATE TIMESTAMP NOT NULL, " +
                   "UPDATED_DATE TIMESTAMP, " +
                   "CONSTRAINT PK_REFUND_REQUESTS PRIMARY KEY (ID), " +
                   "CONSTRAINT FK_RR_ORDER FOREIGN KEY (ORDER_ID) REFERENCES ORDERS(ID), " +
                   "CONSTRAINT UQ_RR_ORDER UNIQUE (ORDER_ID))",
                   "REFUND_REQUESTS table");
        executeDDL("CREATE SEQUENCE REFUND_SEQ START WITH 1 INCREMENT BY 1 NOCACHE",
                   "REFUND_SEQ sequence");

        // Feature: Featured Events order
        executeDDL("ALTER TABLE EVENTS ADD FEATURED_ORDER NUMBER(4) DEFAULT 999 NOT NULL",
                   "EVENTS.FEATURED_ORDER column added");
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
