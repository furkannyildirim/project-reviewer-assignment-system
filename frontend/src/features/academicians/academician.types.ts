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
