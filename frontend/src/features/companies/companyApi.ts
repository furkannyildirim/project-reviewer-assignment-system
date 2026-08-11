import { httpClient } from '../../api/httpClient';
import type { PageResponse } from '../../types/api';
import type {
    CompanyDetail,
    CompanyExportParams,
    CompanyIdParams,
    CompanyImportPreviewResponse,
    CompanyImportResultResponse,
    CompanyListItem,
    CompanyNameWarningResponse,
    CompanyPageParams,
    CompanyType,
    CreateCompanyRequest,
    UpdateCompanyRequest,
} from './company.types';

const companyImportTimeoutMs = 120_000;

export async function getCompanies(
    params: CompanyPageParams,
): Promise<PageResponse<CompanyListItem>> {
    const requestParams = {
        ...params,
        initial: params.initial || undefined,
    };

    const response = await httpClient.get<PageResponse<CompanyListItem>>(
        '/companies',
        { params: requestParams },
    );

    return response.data;
}

export async function exportCompanies(
    params: CompanyExportParams,
): Promise<Blob> {
    const response = await httpClient.post<Blob>(
        '/companies/export',
        { ids: params.ids },
        {
            params: { companyType: params.companyType },
            responseType: 'blob',
        },
    );

    return response.data;
}

export async function getCompanyIds(params: CompanyIdParams): Promise<number[]> {
    const requestParams = {
        companyType: params.companyType,
        search: params.search || undefined,
        initial: params.initial || undefined,
    };

    const response = await httpClient.get<number[]>(
        '/companies/ids',
        { params: requestParams },
    );

    return response.data;
}

export async function getCompanyCount(
    companyType: CompanyType,
): Promise<number> {
    const response = await httpClient.get<number>(
        '/companies/count',
        { params: { companyType } },
    );

    return response.data;
}

export async function getExternalCompanyNameWarning(
    name: string,
    excludeCompanyId?: number,
): Promise<CompanyNameWarningResponse> {
    const response = await httpClient.get<CompanyNameWarningResponse>(
        '/companies/external-name-warning',
        { params: { name, excludeCompanyId } },
    );

    return response.data;
}

export async function getCompanyById(id: number): Promise<CompanyDetail> {
    const response = await httpClient.get<CompanyDetail>(`/companies/${id}`);

    return response.data;
}

export async function previewCompanyImport(
    file: File,
    companyType: CompanyType = 'TGB',
): Promise<CompanyImportPreviewResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const endpoint = companyType === 'EXTERNAL'
        ? '/companies/external/import/preview'
        : '/companies/import/preview';

    const response = await httpClient.post<CompanyImportPreviewResponse>(
        endpoint,
        formData,
        { timeout: companyImportTimeoutMs },
    );

    return response.data;
}

export async function importCompaniesFromExcel(
    file: File,
    companyType: CompanyType = 'TGB',
): Promise<CompanyImportResultResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const endpoint = companyType === 'EXTERNAL'
        ? '/companies/external/import'
        : '/companies/import';

    const response = await httpClient.post<CompanyImportResultResponse>(
        endpoint,
        formData,
        { timeout: companyImportTimeoutMs },
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
