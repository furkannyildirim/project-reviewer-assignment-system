package com.sakaryateknokent.reviewerassignment.entity;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @Column(unique = true, length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "secondary_phone", length = 30)
    private String secondaryPhone;

    @Column(name = "activity_area", columnDefinition = "text")
    private String activityArea;

    @Column(length = 150)
    private String city;

    @Column(length = 150)
    private String district;

    @Column(length = 255)
    private String osb;

    @Column(columnDefinition = "text")
    private String address;

    @Column(length = 255)
    private String building;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false, length = 20)
    private CompanyType companyType;

    @OneToMany(
            mappedBy = "company",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Setter(AccessLevel.NONE)
    private List<CompanyContact> contacts = new ArrayList<>();

    @OneToMany(
            mappedBy = "company",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Setter(AccessLevel.NONE)
    private List<CompanyContactMethod> contactMethods = new ArrayList<>();

    public void addContact(CompanyContact contact) {
        contacts.add(contact);
        contact.setCompany(this);
    }

    public void removeContact(CompanyContact contact) {
        contacts.remove(contact);
        contact.setCompany(null);
    }

    public void addContactMethod(CompanyContactMethod contactMethod) {
        contactMethods.add(contactMethod);
        contactMethod.setCompany(this);
    }

    public void removeContactMethod(CompanyContactMethod contactMethod) {
        contactMethods.remove(contactMethod);
        contactMethod.setCompany(null);
    }
}
