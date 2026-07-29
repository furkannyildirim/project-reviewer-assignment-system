package com.sakaryateknokent.reviewerassignment.importing;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AcademicianExcelImportParser {

    private static final int HEADER_SEARCH_ROW_LIMIT = 10;
    private static final int MAX_FIRST_NAME_LENGTH = 100;
    private static final int MAX_LAST_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_PHONE_LENGTH = 30;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_KEYWORD_LENGTH = 255;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );
    private static final String COMBINED_NAME_WARNING_MESSAGE =
            "Birleşik ad soyad alanları otomatik ayrıştırılmıştır. "
                    + "Gerekirse aktarımdan sonra düzenleyebilirsiniz.";

    private static final Map<String, HeaderType> HEADER_ALIASES =
            createHeaderAliases();
    private static final List<TitleAlias> TITLE_ALIASES =
            createTitleAliases();

    private final DataFormatter dataFormatter = new DataFormatter();

    public AcademicianImportPreview parse(
            String fileName,
            InputStream inputStream
    ) {
        if (fileName == null
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new InvalidRequestException(
                    "Yalnizca gecerli .xlsx dosyalari yuklenebilir"
            );
        }

        AcademicianImportPreview preview = new AcademicianImportPreview();
        preview.setFileName(fileName);

        try (InputStream stream = inputStream;
             Workbook workbook = WorkbookFactory.create(stream)) {
            parseWorkbook(workbook, preview);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof InvalidRequestException invalidRequest) {
                throw invalidRequest;
            }

            throw new InvalidRequestException(
                    "Excel dosyasi okunamadi"
            );
        }

        if (preview.getSheetCount() == 0 || preview.getRows().isEmpty()) {
            throw new InvalidRequestException(
                    "Excel dosyasinda aktarilabilir akademisyen satiri bulunamadi"
            );
        }

        return preview;
    }

    private void parseWorkbook(
            Workbook workbook,
            AcademicianImportPreview preview
    ) {
        boolean headerFound = false;
        boolean validNameStructureFound = false;
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
            HeaderMapping mapping = headerMapping.get();
            validNameStructureFound |= mapping.hasValidNameStructure();

            if (!mapping.hasValidNameStructure()) {
                continue;
            }

            parseRows(sheet, mapping, preview);
        }

        preview.setSheetCount(visibleSheetCount);

        if (!headerFound) {
            throw new InvalidRequestException(
                    "Excel dosyasinda taninabilir baslik satiri bulunamadi"
            );
        }

        if (!validNameStructureFound) {
            throw new InvalidRequestException(
                    "Excel dosyasinda akademisyen adi ve soyadi icin uygun baslik bulunamadi"
            );
        }
    }

    private Optional<HeaderMapping> findHeaderMapping(
            Sheet sheet,
            AcademicianImportPreview preview
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

            HeaderMapping mapping = readHeaderMapping(row, preview);
            if (mapping.hasValidNameStructure()) {
                return Optional.of(mapping);
            }
            if (firstRecognizedMapping == null
                    && mapping.hasAnyKnownHeader()) {
                firstRecognizedMapping = mapping;
            }
        }

        return Optional.ofNullable(firstRecognizedMapping);
    }

    private HeaderMapping readHeaderMapping(
            Row row,
            AcademicianImportPreview preview
    ) {
        Map<HeaderType, Integer> columns = new LinkedHashMap<>();
        Set<Integer> ignoredColumns = new LinkedHashSet<>();

        for (Cell cell : row) {
            String value = formattedCellValue(cell);
            if (value == null) {
                continue;
            }

            HeaderType headerType = HEADER_ALIASES.get(normalizeHeader(value));
            if (headerType == null) {
                preview.getIgnoredHeaders().add(value.trim());
                ignoredColumns.add(cell.getColumnIndex());
                continue;
            }

            preview.getDetectedHeaders().add(value.trim());
            if (headerType == HeaderType.PROJECTS) {
                preview.getIgnoredHeaders().add(value.trim());
                ignoredColumns.add(cell.getColumnIndex());
            } else {
                columns.putIfAbsent(headerType, cell.getColumnIndex());
            }
        }

        return new HeaderMapping(row.getRowNum(), columns, ignoredColumns);
    }

    private void parseRows(
            Sheet sheet,
            HeaderMapping mapping,
            AcademicianImportPreview preview
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

            if (isCombinedNameContinuation(sheet, rowIndex, mapping)) {
                continue;
            }

            AcademicianImportRow importRow =
                    new AcademicianImportRow(sheetName, rowIndex + 1);

            fillNameAndTitle(sheet, row, mapping, importRow, preview);
            importRow.setEmail(AcademicianTextNormalizer.normalizeEmail(
                    cellValue(sheet, row, mapping.column(HeaderType.EMAIL))
            ));
            importRow.setPhone(AcademicianTextNormalizer.normalizeOptionalText(
                    cellValue(sheet, row, mapping.column(HeaderType.PHONE))
            ));

            if (mapping.column(HeaderType.TITLE) != null) {
                importRow.setTitle(
                        AcademicianTextNormalizer.normalizeOptionalText(
                                cellValue(
                                        sheet,
                                        row,
                                        mapping.column(HeaderType.TITLE)
                                )
                        )
                );
            }

            importRow.setKeywords(parseKeywords(
                    cellValue(sheet, row, mapping.column(HeaderType.KEYWORDS))
            ));
            validateRow(importRow);
            preview.getRows().add(importRow);
        }
    }

    private void fillNameAndTitle(
            Sheet sheet,
            Row row,
            HeaderMapping mapping,
            AcademicianImportRow importRow,
            AcademicianImportPreview preview
    ) {
        Integer firstNameColumn = mapping.column(HeaderType.FIRST_NAME);
        Integer lastNameColumn = mapping.column(HeaderType.LAST_NAME);

        if (firstNameColumn != null && lastNameColumn != null) {
            importRow.setFirstName(AcademicianTextNormalizer
                    .normalizeOptionalText(cellValue(sheet, row, firstNameColumn)));
            importRow.setLastName(AcademicianTextNormalizer
                    .normalizeOptionalText(cellValue(sheet, row, lastNameColumn)));
            return;
        }

        String combinedName = AcademicianTextNormalizer.normalizeOptionalText(
                cellValue(sheet, row, mapping.column(HeaderType.COMBINED_NAME))
        );
        if (combinedName == null) {
            importRow.getErrors().add("Ad soyad bilgisi bos");
            return;
        }

        ParsedName parsedName = parseCombinedName(combinedName);
        if (parsedName == null) {
            importRow.getErrors().add("Ad soyad bilgisi ayrisitirilamadi");
            return;
        }

        importRow.setFirstName(parsedName.firstName());
        importRow.setLastName(parsedName.lastName());
        if (mapping.column(HeaderType.TITLE) == null) {
            importRow.setTitle(parsedName.title());
        }

        if (!preview.getGeneralWarnings().contains(
                COMBINED_NAME_WARNING_MESSAGE
        )) {
            preview.getGeneralWarnings().add(
                    COMBINED_NAME_WARNING_MESSAGE
            );
        }
    }

    private ParsedName parseCombinedName(String combinedName) {
        String remaining = AcademicianTextNormalizer
                .normalizeWhitespace(combinedName);
        String title = null;
        String normalizedRemaining = normalizeTitleLookup(remaining);

        for (TitleAlias titleAlias : TITLE_ALIASES) {
            if (normalizedRemaining.equals(titleAlias.lookup())
                    || normalizedRemaining.startsWith(
                    titleAlias.lookup() + " "
            )) {
                title = titleAlias.standardTitle();
                remaining = AcademicianTextNormalizer.normalizeWhitespace(
                        remaining.substring(titleAlias.sourceLength())
                );
                break;
            }
        }

        String[] parts = remaining.split(" ");
        if (parts.length < 2) {
            return null;
        }

        String lastName = parts[parts.length - 1];
        String firstName = remaining.substring(
                0,
                remaining.length() - lastName.length()
        ).trim();

        return new ParsedName(firstName, lastName, title);
    }

    private Set<String> parseKeywords(String value) {
        if (value == null || value.isBlank()) {
            return new LinkedHashSet<>();
        }

        Set<String> keywords = new LinkedHashSet<>();
        for (String keyword : value.split("[,;\\r\\n]+")) {
            keywords.add(keyword);
        }

        return AcademicianTextNormalizer.normalizeKeywords(keywords);
    }

    private void validateRow(AcademicianImportRow row) {
        validateRequiredLength(
                row.getFirstName(),
                "Ad zorunludur",
                "Ad en fazla 100 karakter olabilir",
                MAX_FIRST_NAME_LENGTH,
                row
        );
        validateRequiredLength(
                row.getLastName(),
                "Soyad zorunludur",
                "Soyad en fazla 100 karakter olabilir",
                MAX_LAST_NAME_LENGTH,
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
                "Telefon en fazla 30 karakter olabilir",
                MAX_PHONE_LENGTH,
                row
        );
        validateOptionalLength(
                row.getTitle(),
                "Unvan en fazla 100 karakter olabilir",
                MAX_TITLE_LENGTH,
                row
        );

        for (String keyword : row.getKeywords()) {
            if (keyword.length() > MAX_KEYWORD_LENGTH) {
                row.getErrors().add(
                        "Anahtar kelime en fazla 255 karakter olabilir"
                );
                break;
            }
        }
    }

    private void validateRequiredLength(
            String value,
            String requiredMessage,
            String lengthMessage,
            int maxLength,
            AcademicianImportRow row
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
            AcademicianImportRow row
    ) {
        if (value != null && value.length() > maxLength) {
            row.getErrors().add(lengthMessage);
        }
    }

    private boolean isRowBlank(Row row, HeaderMapping mapping) {
        for (Integer columnIndex : mapping.dataColumns()) {
            Cell cell = row.getCell(columnIndex);
            if (formattedCellValue(cell) != null) {
                return false;
            }
        }

        return true;
    }

    private boolean isCombinedNameContinuation(
            Sheet sheet,
            int rowIndex,
            HeaderMapping mapping
    ) {
        Integer combinedNameColumn = mapping.column(HeaderType.COMBINED_NAME);
        if (combinedNameColumn == null) {
            return false;
        }

        CellRangeAddress region = mergedRegionAt(
                sheet,
                rowIndex,
                combinedNameColumn
        );

        return region != null
                && (region.getFirstRow() != rowIndex
                || region.getFirstColumn() != combinedNameColumn);
    }

    private String cellValue(Sheet sheet, Row row, Integer columnIndex) {
        if (row == null || columnIndex == null) {
            return null;
        }

        CellRangeAddress region = mergedRegionAt(
                sheet,
                row.getRowNum(),
                columnIndex
        );
        if (region != null) {
            Row anchorRow = sheet.getRow(region.getFirstRow());
            Cell anchorCell = anchorRow == null
                    ? null
                    : anchorRow.getCell(region.getFirstColumn());
            return formattedCellValue(anchorCell);
        }

        return formattedCellValue(row.getCell(columnIndex));
    }

    private CellRangeAddress mergedRegionAt(
            Sheet sheet,
            int rowIndex,
            int columnIndex
    ) {
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.isInRange(rowIndex, columnIndex)) {
                return region;
            }
        }

        return null;
    }

    private String formattedCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        String value = dataFormatter.formatCellValue(cell);
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private static Map<String, HeaderType> createHeaderAliases() {
        Map<String, HeaderType> aliases = new HashMap<>();
        addAliases(
                aliases,
                HeaderType.COMBINED_NAME,
                "ADI SOYADI",
                "AD SOYAD",
                "AKADEMISYEN"
        );
        addAliases(aliases, HeaderType.FIRST_NAME, "AD", "ADI");
        addAliases(aliases, HeaderType.LAST_NAME, "SOYAD", "SOYADI");
        addAliases(aliases, HeaderType.TITLE, "UNVAN", "ÜNVAN");
        addAliases(
                aliases,
                HeaderType.EMAIL,
                "E-POSTA",
                "EPOSTA",
                "EMAIL",
                "MAIL",
                "MAİL"
        );
        addAliases(
                aliases,
                HeaderType.PHONE,
                "TELEFON",
                "GSM",
                "CEP TELEFONU"
        );
        addAliases(
                aliases,
                HeaderType.KEYWORDS,
                "ANAHTAR KELIMELER",
                "ANAHTAR KELİMELER",
                "ANAHTAR KELIMELERI",
                "ANAHTAR KELİMELERİ",
                "KEYWORDS",
                "UZMANLIK ALANLARI"
        );
        addAliases(aliases, HeaderType.PROJECTS, "PROJELERI", "PROJELERİ",
                "PROJELER");
        return aliases;
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

    private static List<TitleAlias> createTitleAliases() {
        List<TitleAlias> aliases = new ArrayList<>();
        addTitle(aliases, "Prof.Dr.", "Prof. Dr.");
        addTitle(aliases, "Prof. Dr.", "Prof. Dr.");
        addTitle(aliases, "Doç.Dr.", "Doç. Dr.");
        addTitle(aliases, "Doç. Dr.", "Doç. Dr.");
        addTitle(aliases, "Dr. Öğr. Üyesi", "Dr. Öğr. Üyesi");
        addTitle(aliases, "Dr.Öğr.Üyesi", "Dr. Öğr. Üyesi");
        addTitle(aliases, "Öğr. Gör. Dr.", "Öğr. Gör. Dr.");
        addTitle(aliases, "Arş. Gör. Dr.", "Arş. Gör. Dr.");
        addTitle(aliases, "Öğr. Gör.", "Öğr. Gör.");
        addTitle(aliases, "Arş. Gör.", "Arş. Gör.");
        addTitle(aliases, "Dr.", "Dr.");
        aliases.sort(
                Comparator
                        .comparingInt(
                                (TitleAlias alias) -> alias.lookup().length()
                        )
                        .thenComparingInt(TitleAlias::sourceLength)
                        .reversed()
        );
        return List.copyOf(aliases);
    }

    private static void addTitle(
            List<TitleAlias> aliases,
            String source,
            String standardTitle
    ) {
        aliases.add(new TitleAlias(
                normalizeTitleLookup(source),
                source.length(),
                standardTitle
        ));
    }

    private static String normalizeTitleLookup(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        normalized = normalized
                .replace('ı', 'i')
                .replace('I', 'i')
                .replaceAll("[.]+", ".")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        normalized = normalized.replace(".", ". ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private enum HeaderType {
        COMBINED_NAME,
        FIRST_NAME,
        LAST_NAME,
        TITLE,
        EMAIL,
        PHONE,
        KEYWORDS,
        PROJECTS
    }

    private record HeaderMapping(
            int headerRowIndex,
            Map<HeaderType, Integer> columns,
            Set<Integer> ignoredColumns
    ) {

        private boolean hasAnyKnownHeader() {
            return !columns.isEmpty() || !ignoredColumns.isEmpty();
        }

        private boolean hasValidNameStructure() {
            return (columns.containsKey(HeaderType.FIRST_NAME)
                    && columns.containsKey(HeaderType.LAST_NAME))
                    || columns.containsKey(HeaderType.COMBINED_NAME);
        }

        private Integer column(HeaderType type) {
            return columns.get(type);
        }

        private Set<Integer> dataColumns() {
            return new LinkedHashSet<>(columns.values());
        }
    }

    private record TitleAlias(
            String lookup,
            int sourceLength,
            String standardTitle
    ) {
    }

    private record ParsedName(
            String firstName,
            String lastName,
            String title
    ) {
    }
}
