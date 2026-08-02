package com.sakaryateknokent.reviewerassignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateCompanyRequest(

        @NotBlank(message = "Şirket adı boş bırakılamaz")
        @Size(max = 200, message = "Şirket adı en fazla 200 karakter olabilir")
        String name,

        @Email(message = "Geçerli bir e-posta adresi girilmelidir")
        @Size(max = 255, message = "E-posta en fazla 255 karakter olabilir")
        String email,

        @Size(max = 30, message = "Telefon en fazla 30 karakter olabilir")
        String phone,

        @Size(max = 30, message = "İkinci telefon en fazla 30 karakter olabilir")
        String secondaryPhone,

        @Size(max = 2000, message = "Faaliyet alanı en fazla 2000 karakter olabilir")
        String activityArea,

        @Size(max = 500, message = "Logo adresi en fazla 500 karakter olabilir")
        String logoUrl,

        @NotEmpty(message = "En az bir muhatap kişi eklenmelidir")
        List<@Valid @NotNull(message = "Muhatap bilgisi boş olamaz") UpdateCompanyContactRequest> contacts
) {
}
