package com.sakaryateknokent.reviewerassignment.service;

import java.io.InputStream;

public record ProjectDocumentDownload(
        String originalFileName,
        String contentType,
        long fileSize,
        InputStream content
) {
}
