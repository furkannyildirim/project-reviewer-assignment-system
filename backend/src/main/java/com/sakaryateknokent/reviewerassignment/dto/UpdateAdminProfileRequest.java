package com.sakaryateknokent.reviewerassignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAdminProfileRequest(

        @NotBlank(message = "Ad boş bırakılamaz")
        @Size(max = 100, message = "Ad en fazla 100 karakter olabilir")
        String firstName,

        @NotBlank(message = "Soyad boş bırakılamaz")
        @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir")
        String lastName,

        @NotBlank(message = "Telefon boş bırakılamaz")
        @Size(max = 30, message = "Telefon en fazla 30 karakter olabilir")
        String phone
) {
}
