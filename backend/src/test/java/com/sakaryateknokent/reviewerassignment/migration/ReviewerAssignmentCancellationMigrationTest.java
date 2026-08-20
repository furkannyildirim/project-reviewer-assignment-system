package com.sakaryateknokent.reviewerassignment.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewerAssignmentCancellationMigrationTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/"
                    + "V22__allow_cancelled_reviewer_assignment_status.sql"
    );

    @Test
    void assignmentConstraintAddsCancelledWithoutRemovingExistingStatuses()
            throws IOException {
        String sql = Files.readString(MIGRATION)
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);

        assertTrue(sql.contains("DROP CONSTRAINT CHK_PROJECT_REVIEWER_ASSIGNMENTS_STATUS"));
        assertTrue(sql.contains("ADD CONSTRAINT CHK_PROJECT_REVIEWER_ASSIGNMENTS_STATUS"));
        assertTrue(sql.contains("'ACTIVE'"));
        assertTrue(sql.contains("'COMPLETED'"));
        assertTrue(sql.contains("'REMOVED'"));
        assertTrue(sql.contains("'CANCELLED'"));
    }
}
