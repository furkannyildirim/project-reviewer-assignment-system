export type CompanyType = 'TGB' | 'EXTERNAL';

export type CompanyContactMethodType = 'PHONE' | 'EMAIL';

export type CompanyContactMethod = {
    id: number;
    type: CompanyContactMethodType;
    value: string;
    label: string | null;
    primary: boolean;
};

export type CompanyContactMethodRequest = {
    id: number | null;
    type: CompanyContactMethodType;
    value: string;
    label: string | null;
    primary: boolean;
};

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
    activityArea: string | null;
    logoUrl: string | null;
    companyType: CompanyType;
    contactMethods: CompanyContactMethod[];
    primaryContact: CompanyContact | null;
    contactCount: number;
};

export type CompanyDetail = {
    id: number;
    name: string;
    activityArea: string | null;
    logoUrl: string | null;
    companyType: CompanyType;
    contactMethods: CompanyContactMethod[];
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
    activityArea: string | null;
    logoUrl: string | null;
    companyType: CompanyType;
    contactMethods: CompanyContactMethodRequest[];
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
    contactFullName: string | null;
    contactPosition: string | null;
    contactPhone: string | null;
    contactEmail: string | null;
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
    contactFullName: string | null;
    contactPosition: string | null;
    contactPhone: string | null;
    contactEmail: string | null;
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
    activityArea: string;
    logoUrl: string;
    contactMethods: CompanyContactMethodFormValues[];
    contacts: CompanyContactFormValues[];
};

export type CompanyContactMethodFormValues = {
    id: number | null;
    type: CompanyContactMethodType;
    value: string;
    label: string;
    primary: boolean;
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
