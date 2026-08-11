package com.sakaryateknokent.reviewerassignment.entity;

import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "company_contact_methods")
@Getter
@Setter
@NoArgsConstructor
public class CompanyContactMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyContactMethodType type;

    @Column(nullable = false, length = 255)
    private String value;

    @Column(length = 100)
    private String label;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;
}
