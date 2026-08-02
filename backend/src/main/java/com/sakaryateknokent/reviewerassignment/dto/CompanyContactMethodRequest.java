package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompanyContactMethodRequest(
        Long id,

        @NotNull(message = "Iletisim tipi zorunludur")
        CompanyContactMethodType type,

        @NotBlank(message = "Iletisim degeri bos birakilamaz")
        @Size(max = 255, message = "Iletisim degeri en fazla 255 karakter olabilir")
        String value,

        @Size(max = 100, message = "Iletisim etiketi en fazla 100 karakter olabilir")
        String label,

        boolean primary
) {
}
