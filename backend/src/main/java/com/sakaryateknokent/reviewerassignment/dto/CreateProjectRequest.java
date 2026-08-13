package com.sakaryateknokent.reviewerassignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record CreateProjectRequest(
        @NotNull(message = "Sirket zorunludur") Long companyId,
        @NotBlank(message = "Proje adi bos birakilamaz")
        @Size(max = 255, message = "Proje adi en fazla 255 karakter olabilir") String name,
        @Size(max = 255, message = "Proje yoneticisi en fazla 255 karakter olabilir") String projectManager,
        @NotNull(message = "Baslangic tarihi zorunludur") LocalDate startDate,
        @NotNull(message = "Sure zorunludur") @Positive(message = "Sure pozitif olmalidir") Integer durationMonths,
        @Size(max = 255, message = "Kategori en fazla 255 karakter olabilir") String category,
        @Size(max = 255, message = "Sektor en fazla 255 karakter olabilir") String sector,
        String summary,
        String researchAndDevelopmentNature,
        String innovativeAspect,
        String mainTechnologyArea,
        String projectDetail,
        Set<@Size(max = 255, message = "Anahtar kelime en fazla 255 karakter olabilir") String> keywords
) {
}
