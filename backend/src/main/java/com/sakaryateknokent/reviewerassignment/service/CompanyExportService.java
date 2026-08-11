package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContact;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactMethodRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyExportService {

    private static final Sort DEFAULT_COMPANY_SORT =
            Sort.by(Sort.Order.asc("name").ignoreCase());

    private static final String[] HEADERS = {
            "Firma",
            "\u015Eirket Tipi",
            "Faaliyet Alan\u0131",
            "Telefonlar",
            "E-postalar",
            "Yetkili Ad",
            "Yetkili \u00DCnvan\u0131",
            "Yetkili Telefon",
            "Yetkili E-posta"
    };

    private final CompanyRepository companyRepository;
    private final CompanyContactRepository companyContactRepository;
    private final CompanyContactMethodRepository contactMethodRepository;

    @Transactional(readOnly = true)
    public byte[] exportToExcel(
            CompanyType companyType,
            String search,
            String initial
    ) {
        String normalizedSearch = normalizeFilter(search);
        String normalizedInitial = normalizeFilter(initial);
        List<Company> companies = companyRepository
                .search(
                        companyType,
                        normalizedSearch,
                        normalizedInitial,
                        Pageable.unpaged(DEFAULT_COMPANY_SORT)
                )
                .getContent();

        Map<Long, List<CompanyContact>> contactsByCompanyId =
                findContactsByCompanyId(companies);
        Map<Long, List<CompanyContactMethod>> contactMethodsByCompanyId =
                findContactMethodsByCompanyId(companies);

        return writeWorkbook(companies, contactsByCompanyId, contactMethodsByCompanyId);
    }

    @Transactional(readOnly = true)
    public byte[] exportToExcel(CompanyType companyType, List<Long> ids) {
        List<Company> companies = findSelectedCompanies(companyType, ids);
        Map<Long, List<CompanyContact>> contactsByCompanyId =
                findContactsByCompanyId(companies);
        Map<Long, List<CompanyContactMethod>> contactMethodsByCompanyId =
                findContactMethodsByCompanyId(companies);

        return writeWorkbook(companies, contactsByCompanyId, contactMethodsByCompanyId);
    }

    private List<Company> findSelectedCompanies(
            CompanyType companyType,
            List<Long> ids
    ) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(ids);
        return companyRepository.findByCompanyTypeAndIdIn(
                companyType,
                uniqueIds,
                DEFAULT_COMPANY_SORT
        );
    }

    private Map<Long, List<CompanyContact>> findContactsByCompanyId(
            List<Company> companies
    ) {
        if (companies.isEmpty()) {
            return Map.of();
        }

        List<Long> companyIds = companies.stream()
                .map(Company::getId)
                .toList();

        return companyContactRepository.findByCompanyIdIn(companyIds)
                .stream()
                .collect(Collectors.groupingBy(
                        contact -> contact.getCompany().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<Long, List<CompanyContactMethod>> findContactMethodsByCompanyId(
            List<Company> companies
    ) {
        if (companies.isEmpty()) {
            return Map.of();
        }

        List<Long> companyIds = companies.stream()
                .map(Company::getId)
                .toList();

        return contactMethodRepository.findByCompanyIdIn(companyIds)
                .stream()
                .collect(Collectors.groupingBy(
                        contactMethod -> contactMethod.getCompany().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private byte[] writeWorkbook(
            List<Company> companies,
            Map<Long, List<CompanyContact>> contactsByCompanyId,
            Map<Long, List<CompanyContactMethod>> contactMethodsByCompanyId
    ) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("\u015Eirketler");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle wrapStyle = createWrapStyle(workbook);

            writeHeader(sheet, headerStyle);
            writeRows(sheet, companies, contactsByCompanyId,
                    contactMethodsByCompanyId, wrapStyle);
            applyColumnWidths(sheet);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "\u015Eirket Excel dosyas\u0131 \u00FCretilemedi",
                    exception
            );
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    private CellStyle createWrapStyle(Workbook workbook) {
        CellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        return wrapStyle;
    }

    private void writeHeader(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);

        for (int columnIndex = 0; columnIndex < HEADERS.length; columnIndex++) {
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(HEADERS[columnIndex]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeRows(
            Sheet sheet,
            List<Company> companies,
            Map<Long, List<CompanyContact>> contactsByCompanyId,
            Map<Long, List<CompanyContactMethod>> contactMethodsByCompanyId,
            CellStyle wrapStyle
    ) {
        for (int index = 0; index < companies.size(); index++) {
            Company company = companies.get(index);
            Row row = sheet.createRow(index + 1);
            List<CompanyContactMethod> contactMethods =
                    contactMethodsByCompanyId.getOrDefault(
                            company.getId(),
                            List.of()
                    );
            List<CompanyContact> contacts = sortContacts(
                    contactsByCompanyId.getOrDefault(company.getId(), List.of())
            );

            row.createCell(0).setCellValue(nullToBlank(company.getName()));
            row.createCell(1).setCellValue(displayCompanyType(company.getCompanyType()));
            row.createCell(2).setCellValue(nullToBlank(company.getActivityArea()));
            createWrappedCell(row, 3, joinContactMethods(
                    contactMethods,
                    CompanyContactMethodType.PHONE
            ), wrapStyle);
            createWrappedCell(row, 4, joinContactMethods(
                    contactMethods,
                    CompanyContactMethodType.EMAIL
            ), wrapStyle);
            createWrappedCell(row, 5, joinContacts(
                    contacts,
                    CompanyContact::getFullName
            ), wrapStyle);
            createWrappedCell(row, 6, joinContacts(
                    contacts,
                    CompanyContact::getPosition
            ), wrapStyle);
            createWrappedCell(row, 7, joinContacts(
                    contacts,
                    CompanyContact::getPhone
            ), wrapStyle);
            createWrappedCell(row, 8, joinContacts(
                    contacts,
                    CompanyContact::getEmail
            ), wrapStyle);
            applyRowHeight(row);
        }
    }

    private void createWrappedCell(
            Row row,
            int columnIndex,
            String value,
            CellStyle wrapStyle
    ) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(wrapStyle);
    }

    private void applyRowHeight(Row row) {
        int maxLineCount = 1;
        for (int columnIndex = 0; columnIndex < HEADERS.length; columnIndex++) {
            Cell cell = row.getCell(columnIndex);
            if (cell == null) {
                continue;
            }
            maxLineCount = Math.max(
                    maxLineCount,
                    cell.getStringCellValue().split("\n", -1).length
            );
        }

        row.setHeightInPoints(maxLineCount * row.getSheet()
                .getDefaultRowHeightInPoints());
    }

    private void applyColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 32 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 42 * 256);
        sheet.setColumnWidth(3, 24 * 256);
        sheet.setColumnWidth(4, 34 * 256);
        sheet.setColumnWidth(5, 28 * 256);
        sheet.setColumnWidth(6, 24 * 256);
        sheet.setColumnWidth(7, 24 * 256);
        sheet.setColumnWidth(8, 34 * 256);
    }

    private String joinContactMethods(
            List<CompanyContactMethod> contactMethods,
            CompanyContactMethodType type
    ) {
        return contactMethods.stream()
                .filter(contactMethod -> type.equals(contactMethod.getType()))
                .sorted(Comparator
                        .comparing(
                                CompanyContactMethod::isPrimary,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                CompanyContactMethod::getId,
                                Comparator.nullsLast(Long::compareTo)
                        ))
                .map(CompanyContactMethod::getValue)
                .map(this::nullToBlank)
                .collect(Collectors.joining("\n"));
    }

    private List<CompanyContact> sortContacts(List<CompanyContact> contacts) {
        return contacts.stream()
                .sorted(Comparator
                        .comparing(
                                CompanyContact::isPrimary,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                CompanyContact::getId,
                                Comparator.nullsLast(Long::compareTo)
                        ))
                .toList();
    }

    private String joinContacts(
            List<CompanyContact> contacts,
            Function<CompanyContact, String> valueExtractor
    ) {
        return contacts.stream()
                .map(valueExtractor)
                .map(this::nullToBlank)
                .collect(Collectors.joining("\n"));
    }

    private String displayCompanyType(CompanyType companyType) {
        if (CompanyType.EXTERNAL.equals(companyType)) {
            return "D\u0131\u015F \u015Eirket";
        }

        return "TGB";
    }

    private String normalizeFilter(String value) {
        return value == null
                ? ""
                : value.trim();
    }

    private String nullToBlank(String value) {
        return value == null
                ? ""
                : value;
    }
}
