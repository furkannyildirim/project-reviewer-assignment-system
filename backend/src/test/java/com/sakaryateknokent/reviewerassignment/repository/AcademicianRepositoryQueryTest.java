package com.sakaryateknokent.reviewerassignment.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicianRepositoryQueryTest {

    @Test
    void searchQueryAppliesInitialToFirstNameInResultAndCountQueries()
            throws NoSuchMethodException {
        Query query = searchMethod().getAnnotation(Query.class);

        assertInitialFilter(query.value());
        assertInitialFilter(query.countQuery());
    }

    @Test
    void searchQueryIncludesFullNameInResultAndCountQueries()
            throws NoSuchMethodException {
        Query query = searchMethod().getAnnotation(Query.class);

        assertFullNameSearch(query.value());
        assertFullNameSearch(query.countQuery());
    }

    @Test
    void searchQueryKeepsExistingSearchFieldsInResultAndCountQueries()
            throws NoSuchMethodException {
        Query query = searchMethod().getAnnotation(Query.class);

        assertExistingSearchFields(query.value());
        assertExistingSearchFields(query.countQuery());
    }

    @Test
    void identityLookupUsesPersistedCanonicalColumns()
            throws NoSuchMethodException {
        Query query = identityMethod().getAnnotation(Query.class);
        String normalizedQuery = query.value().replaceAll("\\s+", " ");

        assertTrue(normalizedQuery.contains(
                "academician.canonicalFirstName = :firstName"
        ));
        assertTrue(normalizedQuery.contains(
                "academician.canonicalLastName = :lastName"
        ));
        assertTrue(normalizedQuery.contains(
                "academician.canonicalTitle = :title"
        ));
        assertFalse(normalizedQuery.contains("LOWER(TRIM"));
        assertFalse(normalizedQuery.contains("COALESCE(LOWER"));
    }

    private void assertInitialFilter(String query) {
        String normalizedQuery = query.replaceAll("\\s+", " ");

        assertTrue(normalizedQuery.contains(":initial = ''"));
        assertTrue(normalizedQuery.contains("LOWER(academician.firstName) LIKE LOWER(CONCAT(:initial, '%'))"));
        assertTrue(normalizedQuery.contains(") AND ("));
        assertFalse(normalizedQuery.contains("LOWER(academician.lastName) LIKE LOWER(CONCAT(:initial, '%'))"));
    }

    private void assertFullNameSearch(String query) {
        String normalizedQuery = query.replaceAll("\\s+", " ");

        assertTrue(normalizedQuery.contains("LOWER(CONCAT(academician.firstName, ' ', academician.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))"));
    }

    private void assertExistingSearchFields(String query) {
        String normalizedQuery = query.replaceAll("\\s+", " ");

        assertTrue(normalizedQuery.contains("LOWER(academician.firstName) LIKE LOWER(CONCAT('%', :search, '%'))"));
        assertTrue(normalizedQuery.contains("LOWER(academician.lastName) LIKE LOWER(CONCAT('%', :search, '%'))"));
        assertTrue(normalizedQuery.contains("LOWER(academician.email) LIKE LOWER(CONCAT('%', :search, '%'))"));
        assertTrue(normalizedQuery.contains("LOWER(academician.phone) LIKE LOWER(CONCAT('%', :search, '%'))"));
        assertTrue(normalizedQuery.contains("LOWER(academician.title) LIKE LOWER(CONCAT('%', :search, '%'))"));
        assertTrue(normalizedQuery.contains("LOWER(keyword) LIKE LOWER(CONCAT('%', :search, '%'))"));
    }

    private Method searchMethod() throws NoSuchMethodException {
        return AcademicianRepository.class.getMethod(
                "search",
                String.class,
                String.class,
                Pageable.class
        );
    }

    private Method identityMethod() throws NoSuchMethodException {
        return AcademicianRepository.class.getMethod(
                "findByCanonicalIdentity",
                String.class,
                String.class,
                String.class
        );
    }
}
