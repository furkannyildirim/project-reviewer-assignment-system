package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCompanyTypeAndEmailIgnoreCase(
            CompanyType companyType,
            String email
    );

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
            SELECT company.name
            FROM Company company
            WHERE company.companyType = :companyType
            """)
    java.util.List<String> findNamesByCompanyType(
            @Param("companyType") CompanyType companyType
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
            """)
    Page<Company> search(
            @Param("companyType") CompanyType companyType,
            @Param("search") String search,
            Pageable pageable
    );
}
