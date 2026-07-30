package com.sakaryateknokent.reviewerassignment.importing;

import java.util.ArrayList;
import java.util.List;

public class CompanyImportRow {

    private final String sheetName;
    private final int rowNumber;
    private String name;
    private String phone;
    private String secondaryPhone;
    private String email;
    private String activityArea;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public CompanyImportRow(String sheetName, int rowNumber) {
        this.sheetName = sheetName;
        this.rowNumber = rowNumber;
    }

    public String getSheetName() {
        return sheetName;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSecondaryPhone() {
        return secondaryPhone;
    }

    public void setSecondaryPhone(String secondaryPhone) {
        this.secondaryPhone = secondaryPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getActivityArea() {
        return activityArea;
    }

    public void setActivityArea(String activityArea) {
        this.activityArea = activityArea;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
