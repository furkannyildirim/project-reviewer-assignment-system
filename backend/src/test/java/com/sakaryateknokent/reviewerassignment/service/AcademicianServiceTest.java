package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianResponse;
import com.sakaryateknokent.reviewerassignment.dto.CreateAcademicianRequest;
import com.sakaryateknokent.reviewerassignment.dto.PageResponse;
import com.sakaryateknokent.reviewerassignment.dto.UpdateAcademicianRequest;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void createsAcademicianWithNormalizedOptionalFieldsAndKeywords() {
        when(academicianRepository.saveAndFlush(any(Academician.class)))
                .thenAnswer(invocation -> {
                    Academician academician = invocation.getArgument(0);
                    academician.setId(1L);
                    academician.setVersion(0L);
                    return academician;
                });

        AcademicianResponse response = academicianService.createAcademician(
                new CreateAcademicianRequest(
                        " Uğur ",
                        " Güzel ",
                        " UGUR@EXAMPLE.COM ",
                        " 555 111 2233 ",
                        " Doç. Dr. ",
                        Set.of(" yapay zeka ", "robotik", "")
                )
        );

        assertEquals("Uğur", response.firstName());
        assertEquals("Güzel", response.lastName());
        assertEquals("ugur@example.com", response.email());
        assertEquals("555 111 2233", response.phone());
        assertEquals("Doç. Dr.", response.title());
        assertEquals(Set.of("yapay zeka", "robotik"), response.keywords());
        Academician saved = savedAcademician();
        assertEquals("uğur", saved.getCanonicalFirstName());
        assertEquals("güzel", saved.getCanonicalLastName());
        assertEquals("doç. dr.", saved.getCanonicalTitle());
        verify(academicianRepository).existsByEmail("ugur@example.com");
    }

    @Test
    void createsAcademicianWithNullOptionalEmailPhoneAndTitle() {
        when(academicianRepository.saveAndFlush(any(Academician.class)))
                .thenAnswer(invocation -> {
                    Academician academician = invocation.getArgument(0);
                    academician.setVersion(0L);
                    return academician;
                });

        AcademicianResponse response = academicianService.createAcademician(
                new CreateAcademicianRequest(
                        "Şeyma",
                        "Şimşek",
                        " ",
                        null,
                        "",
                        null
                )
        );

        assertNull(response.email());
        assertNull(response.phone());
        assertNull(response.title());
        assertEquals(Set.of(), response.keywords());
        Academician saved = savedAcademician();
        assertEquals("şeyma", saved.getCanonicalFirstName());
        assertEquals("şimşek", saved.getCanonicalLastName());
        assertEquals("", saved.getCanonicalTitle());
        verify(academicianRepository, never()).existsByEmail(anyString());
    }

    @Test
    void rejectsDuplicateEmailOnCreate() {
        when(academicianRepository.existsByEmail("ugur@example.com"))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> academicianService.createAcademician(
                        new CreateAcademicianRequest(
                                "Uğur",
                                "Güzel",
                                " UGUR@EXAMPLE.COM ",
                                null,
                                null,
                                Set.of()
                        )
                )
        );

        verify(academicianRepository, never()).save(any(Academician.class));
        verify(academicianRepository, never()).saveAndFlush(any(Academician.class));
    }

    @Test
    void updatesAcademicianWithoutFalsePositiveForOwnEmail() {
        Academician existing = academician(9L, "Uğur", "Güzel");
        existing.setEmail("ugur@example.com");
        existing.getKeywords().add("eski");
        when(academicianRepository.findById(9L))
                .thenReturn(Optional.of(existing));
        when(academicianRepository.saveAndFlush(existing)).thenReturn(existing);

        AcademicianResponse response = academicianService.updateAcademician(
                9L,
                new UpdateAcademicianRequest(
                        0L,
                        " Yunus Emre ",
                        " Tanış ",
                        " UGUR@EXAMPLE.COM ",
                        null,
                        " Arş. Gör. ",
                        Set.of(" yeni ", "ai")
                )
        );

        assertEquals("Yunus Emre", response.firstName());
        assertEquals("Tanış", response.lastName());
        assertEquals("ugur@example.com", response.email());
        assertEquals(0L, response.version());
        assertNull(response.phone());
        assertEquals("Arş. Gör.", response.title());
        assertEquals(Set.of("yeni", "ai"), response.keywords());
        assertEquals("yunus emre", existing.getCanonicalFirstName());
        assertEquals("tanış", existing.getCanonicalLastName());
        assertEquals("arş. gör.", existing.getCanonicalTitle());
        verify(academicianRepository).existsByEmailAndIdNot(
                "ugur@example.com",
                9L
        );
    }

    @Test
    void rejectsDuplicateEmailOnUpdateForAnotherAcademician() {
        when(academicianRepository.findById(9L))
                .thenReturn(Optional.of(academician(9L, "Uğur", "Güzel")));
        when(academicianRepository.existsByEmailAndIdNot(
                "other@example.com",
                9L
        )).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> academicianService.updateAcademician(
                        9L,
                        new UpdateAcademicianRequest(
                                0L,
                                "Uğur",
                                "Güzel",
                                " other@example.com ",
                                null,
                                null,
                                Set.of()
                        )
                )
        );

        verify(academicianRepository, never()).save(any(Academician.class));
        verify(academicianRepository, never()).saveAndFlush(any(Academician.class));
    }

    @Test
    void rejectsStaleUpdateVersionBeforeSaving() {
        Academician existing = academician(9L, "Uğur", "Güzel");
        existing.setVersion(2L);
        when(academicianRepository.findById(9L))
                .thenReturn(Optional.of(existing));

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> academicianService.updateAcademician(
                        9L,
                        new UpdateAcademicianRequest(
                                1L,
                                "Uğur",
                                "Güzel",
                                "ugur@example.com",
                                null,
                                null,
                                Set.of()
                        )
                )
        );

        verify(academicianRepository, never()).save(any(Academician.class));
        verify(academicianRepository, never()).saveAndFlush(any(Academician.class));
        verify(academicianRepository, never()).existsByEmailAndIdNot(
                anyString(),
                eq(9L)
        );
    }

    @Test
    void deletesAcademicianById() {
        Academician academician = academician(7L, "Uğur", "Güzel");
        when(academicianRepository.findById(7L))
                .thenReturn(Optional.of(academician));

        academicianService.deleteAcademician(7L);

        verify(academicianRepository).delete(academician);
    }

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

    @Test
    void exportsAcademiciansWithTrimmedSearchAndInitialWithoutPagination() {
        Academician academician = academician(1L, "Ayse", "Yilmaz");
        when(academicianRepository.search(
                eq("yapay"),
                eq("A"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(academician)));

        List<AcademicianResponse> response =
                academicianService.getAcademiciansForExport(" yapay ", " A ");

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        assertEquals(1, response.size());
        assertEquals("Ayse", response.getFirst().firstName());
        verify(academicianRepository).search(
                eq("yapay"),
                eq("A"),
                pageableCaptor.capture()
        );
        assertFalse(pageableCaptor.getValue().isPaged());
    }

    @Test
    void exportsAcademiciansWithoutFiltersWhenSearchAndInitialAreMissing() {
        when(academicianRepository.search(
                eq(""),
                eq(""),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(
                academician(1L, "Ayse", "Yilmaz"),
                academician(2L, "Mehmet", "Kaya")
        )));

        List<AcademicianResponse> response =
                academicianService.getAcademiciansForExport(null, null);

        assertEquals(2, response.size());
        verify(academicianRepository).search(
                eq(""),
                eq(""),
                any(Pageable.class)
        );
    }

    @Test
    void returnsFilteredAcademicianIdsWithoutPagination() {
        when(academicianRepository.search(
                eq("yazilim"),
                eq("A"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(
                academician(1L, "Ayse", "Yilmaz"),
                academician(2L, "Ali", "Kaya")
        )));

        List<Long> ids = academicianService.getAcademicianIds(
                " yazilim ",
                " A "
        );

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        assertEquals(List.of(1L, 2L), ids);
        verify(academicianRepository).search(
                eq("yazilim"),
                eq("A"),
                pageableCaptor.capture()
        );
        assertFalse(pageableCaptor.getValue().isPaged());
    }

    @Test
    void exportsSelectedAcademiciansWithUniqueIdsAndIgnoresMissingIds() {
        when(academicianRepository.findByIdIn(
                eq(new java.util.LinkedHashSet<>(List.of(2L, 1L, 999L))),
                any(org.springframework.data.domain.Sort.class)
        )).thenReturn(List.of(
                academician(1L, "Ayse", "Yilmaz"),
                academician(2L, "Ali", "Kaya")
        ));

        List<AcademicianResponse> response =
                academicianService.getAcademiciansForExport(
                        List.of(2L, 1L, 2L, 999L)
                );

        assertEquals(List.of(1L, 2L), response.stream()
                .map(AcademicianResponse::id)
                .toList());
    }

    @Test
    void selectedAcademicianExportWithEmptyIdsDoesNotQueryRepository() {
        List<AcademicianResponse> response =
                academicianService.getAcademiciansForExport(List.of());

        assertEquals(List.of(), response);
        verify(academicianRepository, never()).findByIdIn(
                any(),
                any(org.springframework.data.domain.Sort.class)
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
        academician.setVersion(0L);
        return academician;
    }

    private Academician savedAcademician() {
        ArgumentCaptor<Academician> captor =
                ArgumentCaptor.forClass(Academician.class);
        verify(academicianRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
