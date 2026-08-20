package com.sakaryateknokent.reviewerassignment.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermanentProjectDocumentMetadataRemovalMigrationTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/"
                    + "V24__remove_permanent_project_document_metadata.sql"
    );

    @Test
    void removesOnlyPermanentProjectDocumentMetadata() throws IOException {
        String sql = normalizedSql();

        assertTrue(sql.contains(
                "ALTER TABLE PROJECTS DROP CONSTRAINT IF EXISTS "
                        + "CHK_PROJECTS_FILE_SIZE_NON_NEGATIVE"
        ));
        assertTrue(sql.contains("DROP COLUMN SOURCE_FILE_ID"));
        assertTrue(sql.contains("DROP COLUMN ORIGINAL_FILE_NAME"));
        assertTrue(sql.contains("DROP COLUMN STORAGE_KEY"));
        assertTrue(sql.contains("DROP COLUMN CONTENT_TYPE"));
        assertTrue(sql.contains("DROP COLUMN FILE_SIZE"));
        assertFalse(sql.contains("PROJECT_UPLOADS"));
        assertFalse(sql.matches("(?s).*\\b(INSERT|UPDATE|DELETE)\\b.*"));
    }

    private String normalizedSql() throws IOException {
        return Files.readString(MIGRATION)
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
