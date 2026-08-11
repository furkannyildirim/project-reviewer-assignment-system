package com.sakaryateknokent.reviewerassignment.entity;

import com.sakaryateknokent.reviewerassignment.service.AcademicianTextNormalizer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "academicians")
@Getter
@Setter
@NoArgsConstructor
public class Academician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String title;

    @Column(name = "canonical_first_name", nullable = false, length = 100)
    private String canonicalFirstName;

    @Column(name = "canonical_last_name", nullable = false, length = 100)
    private String canonicalLastName;

    @Column(name = "canonical_title", nullable = false, length = 100)
    private String canonicalTitle;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "academician_keywords",
            joinColumns = @JoinColumn(name = "academician_id")
    )
    @Column(name = "keyword", nullable = false, length = 255)
    private Set<String> keywords = new LinkedHashSet<>();

    @PrePersist
    @PreUpdate
    public void refreshCanonicalIdentity() {
        canonicalFirstName =
                AcademicianTextNormalizer.canonicalizeIdentityPart(firstName);
        canonicalLastName =
                AcademicianTextNormalizer.canonicalizeIdentityPart(lastName);
        canonicalTitle =
                AcademicianTextNormalizer.canonicalizeIdentityPart(title);
    }
}
