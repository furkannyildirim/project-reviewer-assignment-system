import type { AcademicianPageParams } from './academician.types';

export const academicianQueryKeys = {
    all: ['academicians'] as const,
    list: (params: AcademicianPageParams) => [
        ...academicianQueryKeys.all,
        'list',
        params,
    ] as const,
};
