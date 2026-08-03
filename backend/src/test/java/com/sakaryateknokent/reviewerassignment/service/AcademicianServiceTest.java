package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianResponse;
import com.sakaryateknokent.reviewerassignment.dto.PageResponse;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicianServiceTest {

    private final AcademicianRepository academicianRepository =
            mock(AcademicianRepository.class);
    private final AcademicianService academicianService =
            new AcademicianService(academicianRepository);

    @Test
    void listsAcademiciansWithoutInitialLikeExistingBehavior() {
        Academician academician = academician(1L, "Mehmet", "Aksoy");
        when(academicianRepository.search(
                eq("yapay"),
                eq(""),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(academician),
                PageRequest.of(0, 10),
                47
        ));

        PageResponse<AcademicianResponse> response =
                academicianService.getAcademicians(" yapay ", null, 0, 10);

        assertEquals(47, response.totalElements());
        assertEquals("Mehmet", response.content().getFirst().firstName());
        verify(academicianRepository).search(
                eq("yapay"),
                eq(""),
                any(Pageable.class)
        );
    }

    @Test
    void passesTrimmedInitialAndPreservesFilteredTotalElements() {
        Academician ayse = academician(1L, "Ayse", "Kaya");
        Academician ahmet = academician(2L, "Ahmet", "Yilmaz");
        when(academicianRepository.search(
                eq("zeka"),
                eq("A"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(ayse, ahmet),
                PageRequest.of(0, 10),
                2
        ));

        PageResponse<AcademicianResponse> response =
                academicianService.getAcademicians(" zeka ", " A ", 0, 10);

        assertEquals(2, response.totalElements());
        assertEquals(List.of("Ayse", "Ahmet"), response.content()
                .stream()
                .map(AcademicianResponse::firstName)
                .toList());
        verify(academicianRepository).search(
                eq("zeka"),
                eq("A"),
                any(Pageable.class)
        );
    }

    @Test
    void passesLowercaseAndTurkishInitialsWithoutLocaleNormalization() {
        when(academicianRepository.search(
                eq(""),
                eq("\u015F"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(academician(1L, "\u015Eule", "Oz")),
                PageRequest.of(0, 10),
                1
        ));

        PageResponse<AcademicianResponse> response =
                academicianService.getAcademicians("", " \u015F ", 0, 10);

        assertEquals(1, response.totalElements());
        verify(academicianRepository).search(
                eq(""),
                eq("\u015F"),
                any(Pageable.class)
        );
    }

    @Test
    void countsAllAcademiciansWithoutSearchOrInitialFilters() {
        when(academicianRepository.count()).thenReturn(248L);

        long count = academicianService.countAcademicians();

        assertEquals(248L, count);
        verify(academicianRepository).count();
        verify(academicianRepository, never()).search(
                anyString(),
                anyString(),
                any(Pageable.class)
        );
    }

    private Academician academician(Long id, String firstName, String lastName) {
        Academician academician = new Academician();
        academician.setId(id);
        academician.setFirstName(firstName);
        academician.setLastName(lastName);
        academician.setEmail(null);
        academician.setPhone(null);
        academician.setTitle(null);
        return academician;
    }
}
