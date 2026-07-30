package com.sakaryateknokent.reviewerassignment.importing;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AcademicianImportRow {

    private final String sheetName;
    private final int rowNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String title;
    private Set<String> keywords = new LinkedHashSet<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public AcademicianImportRow(String sheetName, int rowNumber) {
        this.sheetName = sheetName;
        this.rowNumber = rowNumber;
    }

    public String getSheetName() {
        return sheetName;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
