package com.sakaryateknokent.reviewerassignment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateAcademicianRequest(

        @NotBlank(message = "Akademisyen adı boş bırakılamaz")
        @Size(max = 100, message = "Ad en fazla 100 karakter olabilir")
        String firstName,

        @NotBlank(message = "Akademisyen soyadı boş bırakılamaz")
        @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir")
        String lastName,

        @Email(message = "Geçerli bir e-posta adresi girilmelidir")
        @Size(max = 255, message = "E-posta en fazla 255 karakter olabilir")
        String email,

        @Size(max = 30, message = "Telefon en fazla 30 karakter olabilir")
        String phone,

        @Size(max = 100, message = "Ünvan en fazla 100 karakter olabilir")
        String title,

        Set<
                @NotBlank(message = "Anahtar kelime boş olamaz")
                @Size(
                        max = 255,
                        message = "Anahtar kelime en fazla 255 karakter olabilir"
                )
                        String
                > keywords
) {
}