import { httpClient } from '../../api/httpClient';
import type { LoginCredentials, LoginResponse } from './auth.types';

export async function loginRequest(
    credentials: LoginCredentials,
): Promise<LoginResponse> {
    const response = await httpClient.post<LoginResponse>(
        '/auth/login',
        credentials,
    );

    return response.data;
}
