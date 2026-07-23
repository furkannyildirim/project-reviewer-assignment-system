package com.sakaryateknokent.reviewerassignment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(

        @NotBlank(message = "Şirket adı boş bırakılamaz")
        @Size(max = 200, message = "Şirket adı en fazla 200 karakter olabilir")
        String name,

        @Email(message = "Geçerli bir e-posta adresi girilmelidir")
        @Size(max = 255, message = "E-posta en fazla 255 karakter olabilir")
        String email,

        @Size(max = 30, message = "Telefon en fazla 30 karakter olabilir")
        String phone,

        @Size(max = 255, message = "Faaliyet alanı en fazla 255 karakter olabilir")
        String activityArea,

        @Size(max = 500, message = "Logo adresi en fazla 500 karakter olabilir")
        String logoUrl
) {
}
