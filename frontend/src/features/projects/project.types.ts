export type ProjectCompanyType = 'TGB' | 'EXTERNAL';

export type ProjectStatus =
    | 'DRAFT'
    | 'AWAITING_REVIEWER'
    | 'ACTIVE'
    | 'COMPLETED'
    | 'CANCELLED';

export type ProjectListItem = {
    id: number;
    name: string;
    companyId: number;
    companyName: string;
    companyType: ProjectCompanyType;
    category: string | null;
    sector: string | null;
    startDate: string | null;
    endDate: string | null;
    status: ProjectStatus;
    activeReviewerCount: number;
    createdAt: string;
};

export type ProjectPageParams = {
    page: number;
    size: number;
};

export type ProjectUploadResponse = {
    uploadId: string;
    originalFileName: string;
    contentType: string;
    fileSize: number;
    expiresAt: string;
};

export type ProjectFormField =
    | 'COMPANY_NAME'
    | 'NAME'
    | 'PROJECT_MANAGER'
    | 'START_DATE'
    | 'DURATION_MONTHS'
    | 'CATEGORY'
    | 'SECTOR'
    | 'SUMMARY'
    | 'RESEARCH_AND_DEVELOPMENT_NATURE'
    | 'INNOVATIVE_ASPECT'
    | 'MAIN_TECHNOLOGY_AREA'
    | 'PROJECT_DETAIL'
    | 'KEYWORDS';

export type ProjectCompanyMatchStatus =
    | 'UNIQUE_MATCH'
    | 'AMBIGUOUS'
    | 'NOT_FOUND'
    | 'COMPANY_NAME_MISSING';

export type ProjectCompanyCandidate = {
    id: number;
    name: string;
    companyType: ProjectCompanyType;
};

export type ProjectCompanyMatch = {
    status: ProjectCompanyMatchStatus;
    matchedCompany: ProjectCompanyCandidate | null;
    candidates: ProjectCompanyCandidate[];
    message: string;
};

export type ProjectDocumentPreviewResponse = {
    uploadId: string;
    originalFileName: string;
    contentType: string;
    fileSize: number;
    expiresAt: string;
    templateCode: string | null;
    companyName: string | null;
    name: string | null;
    projectManager: string | null;
    startDate: string | null;
    durationMonths: number | null;
    endDate: string | null;
    category: string | null;
    sector: string | null;
    summary: string | null;
    researchAndDevelopmentNature: string | null;
    innovativeAspect: string | null;
    mainTechnologyArea: string | null;
    projectDetail: string | null;
    keywords: string[];
    missingFields: ProjectFormField[];
    reviewRequiredFields: ProjectFormField[];
    warnings: string[];
    companyMatch: ProjectCompanyMatch;
};

export type ProjectDocumentPreviewDraft = {
    name: string;
    projectManager: string;
    startDate: string;
    durationMonths: string;
    category: string;
    sector: string;
    summary: string;
    researchAndDevelopmentNature: string;
    innovativeAspect: string;
    mainTechnologyArea: string;
    projectDetail: string;
    keywords: string[];
};

export const projectPageSizes = [10, 15, 20, 25] as const;
