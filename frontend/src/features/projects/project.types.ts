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

export const projectPageSizes = [10, 15, 20, 25] as const;
