package com.sakaryateknokent.reviewerassignment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "E-posta boş bırakılamaz")
        @Email(message = "Geçerli bir e-posta adresi girilmelidir")
        @Size(max = 255, message = "E-posta en fazla 255 karakter olabilir")
        String email,

        @NotBlank(message = "Parola boş bırakılamaz")
        @Size(
                min = 8,
                max = 72,
                message = "Parola 8 ile 72 karakter arasında olmalıdır"
        )
        String password

) {
}