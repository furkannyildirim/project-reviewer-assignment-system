import { httpClient } from '../../api/httpClient';
import type { PageResponse } from '../../types/api';
import type {
    Company,
    CompanyPageParams,
    CreateCompanyRequest,
    UpdateCompanyRequest,
} from './company.types';

export async function getCompanies(
    params: CompanyPageParams,
): Promise<PageResponse<Company>> {
    const response = await httpClient.get<PageResponse<Company>>('/companies', {
        params,
    });

    return response.data;
}

export async function createCompany(
    request: CreateCompanyRequest,
): Promise<Company> {
    const response = await httpClient.post<Company>('/companies', request);

    return response.data;
}

export async function updateCompany(
    id: number,
    request: UpdateCompanyRequest,
): Promise<Company> {
    const response = await httpClient.put<Company>(`/companies/${id}`, request);

    return response.data;
}

export async function deleteCompany(id: number): Promise<void> {
    await httpClient.delete(`/companies/${id}`);
}
