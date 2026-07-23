import axios from 'axios';
import type { ApiErrorResponse } from '../types/api';

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
    if (!value || typeof value !== 'object') {
        return false;
    }

    return 'message' in value && typeof value.message === 'string';
}

export function getApiErrorMessage(
    error: unknown,
    fallbackMessage = 'İşlem tamamlanamadı',
): string {
    if (axios.isAxiosError(error) && isApiErrorResponse(error.response?.data)) {
        const apiError = error.response.data;
        const firstFieldError = Object.values(apiError.fieldErrors ?? {})[0];

        return firstFieldError ?? apiError.message;
    }

    return fallbackMessage;
}
