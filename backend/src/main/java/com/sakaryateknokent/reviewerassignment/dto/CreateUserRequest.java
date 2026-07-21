package com.sakaryateknokent.reviewerassignment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "Ad boş bırakılamaz")
        @Size(max = 100, message = "Ad en fazla 100 karakter olabilir")
        String firstName,

        @NotBlank(message = "Soyad boş bırakılamaz")
        @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir")
        String lastName,

        @NotBlank(message = "E-posta boş bırakılamaz")
        @Email(message = "Geçerli bir e-posta adresi girilmelidir")
        @Size(max = 255, message = "E-posta en fazla 255 karakter olabilir")
        String email,

        @NotBlank(message = "Telefon boş bırakılamaz")
        @Size(max = 30, message = "Telefon en fazla 30 karakter olabilir")
        String phone,

        @NotBlank(message = "Parola boş bırakılamaz")
        @Size(min = 8, max = 72, message = "Parola 8 ile 72 karakter arasında olmalıdır")
        String password
) {
}