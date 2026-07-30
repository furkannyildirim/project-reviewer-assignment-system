package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.Academician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AcademicianRepository
        extends JpaRepository<Academician, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
            SELECT LOWER(academician.email)
            FROM Academician academician
            WHERE academician.email IS NOT NULL
              AND LOWER(academician.email) IN :emails
            """)
    Set<String> findExistingEmails(@Param("emails") Collection<String> emails);

    @Query("""
            SELECT academician.firstName AS firstName,
                   academician.lastName AS lastName,
                   academician.title AS title
            FROM Academician academician
            """)
    List<AcademicianIdentityProjection> findIdentities();

    @Query(
            value = """
                    SELECT DISTINCT academician
                    FROM Academician academician
                    LEFT JOIN academician.keywords keyword
                    WHERE :search = ''
                       OR LOWER(academician.firstName)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(academician.lastName)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(academician.email)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(academician.phone)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(academician.title)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(keyword)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT academician)
                    FROM Academician academician
                    LEFT JOIN academician.keywords keyword
                    WHERE :search = ''
                       OR LOWER(academician.firstName)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(academician.lastName)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(academician.email)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(academician.phone)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(academician.title)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(keyword)
                            LIKE LOWER(CONCAT('%', :search, '%'))
                    """
    )
    Page<Academician> search(
            @Param("search") String search,
            Pageable pageable
    );
}
