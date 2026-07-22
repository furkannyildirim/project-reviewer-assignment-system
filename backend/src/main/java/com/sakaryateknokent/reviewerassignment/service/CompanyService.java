package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyResponse;
import com.sakaryateknokent.reviewerassignment.dto.CreateCompanyRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateCompanyRequest;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sakaryateknokent.reviewerassignment.dto.PageResponse;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private static final Sort DEFAULT_COMPANY_SORT =
            Sort.by(Sort.Order.asc("name").ignoreCase());

    private static final Set<Integer> ALLOWED_PAGE_SIZES =
            Set.of(10, 15, 20, 25);

    private final CompanyRepository companyRepository;

    public CompanyResponse createCompany(CreateCompanyRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (companyRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException(
                    "Bu e-posta adresiyle kayıtlı bir şirket zaten var"
            );
        }

        Company company = new Company();
        company.setName(request.name().trim());
        company.setEmail(normalizedEmail);
        company.setPhone(request.phone().trim());
        company.setActivityArea(request.activityArea().trim());
        company.setLogoUrl(normalizeOptionalText(request.logoUrl()));

        return toResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> getCompanies(
            String search,
            int page,
            int size
    ) {
        validatePagination(page, size);

        String normalizedSearch = search == null
                ? ""
                : search.trim();

        Pageable pageable = PageRequest.of(
                page,
                size,
                DEFAULT_COMPANY_SORT
        );

        Page<CompanyResponse> companyPage = companyRepository
                .search(normalizedSearch, pageable)
                .map(this::toResponse);

        return new PageResponse<>(
                companyPage.getContent(),
                companyPage.getNumber(),
                companyPage.getSize(),
                companyPage.getTotalElements(),
                companyPage.getTotalPages(),
                companyPage.isFirst(),
                companyPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        return toResponse(findCompanyById(id));
    }

    public CompanyResponse updateCompany(
            Long id,
            UpdateCompanyRequest request
    ) {
        Company company = findCompanyById(id);
        String normalizedEmail = normalizeEmail(request.email());

        if (companyRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new DuplicateResourceException(
                    "Bu e-posta adresiyle kayıtlı başka bir şirket zaten var"
            );
        }

        company.setName(request.name().trim());
        company.setEmail(normalizedEmail);
        company.setPhone(request.phone().trim());
        company.setActivityArea(request.activityArea().trim());
        company.setLogoUrl(normalizeOptionalText(request.logoUrl()));

        return toResponse(companyRepository.save(company));
    }

    public void deleteCompany(Long id) {
        Company company = findCompanyById(id);
        companyRepository.delete(company);
    }

    private Company findCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Şirket bulunamadı: " + id
                ));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException(
                    "Sayfa numarası 0 veya daha büyük olmalıdır"
            );
        }

        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            throw new InvalidRequestException(
                    "Sayfa boyutu yalnızca 10, 15, 20 veya 25 olabilir"
            );
        }
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getEmail(),
                company.getPhone(),
                company.getActivityArea(),
                company.getLogoUrl()
        );
    }
}