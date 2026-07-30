export type CompanyType = 'TGB' | 'EXTERNAL';

export type CompanyContact = {
    id: number;
    fullName: string;
    position: string | null;
    email: string | null;
    phone: string | null;
    primary: boolean;
};

export type CompanyListItem = {
    id: number;
    name: string;
    email: string | null;
    phone: string | null;
    secondaryPhone: string | null;
    activityArea: string | null;
    logoUrl: string | null;
    companyType: CompanyType;
    primaryContact: CompanyContact | null;
    contactCount: number;
};

export type CompanyDetail = {
    id: number;
    name: string;
    email: string | null;
    phone: string | null;
    secondaryPhone: string | null;
    activityArea: string | null;
    logoUrl: string | null;
    companyType: CompanyType;
    contacts: CompanyContact[];
};

export type Company = CompanyListItem;

export type CreateCompanyContactRequest = {
    fullName: string;
    position: string | null;
    email: string | null;
    phone: string | null;
    primary: boolean;
};

export type UpdateCompanyContactRequest = CreateCompanyContactRequest & {
    id: number | null;
};

export type CreateCompanyRequest = {
    name: string;
    email: string | null;
    phone: string | null;
    secondaryPhone: string | null;
    activityArea: string | null;
    logoUrl: string | null;
    companyType: CompanyType;
    contacts: CreateCompanyContactRequest[];
};

export type UpdateCompanyRequest = Omit<
    CreateCompanyRequest,
    'companyType' | 'contacts'
> & {
    contacts: UpdateCompanyContactRequest[];
};

export type CompanyImportPreviewRow = {
    sheetName: string;
    rowNumber: number;
    name: string | null;
    activityArea: string | null;
    phone: string | null;
    secondaryPhone: string | null;
    email: string | null;
    errors: string[];
    warnings: string[];
    valid: boolean;
};

export type CompanyImportPreviewResponse = {
    fileName: string;
    sheetCount: number;
    totalRowCount: number;
    validRowCount: number;
    invalidRowCount: number;
    warningCount: number;
    detectedHeaders: string[];
    ignoredHeaders: string[];
    generalWarnings: string[];
    generalInformationMessages: string[];
    rows: CompanyImportPreviewRow[];
};

export type CompanyImportResultStatus = 'IMPORTED' | 'DUPLICATE' | 'INVALID';

export type CompanyImportResultRow = {
    sheetName: string;
    rowNumber: number;
    name: string | null;
    activityArea: string | null;
    phone: string | null;
    secondaryPhone: string | null;
    email: string | null;
    status: CompanyImportResultStatus;
    messages: string[];
};

export type CompanyImportResultResponse = {
    fileName: string;
    totalRowCount: number;
    importedCount: number;
    duplicateCount: number;
    invalidCount: number;
    rows: CompanyImportResultRow[];
};

export type CompanyPageParams = {
    companyType: CompanyType;
    search: string;
    page: number;
    size: number;
};

export type CompanyFormValues = {
    name: string;
    email: string;
    phone: string;
    secondaryPhone: string;
    activityArea: string;
    logoUrl: string;
    contacts: CompanyContactFormValues[];
};

export type CompanyContactFormValues = {
    id: number | null;
    fullName: string;
    position: string;
    email: string;
    phone: string;
    primary: boolean;
};

export const companyPageSizes = [10, 15, 20, 25] as const;
