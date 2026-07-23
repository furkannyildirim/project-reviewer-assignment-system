package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianResponse;
import com.sakaryateknokent.reviewerassignment.dto.CreateAcademicianRequest;
import com.sakaryateknokent.reviewerassignment.dto.PageResponse;
import com.sakaryateknokent.reviewerassignment.dto.UpdateAcademicianRequest;
import com.sakaryateknokent.reviewerassignment.service.AcademicianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/academicians")
@RequiredArgsConstructor
public class AcademicianController {

    private final AcademicianService academicianService;

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

    @GetMapping
    public ResponseEntity<PageResponse<AcademicianResponse>>
    getAcademicians(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                academicianService.getAcademicians(search, page, size)
        );
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
}