package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.ProjectCompanyMatchStatus;

import java.util.List;

public record ProjectCompanyMatchResponse(
        ProjectCompanyMatchStatus status,
        ProjectCompanyCandidateResponse matchedCompany,
        List<ProjectCompanyCandidateResponse> candidates,
        String message
) {
}
