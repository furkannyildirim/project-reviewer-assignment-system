package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.ProjectCompanyMatchStatus;

import java.util.List;

record ProjectCompanyMatchResult(
        ProjectCompanyMatchStatus status,
        ProjectCompanyMatchCandidate matchedCompany,
        List<ProjectCompanyMatchCandidate> candidates,
        String message
) {
}
