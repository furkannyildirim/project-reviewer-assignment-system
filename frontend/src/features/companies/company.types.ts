export type Company = {
    id: number;
    name: string;
    email: string | null;
    phone: string | null;
    activityArea: string | null;
    logoUrl: string | null;
};

export type CreateCompanyRequest = {
    name: string;
    email: string | null;
    phone: string | null;
    activityArea: string | null;
    logoUrl: string | null;
};

export type UpdateCompanyRequest = CreateCompanyRequest;

export type CompanyPageParams = {
    search: string;
    page: number;
    size: number;
};

export type CompanyFormValues = {
    name: string;
    email: string;
    phone: string;
    activityArea: string;
    logoUrl: string;
};

export const companyPageSizes = [10, 15, 20, 25] as const;
