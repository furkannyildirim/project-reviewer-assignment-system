export type Academician = {
    id: number;
    firstName: string;
    lastName: string;
    email: string | null;
    phone: string | null;
    title: string | null;
    keywords: string[];
};

export type CreateAcademicianRequest = {
    firstName: string;
    lastName: string;
    email: string | null;
    phone: string | null;
    title: string | null;
    keywords: string[];
};

export type UpdateAcademicianRequest = CreateAcademicianRequest;

export type AcademicianImportPreviewRow = {
    sheetName: string;
    rowNumber: number;
    firstName: string | null;
    lastName: string | null;
    email: string | null;
    phone: string | null;
    title: string | null;
    keywords: string[];
    valid: boolean;
    warnings: string[];
    errors: string[];
};

export type AcademicianImportPreviewResponse = {
    fileName: string;
    sheetCount: number;
    totalDetectedRows: number;
    validRowCount: number;
    invalidRowCount: number;
    warningCount: number;
    detectedHeaders: string[];
    ignoredHeaders: string[];
    generalWarnings: string[];
    rows: AcademicianImportPreviewRow[];
};

export type AcademicianImportResultRowStatus = 'CREATED' | 'SKIPPED_DUPLICATE';

export type AcademicianImportResultRow = {
    sheetName: string;
    rowNumber: number;
    firstName: string | null;
    lastName: string | null;
    email: string | null;
    title: string | null;
    status: AcademicianImportResultRowStatus;
    messages: string[];
};

export type AcademicianImportResultResponse = {
    fileName: string;
    totalDetectedRows: number;
    createdCount: number;
    skippedDuplicateCount: number;
    warningCount: number;
    createdRows: AcademicianImportResultRow[];
    skippedRows: AcademicianImportResultRow[];
    generalWarnings: string[];
};

export type AcademicianPageParams = {
    search: string;
    page: number;
    size: number;
};

export type AcademicianFormValues = {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    title: string;
    keywordsText: string;
};

export const academicianPageSizes = [10, 15, 20, 25] as const;
