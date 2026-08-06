package com.sakaryateknokent.reviewerassignment.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyContactConstraintMigrationTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/V13__relax_company_contact_method_constraint.sql"
    );

    @Test
    void dropsContactMethodConstraintWithoutChangingFullNameConstraint()
            throws IOException {
        String sql = Files.readString(MIGRATION);
        String normalizedSql = sql.toLowerCase()
                .replaceAll("\\s+", " ");

        assertTrue(normalizedSql.contains("alter table company_contacts"));
        assertTrue(normalizedSql.contains("chk_company_contacts_contact_method"));
        assertFalse(normalizedSql.contains(
                "chk_company_contacts_full_name_not_blank"
        ));
        assertFalse(normalizedSql.contains("drop column"));
    }
}
