package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectDocumentTextExtractionServiceTest {

    static {
        System.setProperty("pdfbox.fontcache", "target/pdfbox-font-cache");
    }

    private final ProjectDocumentTextExtractionService service =
            new ProjectDocumentTextExtractionService();

    @Test
    void extractsNormalDocxParagraphAndPreservesTurkishCharacters() {
        byte[] docx = docx(document -> document.createParagraph().createRun()
                .setText("Proje Özeti: Yapay zekâ çözümü İŞĞÜÇ"));

        ExtractedProjectDocumentText result = service.extract(
                new ByteArrayInputStream(docx),
                ProjectDocumentTextExtractionService.DOCX_CONTENT_TYPE);

        assertTrue(result.extractedText().contains(
                "Proje Özeti: Yapay zekâ çözümü İŞĞÜÇ"));
        assertEquals(ProjectDocumentTextExtractionService.DOCX_CONTENT_TYPE,
                result.contentType());
    }

    @Test
    void extractsDocxTableCellsAndNestedTableWithoutDuplicates() {
        byte[] docx = docx(document -> {
            XWPFTable table = document.createTable(1, 2);
            setCellText(table.getRow(0).getCell(0), "Proje Adı");
            XWPFTableCell valueCell = table.getRow(0).getCell(1);
            setCellText(valueCell, "Akıllı Üretim");
            CTTbl nestedXml = valueCell.getCTTc().addNewTbl();
            XWPFTable nested = new XWPFTable(nestedXml, valueCell);
            nested.createRow().createCell().setText("İç Tablo Değeri");
        });

        String text = service.extract(new ByteArrayInputStream(docx),
                ProjectDocumentTextExtractionService.DOCX_CONTENT_TYPE).extractedText();

        assertTrue(text.contains("Proje Adı"));
        assertTrue(text.contains("Akıllı Üretim"));
        assertTrue(text.contains("İç Tablo Değeri"));
        assertEquals(1, occurrences(text, "Proje Adı"));
    }

    @Test
    void extractsEmbeddedPdfTextAndPreservesPageOrder() {
        byte[] pdf = pdf("FIRST PAGE", "SECOND PAGE");

        String text = service.extract(new ByteArrayInputStream(pdf),
                ProjectDocumentTextExtractionService.PDF_CONTENT_TYPE).extractedText();

        assertTrue(text.contains("FIRST PAGE"));
        assertTrue(text.contains("SECOND PAGE"));
        assertTrue(text.indexOf("FIRST PAGE") < text.indexOf("SECOND PAGE"));
    }

    @Test
    void rejectsPdfWithoutMeaningfulText() {
        assertThrows(InvalidRequestException.class, () -> service.extract(
                new ByteArrayInputStream(pdf()),
                ProjectDocumentTextExtractionService.PDF_CONTENT_TYPE));
    }

    @Test
    void rejectsEmptyDocx() {
        byte[] docx = docx(document -> { });

        assertThrows(InvalidRequestException.class, () -> service.extract(
                new ByteArrayInputStream(docx),
                ProjectDocumentTextExtractionService.DOCX_CONTENT_TYPE));
    }

    @Test
    void rejectsCorruptPdfAndDocxWithControlledErrors() {
        InvalidRequestException pdfError = assertThrows(InvalidRequestException.class,
                () -> service.extract(new ByteArrayInputStream("broken".getBytes()),
                        ProjectDocumentTextExtractionService.PDF_CONTENT_TYPE));
        InvalidRequestException docxError = assertThrows(InvalidRequestException.class,
                () -> service.extract(new ByteArrayInputStream("broken".getBytes()),
                        ProjectDocumentTextExtractionService.DOCX_CONTENT_TYPE));

        assertEquals("PDF belgesi okunamadi", pdfError.getMessage());
        assertEquals("DOCX belgesi okunamadi", docxError.getMessage());
    }

    @Test
    void rejectsUnsupportedContentType() {
        assertThrows(InvalidRequestException.class, () -> service.extract(
                new ByteArrayInputStream("text".getBytes()), "text/plain"));
    }

    @Test
    void closesInputStreamsOnSuccessAndFailure() {
        CloseTrackingInputStream successful = new CloseTrackingInputStream(
                docx(document -> document.createParagraph().createRun().setText("Metin")));
        CloseTrackingInputStream failed = new CloseTrackingInputStream("broken".getBytes());

        service.extract(successful,
                ProjectDocumentTextExtractionService.DOCX_CONTENT_TYPE);
        assertThrows(InvalidRequestException.class, () -> service.extract(failed,
                ProjectDocumentTextExtractionService.PDF_CONTENT_TYPE));

        assertTrue(successful.closed);
        assertTrue(failed.closed);
    }

    @Test
    void resultModelDoesNotExposePathOrStorageKey() {
        List<String> fields = Arrays.stream(
                        ExtractedProjectDocumentText.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertEquals(List.of("contentType", "extractedText"), fields);
        assertFalse(fields.contains("path"));
        assertFalse(fields.contains("storageKey"));
    }

    private byte[] docx(DocumentWriter writer) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            writer.write(document);
            document.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private byte[] pdf(String... pages) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String pageText : pages) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(pageText);
                    content.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private void setCellText(XWPFTableCell cell, String value) {
        cell.getParagraphs().getFirst().createRun().setText(value);
    }

    private int occurrences(String value, String needle) {
        return (value.length() - value.replace(needle, "").length()) / needle.length();
    }

    @FunctionalInterface
    private interface DocumentWriter {
        void write(XWPFDocument document);
    }

    private static class CloseTrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private CloseTrackingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
