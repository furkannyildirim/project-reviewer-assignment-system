import { httpClient } from '../../api/httpClient';
import type { PageResponse } from '../../types/api';
import type {
    CompanyDetail,
    CompanyImportPreviewResponse,
    CompanyImportResultResponse,
    CompanyListItem,
    CompanyPageParams,
    CreateCompanyRequest,
    UpdateCompanyRequest,
} from './company.types';

export async function getCompanies(
    params: CompanyPageParams,
): Promise<PageResponse<CompanyListItem>> {
    const response = await httpClient.get<PageResponse<CompanyListItem>>(
        '/companies',
        { params },
    );

    return response.data;
}

export async function getCompanyById(id: number): Promise<CompanyDetail> {
    const response = await httpClient.get<CompanyDetail>(`/companies/${id}`);

    return response.data;
}

export async function previewCompanyImport(
    file: File,
): Promise<CompanyImportPreviewResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await httpClient.post<CompanyImportPreviewResponse>(
        '/companies/import/preview',
        formData,
    );

    return response.data;
}

export async function importCompaniesFromExcel(
    file: File,
): Promise<CompanyImportResultResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await httpClient.post<CompanyImportResultResponse>(
        '/companies/import',
        formData,
    );

    return response.data;
}

export async function createCompany(
    request: CreateCompanyRequest,
): Promise<CompanyDetail> {
    const response = await httpClient.post<CompanyDetail>('/companies', request);

    return response.data;
}

export async function updateCompany(
    id: number,
    request: UpdateCompanyRequest,
): Promise<CompanyDetail> {
    const response = await httpClient.put<CompanyDetail>(
        `/companies/${id}`,
        request,
    );

    return response.data;
}

export async function deleteCompany(id: number): Promise<void> {
    await httpClient.delete(`/companies/${id}`);
}
