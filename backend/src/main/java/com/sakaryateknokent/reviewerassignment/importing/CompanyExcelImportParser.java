package com.sakaryateknokent.reviewerassignment.importing;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.service.AcademicianTextNormalizer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CompanyExcelImportParser {

    private static final int HEADER_SEARCH_ROW_LIMIT = 10;
    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_PHONE_LENGTH = 30;
    private static final int MAX_ACTIVITY_AREA_LENGTH = 2000;
    private static final int MAX_CONTACT_POSITION_LENGTH = 150;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );
    private static final Set<String> OPTIONAL_PLACEHOLDER_KEYS =
            createOptionalPlaceholderKeys();
    private static final Set<HeaderType> IGNORED_KNOWN_HEADER_TYPES =
            Set.of(
                    HeaderType.NO,
                    HeaderType.BUILDING,
                    HeaderType.WEB,
                    HeaderType.CITY,
                    HeaderType.DISTRICT,
                    HeaderType.OSB,
                    HeaderType.ADDRESS
            );
    private static final String UNUSED_COMPANY_COLUMNS_MESSAGE =
            "No, Bina ve Web sutunlari bu aktarimda kullanilmayacaktir.";
    private static final Map<String, HeaderType> HEADER_ALIASES =
            createHeaderAliases();

    private final DataFormatter dataFormatter = new DataFormatter();

    public CompanyImportPreview parse(
            String fileName,
            InputStream inputStream
    ) {
        return parse(fileName, inputStream, CompanyType.TGB);
    }

    public CompanyImportPreview parse(
            String fileName,
            InputStream inputStream,
            CompanyType companyType
    ) {
        if (fileName == null
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new InvalidRequestException(
                    "Yalnizca gecerli .xlsx dosyalari yuklenebilir"
            );
        }

        CompanyImportPreview preview = new CompanyImportPreview();
        preview.setFileName(fileName);

        try (InputStream stream = inputStream;
             Workbook workbook = WorkbookFactory.create(stream)) {
            parseWorkbook(workbook, preview, companyType);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof InvalidRequestException invalidRequest) {
                throw invalidRequest;
            }

            throw new InvalidRequestException("Excel dosyasi okunamadi");
        }

        if (preview.getSheetCount() == 0 || preview.getRows().isEmpty()) {
            throw new InvalidRequestException(
                    "Excel dosyasinda aktarilabilir sirket satiri bulunamadi"
            );
        }

        validateDuplicateEmailsInsideFile(preview.getRows());
        addUnusedColumnMessage(preview);

        return preview;
    }

    private void parseWorkbook(
            Workbook workbook,
            CompanyImportPreview preview,
            CompanyType companyType
    ) {
        boolean headerFound = false;
        int visibleSheetCount = 0;

        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets();
             sheetIndex++) {
            if (workbook.isSheetHidden(sheetIndex)
                    || workbook.isSheetVeryHidden(sheetIndex)) {
                continue;
            }

            visibleSheetCount++;
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            Optional<HeaderMapping> headerMapping =
                    findHeaderMapping(sheet, preview);

            if (headerMapping.isEmpty()) {
                continue;
            }

            headerFound = true;
            parseRows(sheet, headerMapping.get(), preview, companyType);
        }

        preview.setSheetCount(visibleSheetCount);

        if (!headerFound) {
            throw new InvalidRequestException(
                    "Excel dosyasinda taninabilir sirket baslik satiri bulunamadi"
            );
        }
    }

    private Optional<HeaderMapping> findHeaderMapping(
            Sheet sheet,
            CompanyImportPreview preview
    ) {
        int lastCandidateRow = Math.min(
                sheet.getLastRowNum(),
                HEADER_SEARCH_ROW_LIMIT - 1
        );
        HeaderMapping firstRecognizedMapping = null;

        for (int rowIndex = 0; rowIndex <= lastCandidateRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            HeaderMapping mapping = readHeaderMapping(row);
            if (mapping.hasRequiredHeaders()) {
                addHeadersToPreview(preview, mapping);
                return Optional.of(mapping);
            }
            if (firstRecognizedMapping == null
                    && mapping.hasAnyKnownHeader()) {
                firstRecognizedMapping = mapping;
            }
        }

        return Optional.ofNullable(firstRecognizedMapping)
                .filter(HeaderMapping::hasRequiredHeaders);
    }

    private HeaderMapping readHeaderMapping(Row row) {
        Map<HeaderType, ColumnRange> columns = new LinkedHashMap<>();
        Set<ColumnRange> ignoredColumns = new LinkedHashSet<>();
        Set<String> detectedHeaders = new LinkedHashSet<>();
        Set<String> ignoredHeaders = new LinkedHashSet<>();
        Set<String> processedMergedRegions = new LinkedHashSet<>();
        boolean knownHeaderFound = false;

        for (Cell cell : row) {
            CellRangeAddress mergedRegion = mergedRegionContaining(
                    row.getSheet(),
                    row.getRowNum(),
                    cell.getColumnIndex()
            );
            if (mergedRegion != null
                    && !processedMergedRegions.add(regionKey(mergedRegion))) {
                continue;
            }

            String value = formattedCellValue(cell);
            if (value == null) {
                continue;
            }

            ColumnRange columnRange = headerColumnRange(cell, mergedRegion);
            HeaderType headerType = HEADER_ALIASES.get(normalizeHeader(value));
            if (headerType == null) {
                ignoredHeaders.add(value.trim());
                ignoredColumns.add(columnRange);
                continue;
            }

            knownHeaderFound = true;
            if (IGNORED_KNOWN_HEADER_TYPES.contains(headerType)) {
                ignoredHeaders.add(value.trim());
                ignoredColumns.add(columnRange);
                continue;
            }

            detectedHeaders.add(value.trim());
            columns.putIfAbsent(headerType, columnRange);
        }

        return new HeaderMapping(
                row.getRowNum(),
                columns,
                ignoredColumns,
                detectedHeaders,
                ignoredHeaders,
                knownHeaderFound
        );
    }

    private void addHeadersToPreview(
            CompanyImportPreview preview,
            HeaderMapping mapping
    ) {
        preview.getDetectedHeaders().addAll(mapping.detectedHeaders());
        preview.getIgnoredHeaders().addAll(mapping.ignoredHeaders());
    }

    private void parseRows(
            Sheet sheet,
            HeaderMapping mapping,
            CompanyImportPreview preview,
            CompanyType companyType
    ) {
        String sheetName = sheet.getSheetName() == null
                ? ""
                : sheet.getSheetName().trim();

        for (int rowIndex = mapping.headerRowIndex() + 1;
             rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isRowBlank(row, mapping)) {
                continue;
            }

            CompanyImportRow importRow =
                    new CompanyImportRow(sheetName, rowIndex + 1);

            importRow.setName(normalizeOptionalImportText(
                    cellValue(row, mapping.columnRange(HeaderType.NAME))
            ));
            importRow.setPhone(normalizeOptionalImportText(
                    cellValue(row, mapping.columnRange(HeaderType.PHONE))
            ));
            importRow.setSecondaryPhone(
                    normalizeOptionalImportText(
                            cellValue(row, mapping.columnRange(HeaderType.SECONDARY_PHONE))
                    )
            );
            importRow.setEmail(normalizeImportEmail(
                    cellValue(row, mapping.columnRange(HeaderType.EMAIL))
            ));
            importRow.setActivityArea(resolveActivityArea(row, mapping));
            importRow.setContactFullName(
                    normalizeOptionalImportText(
                            cellValue(row, mapping.columnRange(HeaderType.CONTACT_NAME))
                    )
            );
            importRow.setContactPosition(
                    normalizeOptionalImportText(
                            cellValue(row, mapping.columnRange(HeaderType.CONTACT_POSITION))
                    )
            );
            importRow.setContactPhone(
                    normalizeOptionalImportText(
                            cellValue(row, mapping.columnRange(HeaderType.CONTACT_PHONE))
                    )
            );
            importRow.setContactEmail(normalizeImportEmail(
                    cellValue(row, mapping.columnRange(HeaderType.CONTACT_EMAIL))
            ));

            validateRow(importRow, companyType);
            preview.getRows().add(importRow);
        }
    }

    private String resolveActivityArea(Row row, HeaderMapping mapping) {
        String activityArea = normalizeOptionalImportText(
                cellValue(row, mapping.columnRange(HeaderType.ACTIVITY_AREA))
        );
        if (activityArea != null) {
            return activityArea;
        }

        return normalizeOptionalImportText(
                cellValue(row, mapping.columnRange(HeaderType.SECTOR))
        );
    }

    private String normalizeOptionalImportText(String value) {
        String normalized = AcademicianTextNormalizer.normalizeOptionalText(value);
        if (normalized == null || isOptionalPlaceholder(normalized)) {
            return null;
        }

        return normalized;
    }

    private String normalizeImportEmail(String value) {
        String normalized = normalizeOptionalImportText(value);
        if (normalized == null) {
            return null;
        }

        return AcademicianTextNormalizer.normalizeEmail(normalized);
    }

    private boolean isOptionalPlaceholder(String value) {
        String trimmed = value.trim();
        if (trimmed.equals("-") || trimmed.equals("--")) {
            return true;
        }

        return OPTIONAL_PLACEHOLDER_KEYS.contains(normalizeHeader(trimmed));
    }

    private void validateRow(CompanyImportRow row, CompanyType companyType) {
        validateRequiredLength(
                row.getName(),
                "Firma unvani zorunludur",
                "Firma unvani en fazla 200 karakter olabilir",
                MAX_NAME_LENGTH,
                row
        );
        validateOptionalLength(
                row.getEmail(),
                "E-posta en fazla 255 karakter olabilir",
                MAX_EMAIL_LENGTH,
                row
        );
        if (row.getEmail() != null
                && !EMAIL_PATTERN.matcher(row.getEmail()).matches()) {
            row.getErrors().add("E-posta formati gecersiz");
        }
        validateOptionalLength(
                row.getPhone(),
                "Telefon 1 en fazla 30 karakter olabilir",
                MAX_PHONE_LENGTH,
                row
        );
        validateOptionalLength(
                row.getSecondaryPhone(),
                "Telefon 2 en fazla 30 karakter olabilir",
                MAX_PHONE_LENGTH,
                row
        );
        validateOptionalLength(
                row.getActivityArea(),
                "Faaliyet alanı en fazla 2000 karakter olabilir.",
                MAX_ACTIVITY_AREA_LENGTH,
                row
        );
        validateOptionalLength(
                row.getContactFullName(),
                "Yetkili adi soyadi en fazla 200 karakter olabilir",
                MAX_NAME_LENGTH,
                row
        );
        validateOptionalLength(
                row.getContactPosition(),
                "Yetkili unvani en fazla 150 karakter olabilir",
                MAX_CONTACT_POSITION_LENGTH,
                row
        );
        validateOptionalLength(
                row.getContactPhone(),
                "Yetkili telefonu en fazla 30 karakter olabilir",
                MAX_PHONE_LENGTH,
                row
        );
        validateOptionalLength(
                row.getContactEmail(),
                "Yetkili e-postasi en fazla 255 karakter olabilir",
                MAX_EMAIL_LENGTH,
                row
        );
        if (row.getContactEmail() != null
                && !EMAIL_PATTERN.matcher(row.getContactEmail()).matches()) {
            row.getErrors().add("Yetkili e-postasi formati gecersiz");
        }
        if (CompanyType.EXTERNAL.equals(companyType)
                && row.getContactFullName() == null
                && hasAnyContactDetail(row)) {
            row.getWarnings().add(
                    "Yetkili adi soyadi bos oldugu icin yetkili bilgileri aktarilmayacak"
            );
        }
    }

    private boolean hasAnyContactDetail(CompanyImportRow row) {
        return row.getContactPosition() != null
                || row.getContactPhone() != null
                || row.getContactEmail() != null;
    }

    private void validateRequiredLength(
            String value,
            String requiredMessage,
            String lengthMessage,
            int maxLength,
            CompanyImportRow row
    ) {
        if (value == null || value.isBlank()) {
            row.getErrors().add(requiredMessage);
            return;
        }

        if (value.length() > maxLength) {
            row.getErrors().add(lengthMessage);
        }
    }

    private void validateOptionalLength(
            String value,
            String lengthMessage,
            int maxLength,
            CompanyImportRow row
    ) {
        if (value != null && value.length() > maxLength) {
            row.getErrors().add(lengthMessage);
        }
    }

    private boolean isRowBlank(Row row, HeaderMapping mapping) {
        for (ColumnRange columnRange : mapping.allColumnRanges()) {
            if (cellValue(row, columnRange) != null) {
                return false;
            }
        }

        return true;
    }

    private String cellValue(Row row, ColumnRange columnRange) {
        if (row == null || columnRange == null) {
            return null;
        }

        int anchorColumn = columnRange.startColumn();
        String directValue = directFormattedCellValue(row.getCell(anchorColumn));
        if (directValue != null) {
            return directValue;
        }

        CellRangeAddress mergedRegion = mergedRegionContaining(
                row.getSheet(),
                row.getRowNum(),
                anchorColumn
        );
        if (mergedRegion == null) {
            return null;
        }

        Row firstRow = row.getSheet().getRow(mergedRegion.getFirstRow());
        Cell firstCell = firstRow == null
                ? null
                : firstRow.getCell(mergedRegion.getFirstColumn());
        return directFormattedCellValue(firstCell);
    }

    private String formattedCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        String directValue = directFormattedCellValue(cell);
        if (directValue != null) {
            return directValue;
        }

        CellRangeAddress mergedRegion = mergedRegionContaining(
                cell.getSheet(),
                cell.getRowIndex(),
                cell.getColumnIndex()
        );
        if (mergedRegion == null) {
            return null;
        }

        Row firstRow = cell.getSheet().getRow(mergedRegion.getFirstRow());
        Cell firstCell = firstRow == null
                ? null
                : firstRow.getCell(mergedRegion.getFirstColumn());
        return directFormattedCellValue(firstCell);
    }

    private String directFormattedCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        String value = dataFormatter.formatCellValue(cell);
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private ColumnRange headerColumnRange(
            Cell cell,
            CellRangeAddress mergedRegion
    ) {
        if (mergedRegion == null
                || !mergedRegionContainsRow(mergedRegion, cell.getRowIndex())) {
            return new ColumnRange(cell.getColumnIndex(), cell.getColumnIndex());
        }

        return new ColumnRange(
                mergedRegion.getFirstColumn(),
                mergedRegion.getLastColumn()
        );
    }

    private CellRangeAddress mergedRegionContaining(
            Sheet sheet,
            int rowIndex,
            int columnIndex
    ) {
        for (CellRangeAddress mergedRegion : sheet.getMergedRegions()) {
            if (mergedRegion.isInRange(rowIndex, columnIndex)) {
                return mergedRegion;
            }
        }

        return null;
    }

    private boolean mergedRegionContainsRow(
            CellRangeAddress mergedRegion,
            int rowIndex
    ) {
        return mergedRegion.getFirstRow() <= rowIndex
                && rowIndex <= mergedRegion.getLastRow();
    }

    private String regionKey(CellRangeAddress mergedRegion) {
        return mergedRegion.getFirstRow() + ":"
                + mergedRegion.getLastRow() + ":"
                + mergedRegion.getFirstColumn() + ":"
                + mergedRegion.getLastColumn();
    }

    private static Map<String, HeaderType> createHeaderAliases() {
        Map<String, HeaderType> aliases = new HashMap<>();
        addAliases(aliases, HeaderType.NO, "No");
        addAliases(
                aliases,
                HeaderType.NAME,
                "Firma Ünvanı",
                "Firma Unvanı"
        );
        addAliases(
                aliases,
                HeaderType.PHONE,
                "Telefon 1",
                "Sirket Telefon",
                "Şirket Telefon",
                "Telefon"
        );
        addAliases(aliases, HeaderType.NAME, "Firma");
        addAliases(aliases, HeaderType.SECONDARY_PHONE, "Telefon 2");
        addAliases(
                aliases,
                HeaderType.EMAIL,
                "E-Posta",
                "Eposta",
                "Email"
        );
        addAliases(
                aliases,
                HeaderType.EMAIL,
                "Sirket E-Posta",
                "Şirket E-Posta"
        );
        addAliases(
                aliases,
                HeaderType.SECTOR,
                "Sektör",
                "Sektörü",
                "Sektor",
                "Sektoru"
        );
        addAliases(
                aliases,
                HeaderType.ACTIVITY_AREA,
                "Faaliyet Alanı",
                "Faaliyet Alani"
        );
        addAliases(aliases, HeaderType.BUILDING, "Bina");
        addAliases(aliases, HeaderType.WEB, "Web");
        addAliases(aliases, HeaderType.WEB, "Web Sitesi");
        addAliases(aliases, HeaderType.CITY, "Il", "İl");
        addAliases(aliases, HeaderType.DISTRICT, "Ilce", "İlçe");
        addAliases(aliases, HeaderType.OSB, "OSB");
        addAliases(aliases, HeaderType.ADDRESS, "Acik Adres", "Açık Adres");
        addAliases(aliases, HeaderType.CONTACT_NAME, "Yetkili Ad Soyad");
        addAliases(
                aliases,
                HeaderType.CONTACT_POSITION,
                "Yetkili Unvan",
                "Yetkili Ünvan"
        );
        addAliases(aliases, HeaderType.CONTACT_PHONE, "Yetkili Telefon");
        addAliases(aliases, HeaderType.CONTACT_EMAIL, "Yetkili E-Posta");
        return aliases;
    }

    private static Set<String> createOptionalPlaceholderKeys() {
        Set<String> placeholders = new LinkedHashSet<>();
        addPlaceholder(placeholders, "Bilgi Yok");
        addPlaceholder(placeholders, "Yok");
        addPlaceholder(placeholders, "Bilinmiyor");
        addPlaceholder(placeholders, "Belirtilmemiş");
        addPlaceholder(placeholders, "Mevcut Değil");
        addPlaceholder(placeholders, "N/A");
        addPlaceholder(placeholders, "NA");
        return Set.copyOf(placeholders);
    }

    private static void addPlaceholder(Set<String> placeholders, String value) {
        placeholders.add(normalizeHeader(value));
    }

    private void validateDuplicateEmailsInsideFile(
            java.util.List<CompanyImportRow> rows
    ) {
        Set<String> duplicateEmails = rows.stream()
                .map(CompanyImportRow::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .collect(Collectors.groupingBy(
                        email -> email,
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (duplicateEmails.isEmpty()) {
            return;
        }

        for (CompanyImportRow row : rows) {
            if (duplicateEmails.contains(row.getEmail())) {
                row.getErrors().add(
                        "Dosya icinde ayni e-posta birden fazla kullanilmis"
                );
            }
        }
    }

    private void addUnusedColumnMessage(CompanyImportPreview preview) {
        boolean hasUnusedCompanyColumn = preview.getIgnoredHeaders()
                .stream()
                .map(CompanyExcelImportParser::normalizeHeader)
                .anyMatch(header -> header.equals(normalizeHeader("No"))
                        || header.equals(normalizeHeader("Bina"))
                        || header.equals(normalizeHeader("Web")));

        if (hasUnusedCompanyColumn
                && !preview.getGeneralInformationMessages()
                .contains(UNUSED_COMPANY_COLUMNS_MESSAGE)) {
            preview.getGeneralInformationMessages()
                    .add(UNUSED_COMPANY_COLUMNS_MESSAGE);
        }
    }

    private static void addAliases(
            Map<String, HeaderType> aliases,
            HeaderType type,
            String... values
    ) {
        for (String value : values) {
            aliases.put(normalizeHeader(value), type);
        }
    }

    private static String normalizeHeader(String value) {
        String normalized = value
                .trim()
                .replace('ı', 'i')
                .replace('I', 'i');
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[\\p{Punct}\\u2010-\\u2015]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
        return normalized;
    }

    private enum HeaderType {
        NO,
        NAME,
        PHONE,
        SECONDARY_PHONE,
        EMAIL,
        SECTOR,
        ACTIVITY_AREA,
        BUILDING,
        WEB,
        CITY,
        DISTRICT,
        OSB,
        ADDRESS,
        CONTACT_NAME,
        CONTACT_POSITION,
        CONTACT_PHONE,
        CONTACT_EMAIL
    }

    private record HeaderMapping(
            int headerRowIndex,
            Map<HeaderType, ColumnRange> columns,
            Set<ColumnRange> ignoredColumns,
            Set<String> detectedHeaders,
            Set<String> ignoredHeaders,
            boolean knownHeaderFound
    ) {

        private boolean hasAnyKnownHeader() {
            return knownHeaderFound;
        }

        private boolean hasRequiredHeaders() {
            return columns.containsKey(HeaderType.NAME);
        }

        private ColumnRange columnRange(HeaderType type) {
            return columns.get(type);
        }

        private Set<ColumnRange> allColumnRanges() {
            Set<ColumnRange> allColumnRanges =
                    new LinkedHashSet<>(columns.values());
            allColumnRanges.addAll(ignoredColumns);
            return allColumnRanges;
        }
    }

    private record ColumnRange(
            int startColumn,
            int endColumn
    ) {

    }
}
