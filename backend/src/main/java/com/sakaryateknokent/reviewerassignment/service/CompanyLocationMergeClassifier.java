package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportRow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class CompanyLocationMergeClassifier {

    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_UPDATE = "UPDATE";
    public static final String STATUS_EXISTING = "EXISTING";
    public static final String STATUS_DUPLICATE = "DUPLICATE";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_INVALID = "INVALID";

    public LocationMergeDecision classify(
            CompanyImportRow row,
            Company existingCompany
    ) {
        if (existingCompany == null) {
            return new LocationMergeDecision(
                    STATUS_NEW,
                    false,
                    List.of(),
                    List.of()
            );
        }

        List<String> warnings = new ArrayList<>();
        List<String> updateMessages = new ArrayList<>();
        boolean update = false;

        update |= canFill(
                "Il",
                existingCompany.getCity(),
                row.getCity(),
                warnings,
                updateMessages
        );
        update |= canFill(
                "Ilce",
                existingCompany.getDistrict(),
                row.getDistrict(),
                warnings,
                updateMessages
        );
        update |= canFill(
                "OSB",
                existingCompany.getOsb(),
                row.getOsb(),
                warnings,
                updateMessages
        );
        update |= canFill(
                "Acik adres",
                existingCompany.getAddress(),
                row.getAddress(),
                warnings,
                updateMessages
        );
        update |= canFill(
                "Bina",
                existingCompany.getBuilding(),
                row.getBuilding(),
                warnings,
                updateMessages
        );

        if (update) {
            return new LocationMergeDecision(
                    STATUS_UPDATE,
                    true,
                    List.copyOf(warnings),
                    List.copyOf(updateMessages)
            );
        }

        if (!warnings.isEmpty()) {
            return new LocationMergeDecision(
                    STATUS_WARNING,
                    false,
                    List.copyOf(warnings),
                    List.of()
            );
        }

        return new LocationMergeDecision(
                STATUS_EXISTING,
                false,
                List.of(),
                List.of()
        );
    }

    public void applyUpdates(
            CompanyImportRow row,
            Company existingCompany
    ) {
        if (shouldFill(existingCompany.getCity(), row.getCity())) {
            existingCompany.setCity(row.getCity());
        }
        if (shouldFill(existingCompany.getDistrict(), row.getDistrict())) {
            existingCompany.setDistrict(row.getDistrict());
        }
        if (shouldFill(existingCompany.getOsb(), row.getOsb())) {
            existingCompany.setOsb(row.getOsb());
        }
        if (shouldFill(existingCompany.getAddress(), row.getAddress())) {
            existingCompany.setAddress(row.getAddress());
        }
        if (shouldFill(existingCompany.getBuilding(), row.getBuilding())) {
            existingCompany.setBuilding(row.getBuilding());
        }
    }

    private boolean canFill(
            String label,
            String databaseValue,
            String excelValue,
            List<String> warnings,
            List<String> updateMessages
    ) {
        String normalizedDatabaseValue =
                AcademicianTextNormalizer.normalizeOptionalText(databaseValue);
        String normalizedExcelValue =
                AcademicianTextNormalizer.normalizeOptionalText(excelValue);

        if (normalizedExcelValue == null) {
            return false;
        }

        if (normalizedDatabaseValue == null) {
            updateMessages.add(label + " bilgisi eklenecek: "
                    + normalizedExcelValue);
            return true;
        }

        if (!Objects.equals(normalizedDatabaseValue, normalizedExcelValue)) {
            warnings.add(label
                    + " bilgisi sistemde farkli bir deger iceriyor. Mevcut bilgi korunacaktir.");
        }

        return false;
    }

    private boolean shouldFill(String databaseValue, String excelValue) {
        return AcademicianTextNormalizer.normalizeOptionalText(databaseValue)
                == null
                && AcademicianTextNormalizer.normalizeOptionalText(excelValue)
                != null;
    }

    public record LocationMergeDecision(
            String status,
            boolean update,
            List<String> warnings,
            List<String> updateMessages
    ) {
    }
}
