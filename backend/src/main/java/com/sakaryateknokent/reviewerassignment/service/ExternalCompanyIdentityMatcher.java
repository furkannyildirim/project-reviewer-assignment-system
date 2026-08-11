package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportRow;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class ExternalCompanyIdentityMatcher {

    private static final Locale TURKISH_LOCALE =
            Locale.forLanguageTag("tr");

    private ExternalCompanyIdentityMatcher() {
    }

    static boolean matches(
            CompanyImportRow first,
            CompanyImportRow second
    ) {
        return evaluate(identity(first), identity(second)).matches();
    }

    static boolean matches(
            CompanyImportRow row,
            Company company
    ) {
        return evaluate(row, company).matches();
    }

    static MatchStrength evaluate(
            CompanyImportRow row,
            Company company
    ) {
        return evaluate(identity(row), identity(company));
    }

    private static MatchStrength evaluate(Identity first, Identity second) {
        if (!canonicalName(first.name()).equals(canonicalName(second.name()))) {
            return MatchStrength.DIFFERENT;
        }

        String firstAddress = canonicalAddress(first.address());
        String secondAddress = canonicalAddress(second.address());
        if (!firstAddress.isEmpty()
                && !secondAddress.isEmpty()
                && !firstAddress.equals(secondAddress)) {
            return MatchStrength.DIFFERENT;
        }

        Set<String> firstContacts = comparableContacts(first);
        Set<String> secondContacts = comparableContacts(second);
        if (firstContacts.isEmpty() || secondContacts.isEmpty()) {
            if (!firstAddress.isEmpty()
                    && firstAddress.equals(secondAddress)) {
                return MatchStrength.STRONG;
            }
            return MatchStrength.NAME_FALLBACK;
        }

        return firstContacts.stream().anyMatch(secondContacts::contains)
                ? MatchStrength.STRONG
                : MatchStrength.DIFFERENT;
    }

    private static Identity identity(CompanyImportRow row) {
        return new Identity(
                row.getName(),
                row.getAddress(),
                Set.copyOf(row.getCompanyPhones()),
                Set.copyOf(row.getCompanyEmails())
        );
    }

    private static Identity identity(Company company) {
        Set<String> phones = new LinkedHashSet<>();
        Set<String> emails = new LinkedHashSet<>();
        for (CompanyContactMethod method : company.getContactMethods()) {
            if (CompanyContactMethodType.PHONE.equals(method.getType())) {
                phones.add(method.getValue());
            } else if (CompanyContactMethodType.EMAIL.equals(method.getType())) {
                emails.add(method.getValue());
            }
        }

        return new Identity(
                company.getName(),
                company.getAddress(),
                phones,
                emails
        );
    }

    private static Set<String> comparableContacts(Identity identity) {
        Set<String> contacts = new LinkedHashSet<>();
        for (String phone : identity.phones()) {
            String normalizedPhone =
                    AcademicianTextNormalizer.normalizeOptionalText(phone);
            if (normalizedPhone != null) {
                contacts.add("phone:" + normalizedPhone);
            }
        }
        for (String email : identity.emails()) {
            String normalizedEmail =
                    AcademicianTextNormalizer.normalizeOptionalText(email);
            if (normalizedEmail != null) {
                contacts.add("email:" + canonicalEmail(normalizedEmail));
            }
        }

        return contacts;
    }

    static String canonicalEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String canonicalName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        return AcademicianTextNormalizer.normalizeWhitespace(name)
                .toLowerCase(TURKISH_LOCALE);
    }

    static String canonicalAddress(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }

        return AcademicianTextNormalizer.normalizeWhitespace(address)
                .toLowerCase(TURKISH_LOCALE);
    }

    private record Identity(
            String name,
            String address,
            Set<String> phones,
            Set<String> emails
    ) {
    }

    enum MatchStrength {
        DIFFERENT,
        STRONG,
        NAME_FALLBACK;

        boolean matches() {
            return this != DIFFERENT;
        }
    }
}
