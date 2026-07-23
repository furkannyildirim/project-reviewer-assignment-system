import type { CompanyPageParams } from './company.types';

export const companyQueryKeys = {
    all: ['companies'] as const,
    list: (params: CompanyPageParams) => [
        ...companyQueryKeys.all,
        'list',
        params,
    ] as const,
};
