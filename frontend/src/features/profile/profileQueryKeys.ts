export const profileQueryKeys = {
    all: ['admin-profile'] as const,
    detail: () => [...profileQueryKeys.all, 'detail'] as const,
};
