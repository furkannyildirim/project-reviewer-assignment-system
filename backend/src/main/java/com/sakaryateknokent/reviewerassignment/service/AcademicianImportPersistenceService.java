package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportRow;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportClassifier.AcademicianImportDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
public class AcademicianImportPersistenceService {

    private final AcademicianRepository academicianRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(AcademicianImportRow row) {
        academicianRepository.saveAndFlush(toAcademician(row));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void update(AcademicianImportDecision decision) {
        Academician existing = decision.existingAcademician();
        Academician academician = existing.getId() == null
                ? existing
                : academicianRepository.findById(existing.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Akademisyen bulunamadi: " + existing.getId()
                ));

        if (existing.getVersion() != null
                && !existing.getVersion().equals(academician.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(
                    Academician.class,
                    academician.getId()
            );
        }

        applyUpdate(academician, decision);
        academicianRepository.saveAndFlush(academician);
    }

    private void applyUpdate(
            Academician academician,
            AcademicianImportDecision decision
    ) {
        AcademicianImportRow row = decision.row();

        if (academician.getEmail() == null && row.getEmail() != null) {
            academician.setEmail(AcademicianTextNormalizer.normalizeEmail(
                    row.getEmail()
            ));
        }
        if (academician.getPhone() == null && row.getPhone() != null) {
            academician.setPhone(AcademicianTextNormalizer.normalizeOptionalText(
                    row.getPhone()
            ));
        }
        academician.refreshCanonicalIdentity();

        academician.getKeywords().clear();
        academician.getKeywords().addAll(new LinkedHashSet<>(
                decision.mergedKeywords()
        ));
    }

    private Academician toAcademician(AcademicianImportRow row) {
        Academician academician = new Academician();
        academician.setFirstName(normalizeRequiredText(row.getFirstName()));
        academician.setLastName(normalizeRequiredText(row.getLastName()));
        academician.setEmail(AcademicianTextNormalizer.normalizeEmail(
                row.getEmail()
        ));
        academician.setPhone(AcademicianTextNormalizer.normalizeOptionalText(
                row.getPhone()
        ));
        academician.setTitle(AcademicianTextNormalizer.normalizeOptionalText(
                row.getTitle()
        ));
        academician.getKeywords().addAll(
                AcademicianTextNormalizer.normalizeKeywords(
                        row.getKeywords()
                )
        );
        academician.refreshCanonicalIdentity();

        return academician;
    }

    private String normalizeRequiredText(String value) {
        return AcademicianTextNormalizer.normalizeWhitespace(value);
    }
}
