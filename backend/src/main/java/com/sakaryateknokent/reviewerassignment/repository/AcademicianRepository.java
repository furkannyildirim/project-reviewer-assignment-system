package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.Academician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
            SELECT academician
            FROM Academician academician
            WHERE academician.email IS NOT NULL
              AND LOWER(academician.email) = :email
            """)
    List<Academician> findByNormalizedEmail(@Param("email") String email);

    List<Academician> findByIdIn(Collection<Long> ids, Sort sort);

    @Query("""
            SELECT academician
            FROM Academician academician
            WHERE academician.canonicalFirstName = :firstName
              AND academician.canonicalLastName = :lastName
              AND academician.canonicalTitle = :title
            """)
    List<Academician> findByCanonicalIdentity(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("title") String title
    );

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
                    WHERE (
                           :search = ''
                           OR LOWER(academician.firstName)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(academician.lastName)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(CONCAT(academician.firstName, ' ', academician.lastName))
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(academician.email)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(academician.phone)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(academician.title)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(keyword)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                      AND (
                           :initial = ''
                           OR LOWER(academician.firstName)
                                LIKE LOWER(CONCAT(:initial, '%'))
                    )
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT academician)
                    FROM Academician academician
                    LEFT JOIN academician.keywords keyword
                    WHERE (
                           :search = ''
                           OR LOWER(academician.firstName)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(academician.lastName)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(CONCAT(academician.firstName, ' ', academician.lastName))
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(academician.email)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(academician.phone)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(academician.title)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(keyword)
                                LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                      AND (
                           :initial = ''
                           OR LOWER(academician.firstName)
                                LIKE LOWER(CONCAT(:initial, '%'))
                    )
                    """
    )
    Page<Academician> search(
            @Param("search") String search,
            @Param("initial") String initial,
            Pageable pageable
    );
}
