package com.sakaryateknokent.reviewerassignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeAdminPasswordRequest(

        @NotBlank(message = "Mevcut parola boş bırakılamaz")
        String currentPassword,

        @NotBlank(message = "Yeni parola boş bırakılamaz")
        @Size(min = 8, max = 72, message = "Yeni parola 8 ile 72 karakter arasında olmalıdır")
        String newPassword
) {
}
