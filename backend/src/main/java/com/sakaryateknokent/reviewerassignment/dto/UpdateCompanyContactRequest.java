package com.sakaryateknokent.reviewerassignment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateCompanyContactRequest(

        @Positive(message = "Muhatap kimliği pozitif olmalıdır")
        Long id,

        @NotBlank(message = "Muhatap adı soyadı boş bırakılamaz")
        @Size(max = 200, message = "Muhatap adı soyadı en fazla 200 karakter olabilir")
        String fullName,

        @Size(max = 150, message = "Pozisyon en fazla 150 karakter olabilir")
        String position,

        @Email(message = "Geçerli bir muhatap e-posta adresi girilmelidir")
        @Size(max = 255, message = "Muhatap e-postası en fazla 255 karakter olabilir")
        String email,

        @Size(max = 30, message = "Muhatap telefonu en fazla 30 karakter olabilir")
        String phone,

        boolean primary
) {
}
