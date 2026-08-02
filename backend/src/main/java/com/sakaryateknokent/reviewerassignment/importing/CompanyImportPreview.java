package com.sakaryateknokent.reviewerassignment.importing;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CompanyImportPreview {

    private String fileName;
    private int sheetCount;
    private final Set<String> detectedHeaders = new LinkedHashSet<>();
    private final Set<String> ignoredHeaders = new LinkedHashSet<>();
    private final List<String> generalWarnings = new ArrayList<>();
    private final List<String> generalInformationMessages = new ArrayList<>();
    private final List<CompanyImportRow> rows = new ArrayList<>();

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getSheetCount() {
        return sheetCount;
    }

    public void setSheetCount(int sheetCount) {
        this.sheetCount = sheetCount;
    }

    public Set<String> getDetectedHeaders() {
        return detectedHeaders;
    }

    public Set<String> getIgnoredHeaders() {
        return ignoredHeaders;
    }

    public List<String> getGeneralWarnings() {
        return generalWarnings;
    }

    public List<String> getGeneralInformationMessages() {
        return generalInformationMessages;
    }

    public List<CompanyImportRow> getRows() {
        return rows;
    }
}
