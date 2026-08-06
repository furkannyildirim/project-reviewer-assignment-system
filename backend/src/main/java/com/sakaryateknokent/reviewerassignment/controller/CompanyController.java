package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.CompanyListResponse;
import com.sakaryateknokent.reviewerassignment.dto.ExportSelectionRequest;
import com.sakaryateknokent.reviewerassignment.dto.CompanyImportResultResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyImportPreviewResponse;
import com.sakaryateknokent.reviewerassignment.dto.CreateCompanyRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateCompanyRequest;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.service.CompanyExportService;
import com.sakaryateknokent.reviewerassignment.service.CompanyImportService;
import com.sakaryateknokent.reviewerassignment.service.CompanyImportPreviewService;
import com.sakaryateknokent.reviewerassignment.service.CompanyService;
import com.sakaryateknokent.reviewerassignment.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyExportService companyExportService;
    private final CompanyImportPreviewService importPreviewService;
    private final CompanyImportService importService;

    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(
            @Valid @RequestBody CreateCompanyRequest request
    ) {
        CompanyResponse response = companyService.createCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/import/preview")
    public ResponseEntity<CompanyImportPreviewResponse> previewImport(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(importPreviewService.preview(file));
    }

    @PostMapping("/external/import/preview")
    public ResponseEntity<CompanyImportPreviewResponse> previewExternalImport(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                importPreviewService.preview(file, CompanyType.EXTERNAL)
        );
    }

    @PostMapping("/import")
    public ResponseEntity<CompanyImportResultResponse> importCompanies(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(importService.importTgbCompanies(file));
    }

    @PostMapping("/external/import")
    public ResponseEntity<CompanyImportResultResponse> importExternalCompanies(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(importService.importExternalCompanies(file));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CompanyListResponse>> getCompanies(
            @RequestParam CompanyType companyType,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String initial,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                companyService.getCompanies(
                        companyType,
                        search,
                        initial,
                        page,
                        size
                )
        );
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countCompanies(
            @RequestParam CompanyType companyType
    ) {
        return ResponseEntity.ok(companyService.countCompanies(companyType));
    }

    @GetMapping("/ids")
    public ResponseEntity<java.util.List<Long>> getCompanyIds(
            @RequestParam CompanyType companyType,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String initial
    ) {
        return ResponseEntity.ok(
                companyService.getCompanyIds(companyType, search, initial)
        );
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCompanies(
            @RequestParam CompanyType companyType,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String initial
    ) {
        byte[] fileContent = companyExportService.exportToExcel(
                companyType,
                search,
                initial
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(exportFileName(companyType))
                                .build()
                                .toString()
                )
                .body(fileContent);
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportSelectedCompanies(
            @RequestParam CompanyType companyType,
            @RequestBody ExportSelectionRequest request
    ) {
        byte[] fileContent = companyExportService.exportToExcel(
                companyType,
                request.ids()
        );

        return excelResponse(fileContent, exportFileName(companyType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompanyById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCompanyRequest request
    ) {
        return ResponseEntity.ok(companyService.updateCompany(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(
            @PathVariable Long id
    ) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    private String exportFileName(CompanyType companyType) {
        return CompanyType.EXTERNAL.equals(companyType)
                ? "dis-sirketler.xlsx"
                : "tgb-sirketleri.xlsx";
    }

    private ResponseEntity<byte[]> excelResponse(
            byte[] fileContent,
            String fileName
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName)
                                .build()
                                .toString()
                )
                .body(fileContent);
    }
}
