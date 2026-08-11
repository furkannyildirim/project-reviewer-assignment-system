package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianResponse;
import com.sakaryateknokent.reviewerassignment.dto.AcademicianIdentityWarningResponse;
import com.sakaryateknokent.reviewerassignment.dto.CreateAcademicianRequest;
import com.sakaryateknokent.reviewerassignment.dto.PageResponse;
import com.sakaryateknokent.reviewerassignment.dto.UpdateAcademicianRequest;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademicianService {

    private static final Set<Integer> ALLOWED_PAGE_SIZES =
            Set.of(10, 15, 20, 25);

    private static final String IDENTITY_WARNING_MESSAGE =
            "Aynı ad, soyad ve ünvanla kayıtlı bir akademisyen zaten var. "
                    + "Yine de devam etmek istiyor musunuz?";

    private static final Sort DEFAULT_ACADEMICIAN_SORT = Sort.by(
            Sort.Order.asc("firstName"),
            Sort.Order.asc("lastName")
    );

    private final AcademicianRepository academicianRepository;

    public AcademicianResponse createAcademician(
            CreateAcademicianRequest request
    ) {
        String normalizedEmail =
                AcademicianTextNormalizer.normalizeEmail(request.email());
        validateEmailForCreate(normalizedEmail);

        Academician academician = new Academician();
        academician.setFirstName(request.firstName().trim());
        academician.setLastName(request.lastName().trim());
        academician.setEmail(normalizedEmail);
        academician.setPhone(
                AcademicianTextNormalizer.normalizeOptionalText(
                        request.phone()
                )
        );
        academician.setTitle(
                AcademicianTextNormalizer.normalizeOptionalText(
                        request.title()
                )
        );
        academician.getKeywords().addAll(
                AcademicianTextNormalizer.normalizeKeywords(
                        request.keywords()
                )
        );
        academician.refreshCanonicalIdentity();

        return toResponse(academicianRepository.saveAndFlush(academician));
    }

    @Transactional(readOnly = true)
    public PageResponse<AcademicianResponse> getAcademicians(
            String search,
            String initial,
            int page,
            int size
    ) {
        validatePagination(page, size);

        String normalizedSearch = normalizeSearch(search);
        String normalizedInitial = normalizeInitial(initial);

        Pageable pageable = PageRequest.of(
                page,
                size,
                DEFAULT_ACADEMICIAN_SORT
        );

        Page<AcademicianResponse> academicianPage =
                academicianRepository
                        .search(normalizedSearch, normalizedInitial, pageable)
                        .map(this::toResponse);

        return new PageResponse<>(
                academicianPage.getContent(),
                academicianPage.getNumber(),
                academicianPage.getSize(),
                academicianPage.getTotalElements(),
                academicianPage.getTotalPages(),
                academicianPage.isFirst(),
                academicianPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public long countAcademicians() {
        return academicianRepository.count();
    }

    @Transactional(readOnly = true)
    public AcademicianIdentityWarningResponse getIdentityWarning(
            String firstName,
            String lastName,
            String title,
            Long excludeAcademicianId
    ) {
        boolean warning = academicianRepository.findByCanonicalIdentity(
                        AcademicianTextNormalizer.canonicalizeIdentityPart(
                                firstName
                        ),
                        AcademicianTextNormalizer.canonicalizeIdentityPart(
                                lastName
                        ),
                        AcademicianTextNormalizer.canonicalizeIdentityPart(
                                title
                        )
                )
                .stream()
                .anyMatch(academician -> !academician.getId().equals(
                        excludeAcademicianId
                ));

        return new AcademicianIdentityWarningResponse(
                warning,
                warning ? IDENTITY_WARNING_MESSAGE : null
        );
    }

    @Transactional(readOnly = true)
    public List<Long> getAcademicianIds(String search, String initial) {
        String normalizedSearch = normalizeSearch(search);
        String normalizedInitial = normalizeInitial(initial);

        return academicianRepository
                .search(
                        normalizedSearch,
                        normalizedInitial,
                        Pageable.unpaged(DEFAULT_ACADEMICIAN_SORT)
                )
                .stream()
                .map(Academician::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicianResponse> getAcademiciansForExport(
            String search,
            String initial
    ) {
        String normalizedSearch = normalizeSearch(search);
        String normalizedInitial = normalizeInitial(initial);

        return academicianRepository
                .search(
                        normalizedSearch,
                        normalizedInitial,
                        Pageable.unpaged(DEFAULT_ACADEMICIAN_SORT)
                )
                .map(this::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<AcademicianResponse> getAcademiciansForExport(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return academicianRepository
                .findByIdIn(
                        new LinkedHashSet<>(ids),
                        DEFAULT_ACADEMICIAN_SORT
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcademicianResponse getAcademicianById(Long id) {
        return toResponse(findAcademicianById(id));
    }

    public AcademicianResponse updateAcademician(
            Long id,
            UpdateAcademicianRequest request
    ) {
        Academician academician = findAcademicianById(id);
        String normalizedEmail =
                AcademicianTextNormalizer.normalizeEmail(request.email());

        validateVersion(request.version(), academician);
        validateEmailForUpdate(normalizedEmail, id);

        academician.setFirstName(request.firstName().trim());
        academician.setLastName(request.lastName().trim());
        academician.setEmail(normalizedEmail);
        academician.setPhone(
                AcademicianTextNormalizer.normalizeOptionalText(
                        request.phone()
                )
        );
        academician.setTitle(
                AcademicianTextNormalizer.normalizeOptionalText(
                        request.title()
                )
        );

        academician.getKeywords().clear();
        academician.getKeywords().addAll(
                AcademicianTextNormalizer.normalizeKeywords(
                        request.keywords()
                )
        );
        academician.refreshCanonicalIdentity();

        return toResponse(academicianRepository.saveAndFlush(academician));
    }

    public void deleteAcademician(Long id) {
        Academician academician = findAcademicianById(id);
        academicianRepository.delete(academician);
    }

    private Academician findAcademicianById(Long id) {
        return academicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Akademisyen bulunamadı: " + id
                ));
    }

    private void validateEmailForCreate(String email) {
        if (email != null && academicianRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Bu e-posta adresiyle kayıtlı bir akademisyen zaten var"
            );
        }
    }

    private void validateEmailForUpdate(String email, Long id) {
        if (email != null
                && academicianRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException(
                    "Bu e-posta adresiyle kayıtlı başka bir akademisyen zaten var"
            );
        }
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

    private void validateVersion(Long requestVersion, Academician academician) {
        if (!requestVersion.equals(academician.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(
                    Academician.class,
                    academician.getId()
            );
        }
    }

    private String normalizeSearch(String search) {
        return search == null
                ? ""
                : search.trim();
    }

    private String normalizeInitial(String initial) {
        return initial == null
                ? ""
                : initial.trim();
    }

    private AcademicianResponse toResponse(Academician academician) {
        Set<String> keywords = Collections.unmodifiableSet(
                new LinkedHashSet<>(academician.getKeywords())
        );

        return new AcademicianResponse(
                academician.getId(),
                academician.getVersion(),
                academician.getFirstName(),
                academician.getLastName(),
                academician.getEmail(),
                academician.getPhone(),
                academician.getTitle(),
                keywords
        );
    }
}
