import { httpClient } from '../../api/httpClient';
import type { PageResponse } from '../../types/api';
import type {
    Academician,
    AcademicianImportPreviewResponse,
    AcademicianImportResultResponse,
    AcademicianPageParams,
    CreateAcademicianRequest,
    UpdateAcademicianRequest,
} from './academician.types';

export async function getAcademicians(
    params: AcademicianPageParams,
): Promise<PageResponse<Academician>> {
    const requestParams = {
        ...params,
        initial: params.initial || undefined,
    };

    const response = await httpClient.get<PageResponse<Academician>>(
        '/academicians',
        { params: requestParams },
    );

    return response.data;
}

export async function getAcademicianCount(): Promise<number> {
    const response = await httpClient.get<number>('/academicians/count');

    return response.data;
}

export async function importAcademicians(
    file: File,
): Promise<AcademicianImportResultResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await httpClient.post<AcademicianImportResultResponse>(
        '/academicians/import',
        formData,
    );

    return response.data;
}

export async function createAcademician(
    request: CreateAcademicianRequest,
): Promise<Academician> {
    const response = await httpClient.post<Academician>(
        '/academicians',
        request,
    );

    return response.data;
}

export async function updateAcademician(
    id: number,
    request: UpdateAcademicianRequest,
): Promise<Academician> {
    const response = await httpClient.put<Academician>(
        `/academicians/${id}`,
        request,
    );

    return response.data;
}

export async function deleteAcademician(id: number): Promise<void> {
    await httpClient.delete(`/academicians/${id}`);
}

export async function previewAcademicianImport(
    file: File,
): Promise<AcademicianImportPreviewResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await httpClient.post<AcademicianImportPreviewResponse>(
        '/academicians/import/preview',
        formData,
    );

    return response.data;
}
