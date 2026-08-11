package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportRow;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcademicianImportPersistenceService {

    private final AcademicianRepository academicianRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(AcademicianImportRow row) {
        academicianRepository.saveAndFlush(toAcademician(row));
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
