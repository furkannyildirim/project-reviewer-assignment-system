package com.sakaryateknokent.reviewerassignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateCompanyRequest(

        @NotBlank(message = "Sirket adi bos birakilamaz")
        @Size(max = 200, message = "Sirket adi en fazla 200 karakter olabilir")
        String name,

        @Size(max = 2000, message = "Faaliyet alani en fazla 2000 karakter olabilir")
        String activityArea,

        @Size(max = 150, message = "Il en fazla 150 karakter olabilir")
        String city,

        @Size(max = 150, message = "Ilce en fazla 150 karakter olabilir")
        String district,

        @Size(max = 255, message = "OSB en fazla 255 karakter olabilir")
        String osb,

        String address,

        @Size(max = 255, message = "Bina en fazla 255 karakter olabilir")
        String building,

        @Size(max = 500, message = "Logo adresi en fazla 500 karakter olabilir")
        String logoUrl,

        List<@Valid @NotNull(message = "Iletisim bilgisi bos olamaz") CompanyContactMethodRequest> contactMethods,

        @NotEmpty(message = "En az bir muhatap kisi eklenmelidir")
        List<@Valid @NotNull(message = "Muhatap bilgisi bos olamaz") UpdateCompanyContactRequest> contacts
) {
}
