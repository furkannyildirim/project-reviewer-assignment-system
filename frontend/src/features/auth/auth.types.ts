export const roles = ['ADMIN', 'USER'] as const;

export type Role = (typeof roles)[number];

export type LoginCredentials = {
    email: string;
    password: string;
};

export type LoginResponse = {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
};

export type AuthUser = {
    email: string;
};

export type JwtAccessTokenClaims = {
    sub: string;
    roles: Role[];
    exp: number;
    iat?: number;
    iss?: string;
};

export type AuthContextValue = {
    accessToken: string | null;
    isAuthenticated: boolean;
    currentUser: AuthUser | null;
    role: Role | null;
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => void;
};
