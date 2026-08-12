import { httpClient } from '../../api/httpClient';
import type {
    CurrentUserResponse,
    LoginCredentials,
    LoginResponse,
} from './auth.types';

export async function loginRequest(
    credentials: LoginCredentials,
): Promise<LoginResponse> {
    const response = await httpClient.post<LoginResponse>(
        '/auth/login',
        credentials,
    );

    return response.data;
}

export async function getCurrentUser(): Promise<CurrentUserResponse> {
    const response = await httpClient.get<CurrentUserResponse>('/auth/me');

    return response.data;
}
