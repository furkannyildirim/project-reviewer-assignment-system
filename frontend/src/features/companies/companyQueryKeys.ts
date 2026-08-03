import type { CompanyPageParams } from './company.types';

export const companyQueryKeys = {
    all: ['companies'] as const,
    list: (params: CompanyPageParams) => [
        ...companyQueryKeys.all,
        'list',
        params,
    ] as const,
    detail: (id: number) => [
        ...companyQueryKeys.all,
        'detail',
        id,
    ] as const,
    count: (companyType: string) => [
        ...companyQueryKeys.all,
        'count',
        companyType,
    ] as const,
};
