package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.CompanyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CompanyContactRepository
        extends JpaRepository<CompanyContact, Long> {

    List<CompanyContact> findByCompanyIdIn(Collection<Long> companyIds);
}
