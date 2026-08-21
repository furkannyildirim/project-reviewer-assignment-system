import type { ProjectPageParams } from './project.types';

export const projectQueryKeys = {
    all: ['projects'] as const,
    lists: () => [...projectQueryKeys.all, 'list'] as const,
    list: (params: ProjectPageParams) => [
        ...projectQueryKeys.lists(),
        params,
    ] as const,
    detail: (id: number) => [...projectQueryKeys.all, 'detail', id] as const,
    reviewerRecommendations: (id: number) => [
        ...projectQueryKeys.all,
        'reviewer-recommendations',
        id,
    ] as const,
    activeReviewerAssignments: (id: number) => [
        ...projectQueryKeys.all,
        'active-reviewer-assignments',
        id,
    ] as const,
    reviewerAssignmentHistory: (id: number) => [
        ...projectQueryKeys.all,
        'reviewer-assignment-history',
        id,
    ] as const,
};
