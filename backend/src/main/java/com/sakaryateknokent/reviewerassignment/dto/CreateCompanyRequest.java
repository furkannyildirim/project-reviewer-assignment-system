package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCompanyRequest(

        @NotBlank(message = "Sirket adi bos birakilamaz")
        @Size(max = 200, message = "Sirket adi en fazla 200 karakter olabilir")
        String name,

        @Size(max = 2000, message = "Faaliyet alani en fazla 2000 karakter olabilir")
        String activityArea,

        @Size(max = 500, message = "Logo adresi en fazla 500 karakter olabilir")
        String logoUrl,

        @NotNull(message = "Sirket turu zorunludur")
        CompanyType companyType,

        List<@Valid @NotNull(message = "Iletisim bilgisi bos olamaz") CompanyContactMethodRequest> contactMethods,

        @NotEmpty(message = "En az bir yetkili kisi eklenmelidir")
        List<@Valid @NotNull(message = "Yetkili kisi bilgisi bos olamaz") CreateCompanyContactRequest> contacts
) {
}
