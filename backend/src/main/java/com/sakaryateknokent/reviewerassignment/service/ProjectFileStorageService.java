package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.config.ProjectFileStorageProperties;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ProjectFileStorageService {

    private static final String TEMPORARY_AREA = "temporary";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final Path root;
    private final Path temporaryRoot;
    private final long maxSize;

    public ProjectFileStorageService(ProjectFileStorageProperties properties) {
        this.root = properties.path().toAbsolutePath().normalize();
        this.temporaryRoot = root.resolve(TEMPORARY_AREA);
        this.maxSize = properties.maxSize().toBytes();
        createStorageDirectories();
    }

    public StoredProjectFile storeTemporary(
            String originalFileName,
            InputStream content
    ) {
        String cleanedFileName = cleanOriginalFileName(originalFileName);
        String extension = extensionOf(cleanedFileName);
        byte[] bytes = readLimited(content);
        String contentType = validateContent(extension, bytes);

        UUID fileId = UUID.randomUUID();
        String storageKey = TEMPORARY_AREA + "/" + fileId + "." + extension;
        Path target = resolveStorageKey(storageKey);
        try {
            Files.write(target, bytes);
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new UncheckedIOException("Gecici proje dosyasi yazilamadi", exception);
        }
        return new StoredProjectFile(fileId, storageKey, cleanedFileName,
                contentType, bytes.length);
    }

    public void deleteTemporary(String storageKey) {
        deleteFromArea(storageKey, temporaryRoot);
    }

    public InputStream loadTemporary(String storageKey) {
        Path file = resolveStorageKey(storageKey);
        if (!file.startsWith(temporaryRoot)) {
            throw new InvalidRequestException("Gecici dosya anahtari gecersiz");
        }
        return openFile(file);
    }

    private InputStream openFile(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Proje dosyasi bulunamadi");
        }
        try {
            return Files.newInputStream(file);
        } catch (IOException exception) {
            throw new UncheckedIOException("Proje dosyasi okunamadi", exception);
        }
    }

    private void deleteFromArea(String storageKey, Path allowedRoot) {
        Path file = resolveStorageKey(storageKey);
        if (!file.startsWith(allowedRoot)) {
            throw new InvalidRequestException("Dosya anahtari gecersiz");
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new UncheckedIOException("Proje dosyasi silinemedi", exception);
        }
    }

    private void createStorageDirectories() {
        try {
            Files.createDirectories(temporaryRoot);
        } catch (IOException exception) {
            throw new UncheckedIOException("Proje dosya klasorleri olusturulamadi", exception);
        }
    }

    private byte[] readLimited(InputStream content) {
        if (content == null) {
            throw new InvalidRequestException("Dosya bos olamaz");
        }
        try (InputStream input = content; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxSize) {
                    throw new InvalidRequestException("Dosya boyutu izin verilen limiti asiyor");
                }
                output.write(buffer, 0, read);
            }
            if (total == 0) {
                throw new InvalidRequestException("Dosya bos olamaz");
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new InvalidRequestException("Dosya okunamadi");
        }
    }

    private String validateContent(String extension, byte[] content) {
        return switch (extension) {
            case "pdf" -> validatePdf(content);
            case "docx" -> validateDocx(content);
            default -> throw new InvalidRequestException("Yalnizca PDF veya DOCX dosyasi kabul edilir");
        };
    }

    private String validatePdf(byte[] content) {
        if (content.length < 5
                || content[0] != '%' || content[1] != 'P'
                || content[2] != 'D' || content[3] != 'F' || content[4] != '-') {
            throw new InvalidRequestException("PDF dosyasi gecersiz");
        }
        return PDF_CONTENT_TYPE;
    }

    private String validateDocx(byte[] content) {
        boolean hasContentTypes = false;
        boolean hasDocument = false;
        long expandedSize = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if ("[Content_Types].xml".equals(entry.getName())) {
                    hasContentTypes = true;
                }
                if ("word/document.xml".equals(entry.getName())) {
                    hasDocument = true;
                }
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    expandedSize += read;
                    if (expandedSize > maxSize * 10) {
                        throw new InvalidRequestException("DOCX dosyasi gecersiz");
                    }
                }
            }
        } catch (IOException exception) {
            throw new InvalidRequestException("DOCX dosyasi gecersiz");
        }
        if (!hasContentTypes || !hasDocument) {
            throw new InvalidRequestException("DOCX dosyasi gecersiz");
        }
        return DOCX_CONTENT_TYPE;
    }

    private String cleanOriginalFileName(String originalFileName) {
        if (originalFileName == null) {
            throw new InvalidRequestException("Dosya adi zorunludur");
        }
        String filename = originalFileName.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "").trim();
        if (filename.isBlank()) {
            throw new InvalidRequestException("Dosya adi gecersiz");
        }
        return filename.length() > 255 ? filename.substring(0, 255) : filename;
    }

    private String extensionOf(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 1 || lastDot == filename.length() - 1) {
            throw new InvalidRequestException("Dosya uzantisi gecersiz");
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private Path resolveStorageKey(String storageKey) {
        if (storageKey == null || !storageKey.matches(
                "^temporary/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(pdf|docx)$")) {
            throw new InvalidRequestException("Dosya anahtari gecersiz");
        }
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(temporaryRoot)) {
            throw new InvalidRequestException("Dosya anahtari gecersiz");
        }
        return resolved;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The original write failure is more useful to callers.
        }
    }
}
