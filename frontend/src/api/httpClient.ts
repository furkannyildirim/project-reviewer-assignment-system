import axios from 'axios';
import { authStorage } from '../features/auth/authStorage';

export const AUTH_UNAUTHORIZED_EVENT = 'auth:unauthorized';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;

if (!apiBaseUrl) {
    throw new Error('VITE_API_BASE_URL ortam değişkeni tanımlı değil');
}

export const httpClient = axios.create({
    baseURL: apiBaseUrl,
    timeout: 10_000,
    headers: {
        Accept: 'application/json',
    },
});

httpClient.interceptors.request.use((config) => {
    const accessToken = authStorage.getAccessToken();

    if (accessToken && !config.url?.endsWith('/auth/login')) {
        config.headers.Authorization = `Bearer ${accessToken}`;
    }

    return config;
});

httpClient.interceptors.response.use(
    (response) => response,
    (error: unknown) => {
        if (
            axios.isAxiosError(error)
            && error.response?.status === 401
            && !error.config?.url?.endsWith('/auth/login')
        ) {
            authStorage.clearAccessToken();
            window.dispatchEvent(new Event(AUTH_UNAUTHORIZED_EVENT));
        }

        return Promise.reject(error);
    },
);
