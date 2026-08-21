package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.config.ProjectFileStorageProperties;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectFileStorageServiceTest {

    @TempDir
    Path storageRoot;

    @Test
    void storesValidPdfInTemporaryAreaUsingUuidKey() throws Exception {
        ProjectFileStorageService service = service(1024);

        StoredProjectFile stored = service.storeTemporary("proposal.pdf",
                new ByteArrayInputStream(pdfBytes()));

        assertEquals("application/pdf", stored.contentType());
        assertTrue(stored.storageKey().matches(
                "temporary/[0-9a-f-]{36}\\.pdf"));
        assertTrue(Files.isRegularFile(storageRoot.resolve(stored.storageKey())));
    }

    @Test
    void acceptsValidDocxBasedOnZipContents() {
        ProjectFileStorageService service = service(4096);

        StoredProjectFile stored = service.storeTemporary("proposal.docx",
                new ByteArrayInputStream(docxBytes()));

        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                stored.contentType());
        assertTrue(stored.storageKey().endsWith(".docx"));
    }

    @Test
    void rejectsFakePdfFakeDocxAndLegacyDoc() {
        ProjectFileStorageService service = service(4096);

        assertThrows(InvalidRequestException.class, () -> service.storeTemporary(
                "fake.pdf", new ByteArrayInputStream("not a pdf".getBytes())));
        assertThrows(InvalidRequestException.class, () -> service.storeTemporary(
                "fake.docx", new ByteArrayInputStream("not a zip".getBytes())));
        assertThrows(InvalidRequestException.class, () -> service.storeTemporary(
                "legacy.doc", new ByteArrayInputStream(pdfBytes())));
    }

    @Test
    void rejectsEmptyAndOversizedFiles() {
        ProjectFileStorageService service = service(10);

        assertThrows(InvalidRequestException.class, () -> service.storeTemporary(
                "empty.pdf", new ByteArrayInputStream(new byte[0])));
        assertThrows(InvalidRequestException.class, () -> service.storeTemporary(
                "large.pdf", new ByteArrayInputStream("%PDF-123456".getBytes())));
    }

    @Test
    void cleansTraversalFilenameWithoutEscapingTemporaryArea() {
        ProjectFileStorageService service = service(4096);

        StoredProjectFile stored = service.storeTemporary("../outside\\proposal.pdf",
                new ByteArrayInputStream(pdfBytes()));

        assertEquals("proposal.pdf", stored.originalFileName());
        assertTrue(storageRoot.resolve(stored.storageKey()).normalize()
                .startsWith(storageRoot.resolve("temporary")));
        assertFalse(Files.exists(storageRoot.getParent().resolve("proposal.pdf")));
    }

    @Test
    void loadsTemporaryFileByStorageKey() throws Exception {
        ProjectFileStorageService service = service(4096);
        StoredProjectFile temporary = service.storeTemporary("proposal.pdf",
                new ByteArrayInputStream(pdfBytes()));

        try (var loaded = service.loadTemporary(temporary.storageKey())) {
            assertArrayEquals(pdfBytes(), loaded.readAllBytes());
        }
    }

    @Test
    void rejectsInvalidOrNonTemporaryStorageKeys() {
        ProjectFileStorageService service = service(4096);

        assertThrows(InvalidRequestException.class,
                () -> service.loadTemporary("../secrets.pdf"));
        assertThrows(InvalidRequestException.class,
                () -> service.deleteTemporary("projects/00000000-0000-0000-0000-000000000011.pdf"));
    }

    @Test
    void deletesTemporaryFileIdempotently() {
        ProjectFileStorageService service = service(4096);
        StoredProjectFile stored = service.storeTemporary("proposal.pdf",
                new ByteArrayInputStream(pdfBytes()));

        service.deleteTemporary(stored.storageKey());

        assertFalse(Files.exists(storageRoot.resolve(stored.storageKey())));
        assertDoesNotThrow(() -> service.deleteTemporary(stored.storageKey()));
    }

    @Test
    void leavesNoTemporaryFileAfterFailedValidation() throws Exception {
        ProjectFileStorageService service = service(4096);

        assertThrows(InvalidRequestException.class, () -> service.storeTemporary(
                "fake.pdf", new ByteArrayInputStream("invalid".getBytes())));

        try (var files = Files.list(storageRoot.resolve("temporary"))) {
            assertEquals(0, files.count());
        }
    }

    private ProjectFileStorageService service(long maxBytes) {
        return new ProjectFileStorageService(new ProjectFileStorageProperties(
                storageRoot, DataSize.ofBytes(maxBytes), Duration.ofHours(24)));
    }

    private byte[] pdfBytes() {
        return "%PDF-1.7\nminimal".getBytes();
    }

    private byte[] docxBytes() {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document/>".getBytes());
            zip.closeEntry();
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
