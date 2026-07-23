import axios from 'axios';
import { authStorage } from '../features/auth/authStorage';

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

    if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
    }

    return config;
});