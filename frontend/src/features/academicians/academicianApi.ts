import { httpClient } from '../../api/httpClient';
import type { PageResponse } from '../../types/api';
import type {
    Academician,
    AcademicianPageParams,
    CreateAcademicianRequest,
    UpdateAcademicianRequest,
} from './academician.types';

export async function getAcademicians(
    params: AcademicianPageParams,
): Promise<PageResponse<Academician>> {
    const response = await httpClient.get<PageResponse<Academician>>(
        '/academicians',
        { params },
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
