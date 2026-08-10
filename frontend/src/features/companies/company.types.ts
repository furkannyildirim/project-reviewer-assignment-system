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
    companyType: CompanyType;
    contactMethods: CompanyContactMethod[];
    primaryContact: CompanyContact | null;
    contactCount: number;
};

export type CompanyDetail = {
    id: number;
    name: string;
    activityArea: string | null;
    city: string | null;
    district: string | null;
    osb: string | null;
    address: string | null;
    building: string | null;
    companyType: CompanyType;
    contactMethods: CompanyContactMethod[];
    contacts: CompanyContact[];
};

export type Company = CompanyListItem;

export type CompanyNameWarningResponse = {
    warning: boolean;
    message: string | null;
};

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
    city: string | null;
    district: string | null;
    osb: string | null;
    address: string | null;
    building: string | null;
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
    status: 'NEW' | 'EXISTING' | 'DUPLICATE' | 'WARNING' | 'INVALID';
    city: string | null;
    district: string | null;
    osb: string | null;
    address: string | null;
    building: string | null;
    companyPhones: string[];
    companyEmails: string[];
    contactFullName: string | null;
    contactPosition: string | null;
    contactPhone: string | null;
    contactEmail: string | null;
    updateMessages: string[];
    locationWarnings: string[];
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

export type CompanyImportResultStatus = 'IMPORTED' | 'DUPLICATE' | 'INVALID' | 'FAILED';

export type CompanyImportResultRow = {
    sheetName: string;
    rowNumber: number;
    name: string | null;
    activityArea: string | null;
    city: string | null;
    district: string | null;
    osb: string | null;
    address: string | null;
    building: string | null;
    companyPhones: string[];
    companyEmails: string[];
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
    updatedCount: number;
    duplicateCount: number;
    invalidCount: number;
    rows: CompanyImportResultRow[];
};

export type CompanyPageParams = {
    companyType: CompanyType;
    search: string;
    initial?: string;
    page: number;
    size: number;
};

export type CompanyExportParams = {
    companyType: CompanyType;
    ids: number[];
};

export type CompanyIdParams = {
    companyType: CompanyType;
    search: string;
    initial?: string;
};

export type CompanyFormValues = {
    name: string;
    activityArea: string;
    city: string;
    district: string;
    osb: string;
    address: string;
    building: string;
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
