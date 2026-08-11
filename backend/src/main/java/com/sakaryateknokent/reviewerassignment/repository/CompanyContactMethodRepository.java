package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CompanyContactMethodRepository
        extends JpaRepository<CompanyContactMethod, Long> {

    List<CompanyContactMethod> findByCompanyIdIn(Collection<Long> companyIds);
}
