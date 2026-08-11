package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyFileDuplicateClassifierTest {

    @Test
    void externalDifferentNamesWithSameEmailAreNew() {
        assertUnique(
                row("ABC", null, null, "same@example.com"),
                row("XYZ", null, null, "same@example.com")
        );
    }

    @Test
    void externalSameNameDifferentAddressesAreNew() {
        assertUnique(
                row("ABC", "Sakarya Teknokent", null, "same@example.com"),
                row("ABC", "Istanbul Teknopark", null, "same@example.com")
        );
    }

    @Test
    void externalSameNameOneAddressWithMatchingPhoneIsDuplicate() {
        assertDuplicate(
                row("ABC", "Sakarya Teknokent", "0532", null),
                row("ABC", null, "0532", null)
        );
    }

    @Test
    void externalSameNameOneAddressWithMatchingEmailIsDuplicate() {
        assertDuplicate(
                row("ABC", "Sakarya Teknokent", null, "info@example.com"),
                row("ABC", null, null, "INFO@example.com")
        );
    }

    @Test
    void externalSameNameSameAddressWithoutContactsIsDuplicate() {
        assertDuplicate(
                row("ABC", "Sakarya Teknokent", null, null),
                row(" abc ", " sakarya   teknokent ", null, null)
        );
    }

    @Test
    void externalSameNameSameAddressWithMatchingContactIsDuplicate() {
        assertDuplicate(
                row("ABC", "Sakarya Teknokent", "0532", "info@example.com"),
                row("ABC", "Sakarya Teknokent", "0544", "INFO@example.com")
        );
    }

    @Test
    void externalSameNameSameAddressWithDifferentContactsIsNew() {
        assertUnique(
                row("ABC", "Sakarya Teknokent", "0532", "info@example.com"),
                row("ABC", "Sakarya Teknokent", "0544", "other@example.com")
        );
    }

    @Test
    void externalSameNameOneAddressWithDifferentContactsIsNew() {
        assertUnique(
                row("ABC", "Sakarya Teknokent", "0532", null),
                row("ABC", null, "0544", null)
        );
    }

    @Test
    void externalSameNameOneAddressWithInsufficientContactIsDuplicate() {
        assertDuplicate(
                row("ABC", "Sakarya Teknokent", "0532", null),
                row("ABC", null, null, null)
        );
    }

    @Test
    void externalNoAddressWithoutContactsIsDuplicate() {
        assertDuplicate(
                row("ABC", null, null, null),
                row("ABC", null, null, null)
        );
    }

    @Test
    void externalNoAddressWithContactOnlyOnOneSideUsesNameFallback() {
        assertDuplicate(
                row("ABC", null, null, null),
                row("ABC", null, "0532", null)
        );
    }

    @Test
    void externalNoAddressWithMatchingContactIsDuplicate() {
        assertDuplicate(
                row("ABC", null, "0532", null),
                row("ABC", null, "0532", null)
        );
    }

    @Test
    void externalNoAddressWithDifferentContactsIsNew() {
        assertUnique(
                row("ABC", null, "0532", null),
                row("ABC", null, "0544", null)
        );
    }

    @Test
    void tgbSameCanonicalNamesAreDuplicateRegardlessOfContacts() {
        CompanyImportRow first = row(
                "ABC Teknoloji",
                "Adres A",
                "111",
                "first@example.com"
        );
        CompanyImportRow second = row(
                " abc   teknoloji ",
                "Adres B",
                "222",
                "second@example.com"
        );

        CompanyFileDuplicateClassifier.FileDuplicateRows duplicates =
                CompanyFileDuplicateClassifier.findDuplicates(
                        List.of(first, second),
                        CompanyType.TGB
                );

        assertTrue(duplicates.duplicate(first));
        assertTrue(duplicates.duplicate(second));
    }

    @Test
    void tgbDifferentNamesWithSameEmailAndPhoneAreNew() {
        CompanyImportRow first = row(
                "ABC Teknoloji",
                "Ayni Adres",
                "111",
                "same@example.com"
        );
        CompanyImportRow second = row(
                "XYZ Teknoloji",
                "Ayni Adres",
                "111",
                "SAME@example.com"
        );

        CompanyFileDuplicateClassifier.FileDuplicateRows duplicates =
                CompanyFileDuplicateClassifier.findDuplicates(
                        List.of(first, second),
                        CompanyType.TGB
                );

        assertFalse(duplicates.duplicate(first));
        assertFalse(duplicates.duplicate(second));
    }

    private void assertDuplicate(
            CompanyImportRow first,
            CompanyImportRow second
    ) {
        CompanyFileDuplicateClassifier.FileDuplicateRows duplicates =
                CompanyFileDuplicateClassifier.findDuplicates(
                        List.of(first, second),
                        CompanyType.EXTERNAL
                );

        assertTrue(duplicates.duplicate(first));
        assertTrue(duplicates.duplicate(second));
    }

    private void assertUnique(
            CompanyImportRow first,
            CompanyImportRow second
    ) {
        CompanyFileDuplicateClassifier.FileDuplicateRows duplicates =
                CompanyFileDuplicateClassifier.findDuplicates(
                        List.of(first, second),
                        CompanyType.EXTERNAL
                );

        assertFalse(duplicates.duplicate(first));
        assertFalse(duplicates.duplicate(second));
    }

    private CompanyImportRow row(
            String name,
            String address,
            String phone,
            String email
    ) {
        CompanyImportRow row = new CompanyImportRow("Sheet", 1);
        row.setName(name);
        row.setAddress(address);
        if (phone != null) {
            row.addCompanyPhone(phone);
        }
        if (email != null) {
            row.addCompanyEmail(email);
        }

        return row;
    }
}
