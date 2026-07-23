const ACCESS_TOKEN_KEY = 'reviewer-assignment.access-token';

export const authStorage = {
    getAccessToken(): string | null {
        return sessionStorage.getItem(ACCESS_TOKEN_KEY);
    },

    setAccessToken(accessToken: string): void {
        sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    },

    clearAccessToken(): void {
        sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    },
};