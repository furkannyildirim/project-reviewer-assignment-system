package com.sakaryateknokent.reviewerassignment.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyLocationMigrationTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/V12__add_company_location_fields.sql"
    );

    @Test
    void addsNullableLocationColumnsWithoutBackfillOrConstraints()
            throws IOException {
        String sql = Files.readString(MIGRATION);
        String normalizedSql = sql.toLowerCase()
                .replaceAll("\\s+", " ");

        assertTrue(normalizedSql.contains("alter table companies"));
        assertTrue(normalizedSql.contains("add column city varchar(150)"));
        assertTrue(normalizedSql.contains("add column district varchar(150)"));
        assertTrue(normalizedSql.contains("add column osb varchar(255)"));
        assertTrue(normalizedSql.contains("add column address text"));
        assertTrue(normalizedSql.contains("add column building varchar(255)"));
        assertFalse(normalizedSql.contains("not null"));
        assertFalse(normalizedSql.contains("update companies"));
        assertFalse(Pattern.compile("\\bdefault\\b")
                .matcher(normalizedSql)
                .find());
    }
}
