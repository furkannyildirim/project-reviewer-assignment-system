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
    private String contactFullName;
    private String contactPosition;
    private String contactPhone;
    private String contactEmail;
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

    public String getContactFullName() {
        return contactFullName;
    }

    public void setContactFullName(String contactFullName) {
        this.contactFullName = contactFullName;
    }

    public String getContactPosition() {
        return contactPosition;
    }

    public void setContactPosition(String contactPosition) {
        this.contactPosition = contactPosition;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
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
