package com.ezfinanz.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate ddl-auto=update does not refresh PostgreSQL CHECK constraints when enum values are added.
 * This keeps review_status in sync with {@code SelfieReviewStatus} (including DRAFT).
 */
@Component
public class SchemaConstraintMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaConstraintMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaConstraintMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE selfie_submissions DROP CONSTRAINT IF EXISTS selfie_submissions_review_status_check"
            );
            jdbcTemplate.execute(
                    """
                    ALTER TABLE selfie_submissions
                    ADD CONSTRAINT selfie_submissions_review_status_check
                    CHECK (review_status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED'))
                    """
            );
            log.info("Ensured selfie_submissions.review_status allows DRAFT");
        } catch (Exception ex) {
            log.warn("Could not refresh selfie review_status check constraint: {}", ex.getMessage());
        }
    }
}
