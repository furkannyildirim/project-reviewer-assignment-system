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

export type CurrentUserResponse = {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    role: Role;
    active: boolean;
};

export type AuthContextValue = {
    accessToken: string | null;
    isAuthenticated: boolean;
    currentUser: CurrentUserResponse | null;
    role: Role | null;
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => void;
};
