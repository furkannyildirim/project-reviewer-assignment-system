package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianResponse;
import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportPreviewResponse;
import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportResultResponse;
import com.sakaryateknokent.reviewerassignment.dto.CreateAcademicianRequest;
import com.sakaryateknokent.reviewerassignment.dto.ExportSelectionRequest;
import com.sakaryateknokent.reviewerassignment.dto.PageResponse;
import com.sakaryateknokent.reviewerassignment.dto.UpdateAcademicianRequest;
import com.sakaryateknokent.reviewerassignment.service.AcademicianExportService;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportService;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportPreviewService;
import com.sakaryateknokent.reviewerassignment.service.AcademicianService;
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
@RequestMapping("/api/academicians")
@RequiredArgsConstructor
public class AcademicianController {

    private final AcademicianService academicianService;
    private final AcademicianExportService academicianExportService;
    private final AcademicianImportPreviewService importPreviewService;
    private final AcademicianImportService importService;

    @PostMapping
    public ResponseEntity<AcademicianResponse> createAcademician(
            @Valid @RequestBody CreateAcademicianRequest request
    ) {
        AcademicianResponse response =
                academicianService.createAcademician(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/import/preview")
    public ResponseEntity<AcademicianImportPreviewResponse> previewImport(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(importPreviewService.preview(file));
    }

    @PostMapping("/import")
    public ResponseEntity<AcademicianImportResultResponse> importAcademicians(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(importService.importCreateOnly(file));
    }

    @GetMapping
    public ResponseEntity<PageResponse<AcademicianResponse>>
    getAcademicians(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String initial,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                academicianService.getAcademicians(search, initial, page, size)
        );
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countAcademicians() {
        return ResponseEntity.ok(academicianService.countAcademicians());
    }

    @GetMapping("/ids")
    public ResponseEntity<java.util.List<Long>> getAcademicianIds(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String initial
    ) {
        return ResponseEntity.ok(
                academicianService.getAcademicianIds(search, initial)
        );
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAcademicians(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String initial
    ) {
        byte[] fileContent = academicianExportService.exportToExcel(
                academicianService.getAcademiciansForExport(search, initial)
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("akademisyenler.xlsx")
                                .build()
                                .toString()
                )
                .body(fileContent);
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportSelectedAcademicians(
            @RequestBody ExportSelectionRequest request
    ) {
        byte[] fileContent = academicianExportService.exportToExcel(
                academicianService.getAcademiciansForExport(request.ids())
        );

        return excelResponse(fileContent);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicianResponse> getAcademicianById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                academicianService.getAcademicianById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<AcademicianResponse> updateAcademician(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAcademicianRequest request
    ) {
        return ResponseEntity.ok(
                academicianService.updateAcademician(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAcademician(
            @PathVariable Long id
    ) {
        academicianService.deleteAcademician(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<byte[]> excelResponse(byte[] fileContent) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("akademisyenler.xlsx")
                                .build()
                                .toString()
                )
                .body(fileContent);
    }
}
