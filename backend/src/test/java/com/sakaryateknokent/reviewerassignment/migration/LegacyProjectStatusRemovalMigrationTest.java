package com.sakaryateknokent.reviewerassignment.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyProjectStatusRemovalMigrationTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/"
                    + "V23__remove_legacy_project_statuses.sql"
    );

    @Test
    void rejectsLegacyRowsBeforeNarrowingStatusConstraints() throws IOException {
        String sql = normalizedSql();
        int firstConstraintChange = sql.indexOf("ALTER TABLE PROJECTS");
        int projectGuard = sql.indexOf(
                "FROM PROJECTS WHERE STATUS IN ('DRAFT', 'CANCELLED')"
        );
        int assignmentGuard = sql.indexOf(
                "FROM PROJECT_REVIEWER_ASSIGNMENTS WHERE STATUS = 'CANCELLED'"
        );

        assertTrue(projectGuard >= 0 && projectGuard < firstConstraintChange);
        assertTrue(assignmentGuard >= 0 && assignmentGuard < firstConstraintChange);
        assertTrue(sql.contains("RAISE EXCEPTION"));
        assertFalse(sql.matches("(?s).*\\b(INSERT|UPDATE|DELETE)\\b.*"));
    }

    @Test
    void dropsDraftDefaultAndAllowsOnlyCurrentDomainStatuses() throws IOException {
        String sql = normalizedSql();
        String constraintChanges = sql.substring(sql.indexOf("ALTER TABLE PROJECTS"));

        assertTrue(constraintChanges.contains("ALTER COLUMN STATUS DROP DEFAULT"));
        assertTrue(constraintChanges.contains(
                "CHECK (STATUS IN ('AWAITING_REVIEWER', 'ACTIVE', 'COMPLETED'))"
        ));
        assertTrue(constraintChanges.contains(
                "CHECK (STATUS IN ('ACTIVE', 'COMPLETED', 'REMOVED'))"
        ));
        assertFalse(constraintChanges.contains("'DRAFT'"));
        assertFalse(constraintChanges.contains("'CANCELLED'"));
    }

    private String normalizedSql() throws IOException {
        return Files.readString(MIGRATION)
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
