package com.sakaryateknokent.reviewerassignment.entity;

import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "project_manager", length = 255)
    private String projectManager;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 255)
    private String category;

    @Column(length = 255)
    private String sector;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "research_and_development_nature", columnDefinition = "text")
    private String researchAndDevelopmentNature;

    @Column(name = "innovative_aspect", columnDefinition = "text")
    private String innovativeAspect;

    @Column(name = "main_technology_area", columnDefinition = "text")
    private String mainTechnologyArea;

    @Column(name = "project_detail", columnDefinition = "text")
    private String projectDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectStatus status;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "project_keywords",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "keyword", nullable = false, length = 255)
    private Set<String> keywords = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void setCreationTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void setUpdateTimestamp() {
        updatedAt = Instant.now();
    }
}
