package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.ProjectFormField;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record ParsedProjectForm(
        String templateCode,
        String companyName,
        String name,
        String projectManager,
        LocalDate startDate,
        Integer durationMonths,
        String category,
        String sector,
        String summary,
        String researchAndDevelopmentNature,
        String innovativeAspect,
        String mainTechnologyArea,
        String projectDetail,
        Set<String> keywords,
        Set<ProjectFormField> missingFields,
        Set<ProjectFormField> reviewRequiredFields,
        List<String> warnings
) {
}
