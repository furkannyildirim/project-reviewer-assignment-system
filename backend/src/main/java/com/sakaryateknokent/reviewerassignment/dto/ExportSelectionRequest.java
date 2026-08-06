package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record ExportSelectionRequest(
        List<Long> ids
) {
}
