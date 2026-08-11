export type AdminProfile = {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    role: 'ADMIN';
    active: boolean;
};

export type UpdateAdminProfileRequest = {
    firstName: string;
    lastName: string;
    phone: string;
};

export type ChangeAdminPasswordRequest = {
    currentPassword: string;
    newPassword: string;
};
