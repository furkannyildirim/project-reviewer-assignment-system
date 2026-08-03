package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyRepositoryQueryTest {

    @Test
    void searchQueryAppliesCompanyTypeSearchAndInitialToCompanyName()
            throws NoSuchMethodException {
        Query query = searchMethod().getAnnotation(Query.class);
        String normalizedQuery = query.value().replaceAll("\\s+", " ");

        assertTrue(normalizedQuery.contains("company.companyType = :companyType"));
        assertTrue(normalizedQuery.contains(":search = ''"));
        assertTrue(normalizedQuery.contains("LOWER(company.name) LIKE LOWER(CONCAT('%', :search, '%'))"));
        assertTrue(normalizedQuery.contains(":initial = ''"));
        assertTrue(normalizedQuery.contains("LOWER(company.name) LIKE LOWER(CONCAT(:initial, '%'))"));
        assertTrue(normalizedQuery.contains(") AND ( :initial"));
        assertFalse(normalizedQuery.contains("LOWER(company.activityArea) LIKE LOWER(CONCAT(:initial, '%'))"));
        assertFalse(normalizedQuery.contains("LOWER(method.value) LIKE LOWER(CONCAT(:initial, '%'))"));
    }

    private Method searchMethod() throws NoSuchMethodException {
        return CompanyRepository.class.getMethod(
                "search",
                CompanyType.class,
                String.class,
                String.class,
                Pageable.class
        );
    }
}
