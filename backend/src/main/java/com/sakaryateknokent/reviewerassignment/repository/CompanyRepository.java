package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCompanyTypeAndEmailIgnoreCase(
            CompanyType companyType,
            String email
    );

    boolean existsByEmailAndIdNot(String email, Long id);

    long countByCompanyType(CompanyType companyType);

    @Query("""
            SELECT company.name
            FROM Company company
            WHERE company.companyType = :companyType
            """)
    List<String> findNamesByCompanyType(
            @Param("companyType") CompanyType companyType
    );

    @EntityGraph(attributePaths = "contactMethods")
    @Query("""
            SELECT DISTINCT company
            FROM Company company
            WHERE company.companyType = :companyType
            """)
    List<Company> findByCompanyType(
            @Param("companyType") CompanyType companyType
    );

    List<Company> findByCompanyTypeAndIdIn(
            CompanyType companyType,
            Collection<Long> ids,
            Sort sort
    );

    @Query("""
            SELECT company
            FROM Company company
            WHERE company.companyType = :companyType
              AND (
                   :search = ''
                   OR LOWER(company.name)
                        LIKE LOWER(CONCAT('%', :search, '%'))
                   OR EXISTS (
                        SELECT method.id
                        FROM CompanyContactMethod method
                        WHERE method.company = company
                          AND LOWER(method.value)
                              LIKE LOWER(CONCAT('%', :search, '%'))
                   )
                   OR LOWER(company.activityArea)
                        LIKE LOWER(CONCAT('%', :search, '%'))
              )
              AND (
                   :initial = ''
                   OR LOWER(company.name)
                        LIKE LOWER(CONCAT(:initial, '%'))
              )
            """)
    Page<Company> search(
            @Param("companyType") CompanyType companyType,
            @Param("search") String search,
            @Param("initial") String initial,
            Pageable pageable
    );
}
