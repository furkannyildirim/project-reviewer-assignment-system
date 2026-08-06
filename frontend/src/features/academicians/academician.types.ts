export type Academician = {
    id: number;
    version: number;
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

export type UpdateAcademicianRequest = CreateAcademicianRequest & {
    version: number;
};

export type AcademicianImportPreviewRow = {
    sheetName: string;
    rowNumber: number;
    firstName: string | null;
    lastName: string | null;
    email: string | null;
    phone: string | null;
    title: string | null;
    keywords: string[];
    status: AcademicianImportPreviewRowStatus;
    valid: boolean;
    messages: string[];
    warnings: string[];
    errors: string[];
};

export type AcademicianImportPreviewRowStatus =
    | 'NEW'
    | 'UPDATE'
    | 'DUPLICATE'
    | 'CONFLICT'
    | 'INVALID';

export type AcademicianImportPreviewResponse = {
    fileName: string;
    sheetCount: number;
    totalDetectedRows: number;
    newRowCount: number;
    updateRowCount: number;
    duplicateRowCount: number;
    conflictRowCount: number;
    validRowCount: number;
    invalidRowCount: number;
    warningCount: number;
    detectedHeaders: string[];
    ignoredHeaders: string[];
    generalWarnings: string[];
    generalInformationMessages: string[];
    rows: AcademicianImportPreviewRow[];
};

export type AcademicianImportResultRowStatus =
    | 'CREATED'
    | 'UPDATED'
    | 'DUPLICATE'
    | 'CONFLICT'
    | 'INVALID'
    | 'FAILED';

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
    updatedCount: number;
    duplicateCount: number;
    conflictCount: number;
    invalidCount: number;
    failedCount: number;
    warningCount: number;
    createdRows: AcademicianImportResultRow[];
    updatedRows: AcademicianImportResultRow[];
    duplicateRows: AcademicianImportResultRow[];
    conflictRows: AcademicianImportResultRow[];
    invalidRows: AcademicianImportResultRow[];
    failedRows: AcademicianImportResultRow[];
    generalWarnings: string[];
};

export type AcademicianPageParams = {
    search: string;
    initial?: string;
    page: number;
    size: number;
};

export type AcademicianExportParams = {
    ids: number[];
};

export type AcademicianIdParams = {
    search: string;
    initial?: string;
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
