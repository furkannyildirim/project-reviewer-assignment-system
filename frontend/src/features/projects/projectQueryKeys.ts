import type { ProjectPageParams } from './project.types';

export const projectQueryKeys = {
    all: ['projects'] as const,
    list: (params: ProjectPageParams) => [
        ...projectQueryKeys.all,
        'list',
        params,
    ] as const,
};
