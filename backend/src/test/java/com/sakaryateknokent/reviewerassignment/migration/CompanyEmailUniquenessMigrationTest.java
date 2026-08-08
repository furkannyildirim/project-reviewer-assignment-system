package com.sakaryateknokent.reviewerassignment.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyEmailUniquenessMigrationTest {

    private static final Path GLOBAL_MIGRATION = Path.of(
            "src/main/resources/db/migration/V15__restore_global_company_email_uniqueness.sql"
    );
    private static final Path SCOPED_MIGRATION = Path.of(
            "src/main/resources/db/migration/V16__scope_company_email_uniqueness.sql"
    );

    @Test
    void restoresGlobalEmailUniquenessAfterCompanyScopedMigration()
            throws IOException {
        String normalizedSql = normalizedSql(GLOBAL_MIGRATION);

        assertTrue(normalizedSql.contains(
                "drop index uq_company_contact_methods_company_email_value"
        ));
        assertTrue(normalizedSql.contains(
                "create unique index uq_company_contact_methods_email_value"
        ));
        assertTrue(normalizedSql.contains(
                "on company_contact_methods (lower(value))"
        ));
        assertTrue(normalizedSql.contains("where type = 'email'"));
        assertFalse(normalizedSql.contains("company_id"));
        assertFalse(normalizedSql.contains("where type = 'phone'"));
    }

    @Test
    void finalMigrationRestoresCompanyScopedCaseInsensitiveEmailUniqueness()
            throws IOException {
        String normalizedSql = normalizedSql(SCOPED_MIGRATION);

        assertTrue(normalizedSql.contains(
                "drop index uq_company_contact_methods_email_value"
        ));
        assertTrue(normalizedSql.contains(
                "create unique index uq_company_contact_methods_company_email_value"
        ));
        assertTrue(normalizedSql.contains(
                "on company_contact_methods (company_id, lower(value))"
        ));
        assertTrue(normalizedSql.contains("where type = 'email'"));
        assertFalse(normalizedSql.contains(
                "create unique index uq_company_contact_methods_email_value"
        ));
    }

    @Test
    void finalMigrationLeavesPhoneAndLegacyCompanyEmailSchemaUntouched()
            throws IOException {
        String normalizedSql = normalizedSql(SCOPED_MIGRATION);

        assertFalse(normalizedSql.contains("where type = 'phone'"));
        assertFalse(normalizedSql.contains("alter table companies"));
        assertFalse(normalizedSql.contains("companies.email"));
        assertFalse(normalizedSql.contains(
                "uq_company_contact_methods_primary_type"
        ));
    }

    private String normalizedSql(Path migration) throws IOException {
        return Files.readString(migration)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}
