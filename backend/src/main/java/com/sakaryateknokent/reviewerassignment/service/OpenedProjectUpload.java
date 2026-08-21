package com.sakaryateknokent.reviewerassignment.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public record OpenedProjectUpload(
        ProjectUploadInfo metadata,
        InputStream inputStream
) implements AutoCloseable {

    @Override
    public void close() {
        try {
            inputStream.close();
        } catch (IOException exception) {
            throw new UncheckedIOException("Gecici proje dosyasi kapatilamadi", exception);
        }
    }
}
