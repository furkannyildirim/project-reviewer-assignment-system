import { httpClient } from '../../api/httpClient';
import type {
    AdminProfile,
    ChangeAdminPasswordRequest,
    UpdateAdminProfileRequest,
} from './profile.types';

export async function getAdminProfile(): Promise<AdminProfile> {
    const response = await httpClient.get<AdminProfile>('/admin/profile');
    return response.data;
}

export async function updateAdminProfile(
    request: UpdateAdminProfileRequest,
): Promise<AdminProfile> {
    const response = await httpClient.put<AdminProfile>(
        '/admin/profile',
        request,
    );
    return response.data;
}

export async function changeAdminPassword(
    request: ChangeAdminPasswordRequest,
): Promise<void> {
    await httpClient.put('/admin/profile/password', request);
}
