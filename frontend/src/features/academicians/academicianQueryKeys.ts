import type {
    AcademicianIdParams,
    AcademicianPageParams,
} from './academician.types';

export const academicianQueryKeys = {
    all: ['academicians'] as const,
    list: (params: AcademicianPageParams) => [
        ...academicianQueryKeys.all,
        'list',
        params,
    ] as const,
    count: () => [
        ...academicianQueryKeys.all,
        'count',
    ] as const,
    ids: (params: AcademicianIdParams) => [
        ...academicianQueryKeys.all,
        'ids',
        params,
    ] as const,
};
