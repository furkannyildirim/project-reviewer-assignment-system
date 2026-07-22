package com.sakaryateknokent.reviewerassignment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(name = "activity_area", nullable = false, length = 255)
    private String activityArea;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;
}