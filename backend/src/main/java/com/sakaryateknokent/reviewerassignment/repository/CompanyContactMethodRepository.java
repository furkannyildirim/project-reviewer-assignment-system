package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CompanyContactMethodRepository
        extends JpaRepository<CompanyContactMethod, Long> {

    boolean existsByTypeAndValueIgnoreCase(
            CompanyContactMethodType type,
            String value
    );

    boolean existsByTypeAndValueIgnoreCaseAndCompanyIdNot(
            CompanyContactMethodType type,
            String value,
            Long companyId
    );

    List<CompanyContactMethod> findByCompanyIdIn(Collection<Long> companyIds);
}
