package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ProjectDocumentTextExtractionService {

    public static final String PDF_CONTENT_TYPE = "application/pdf";
    public static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final String NO_PDF_TEXT_MESSAGE =
            "Belgeden metin cikarilamadi; belge taranmis goruntu olabilir.";

    public ExtractedProjectDocumentText extract(
            InputStream inputStream,
            String contentType
    ) {
        if (inputStream == null) {
            throw new InvalidRequestException("Belge bos olamaz");
        }

        String extracted = switch (contentType == null ? "" : contentType) {
            case PDF_CONTENT_TYPE -> extractPdf(inputStream);
            case DOCX_CONTENT_TYPE -> extractDocx(inputStream);
            default -> closeAndRejectUnsupported(inputStream);
        };
        String cleaned = clean(extracted);
        if (!containsMeaningfulText(cleaned)) {
            String message = PDF_CONTENT_TYPE.equals(contentType)
                    ? NO_PDF_TEXT_MESSAGE
                    : "DOCX belgesinden metin cikarilamadi";
            throw new InvalidRequestException(message);
        }
        return new ExtractedProjectDocumentText(contentType, cleaned);
    }

    private String extractPdf(InputStream inputStream) {
        try (InputStream input = inputStream) {
            byte[] bytes = readAll(input);
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (document.isEncrypted() && !document.getCurrentAccessPermission().canExtractContent()) {
                    throw new InvalidRequestException("Sifreli PDF belgesi okunamiyor");
                }
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(false);
                return stripper.getText(document);
            }
        } catch (InvalidRequestException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new InvalidRequestException("PDF belgesi okunamadi");
        }
    }

    private String extractDocx(InputStream inputStream) {
        try (InputStream input = inputStream;
             XWPFDocument document = new XWPFDocument(input)) {
            StringBuilder text = new StringBuilder();
            appendBodyElements(document.getBodyElements(), text);
            return text.toString();
        } catch (IOException | RuntimeException exception) {
            throw new InvalidRequestException("DOCX belgesi okunamadi");
        }
    }

    private void appendBodyElements(List<IBodyElement> elements, StringBuilder text) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph paragraph) {
                appendLine(text, paragraph.getText());
            } else if (element instanceof XWPFTable table) {
                appendTable(table, text);
            }
        }
    }

    private void appendTable(XWPFTable table, StringBuilder text) {
        for (XWPFTableRow row : table.getRows()) {
            boolean firstCell = true;
            for (XWPFTableCell cell : row.getTableCells()) {
                if (!firstCell && text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
                    text.append(" | ");
                }
                appendBodyElements(cell.getBodyElements(), text);
                firstCell = false;
            }
            ensureNewline(text);
        }
    }

    private void appendLine(StringBuilder text, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        text.append(value).append('\n');
    }

    private void ensureNewline(StringBuilder text) {
        if (text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
            text.append('\n');
        }
    }

    private String closeAndRejectUnsupported(InputStream inputStream) {
        try (InputStream ignored = inputStream) {
            throw new InvalidRequestException("Belge turu desteklenmiyor");
        } catch (IOException exception) {
            throw new InvalidRequestException("Belge turu desteklenmiyor");
        }
    }

    private byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        input.transferTo(output);
        return output.toByteArray();
    }

    private String clean(String value) {
        return value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\u0000", "")
                .replaceAll("(?m)[\\t \\x0B\\f]+$", "")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    private boolean containsMeaningfulText(String value) {
        return value.codePoints().anyMatch(Character::isLetterOrDigit);
    }
}
