package com.sakaryateknokent.reviewerassignment.entity;

import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyContactMethodTest {

    @Test
    void companyOwnsContactMethodsAndKeepsBackReferenceInSync() {
        Company company = new Company();
        CompanyContactMethod contactMethod = new CompanyContactMethod();

        company.addContactMethod(contactMethod);

        assertEquals(1, company.getContactMethods().size());
        assertSame(company, contactMethod.getCompany());

        company.removeContactMethod(contactMethod);

        assertTrue(company.getContactMethods().isEmpty());
        assertNull(contactMethod.getCompany());
    }

    @Test
    void persistsTypeAsStringEnum() throws NoSuchFieldException {
        Field typeField = CompanyContactMethod.class.getDeclaredField("type");
        Enumerated enumerated = typeField.getAnnotation(Enumerated.class);
        Column column = typeField.getAnnotation(Column.class);

        assertEquals(EnumType.STRING, enumerated.value());
        assertFalse(column.nullable());
        assertEquals(20, column.length());
    }

    @Test
    void supportsPrimaryPhoneAndPrimaryEmailForSameCompany() {
        Company company = new Company();

        CompanyContactMethod phone = new CompanyContactMethod();
        phone.setType(CompanyContactMethodType.PHONE);
        phone.setValue("111");
        phone.setPrimary(true);

        CompanyContactMethod email = new CompanyContactMethod();
        email.setType(CompanyContactMethodType.EMAIL);
        email.setValue("info@example.com");
        email.setPrimary(true);

        company.addContactMethod(phone);
        company.addContactMethod(email);

        assertEquals(2, company.getContactMethods().size());
        assertTrue(phone.isPrimary());
        assertTrue(email.isPrimary());
        assertSame(company, phone.getCompany());
        assertSame(company, email.getCompany());
    }
}
