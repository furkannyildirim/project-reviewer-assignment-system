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

    private void assertInitialFilter(String query) {
        String normalizedQuery = query.replaceAll("\\s+", " ");

        assertTrue(normalizedQuery.contains(":initial = ''"));
        assertTrue(normalizedQuery.contains("LOWER(academician.firstName) LIKE LOWER(CONCAT(:initial, '%'))"));
        assertTrue(normalizedQuery.contains(") AND ("));
        assertFalse(normalizedQuery.contains("LOWER(academician.lastName) LIKE LOWER(CONCAT(:initial, '%'))"));
    }

    private Method searchMethod() throws NoSuchMethodException {
        return AcademicianRepository.class.getMethod(
                "search",
                String.class,
                String.class,
                Pageable.class
        );
    }
}
