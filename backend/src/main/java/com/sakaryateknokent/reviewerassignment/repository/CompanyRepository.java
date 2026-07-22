package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
            SELECT company
            FROM Company company
            WHERE :search = ''
               OR LOWER(company.name)
                    LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(company.email)
                    LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(company.phone)
                    LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(company.activityArea)
                    LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<Company> search(
            @Param("search") String search,
            Pageable pageable
    );
}