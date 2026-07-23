export type UserRole = 'ADMIN' | 'USER';

export type User = {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    role: UserRole;
    active: boolean;
};

export type CreateUserRequest = {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    password: string;
};

export type UpdateUserRequest = {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    active: boolean;
};

export type UserFormValues = {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    password: string;
    active: boolean;
};

export const userPageSizes = [10, 15, 20, 25] as const;
