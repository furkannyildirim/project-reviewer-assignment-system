import { httpClient } from '../../api/httpClient';
import type { PageResponse } from '../../types/api';
import type {
    ProjectListItem,
    ProjectPageParams,
    ProjectDocumentPreviewResponse,
    ProjectUploadResponse,
    CreateProjectFromUploadRequest,
    ProjectResponse,
} from './project.types';

export async function getProjects(
    params: ProjectPageParams,
): Promise<PageResponse<ProjectListItem>> {
    const response = await httpClient.get<PageResponse<ProjectListItem>>(
        '/projects',
        {
            params: {
                ...params,
                sort: 'createdAt,desc',
            },
        },
    );

    return response.data;
}

export async function getProjectById(id: number): Promise<ProjectResponse> {
    const response = await httpClient.get<ProjectResponse>(`/projects/${id}`);
    return response.data;
}

export async function downloadProjectDocument(
    id: number,
): Promise<{ blob: Blob; fileName: string | null }> {
    const response = await httpClient.get<Blob>(`/projects/${id}/document`, {
        responseType: 'blob',
    });
    const contentDisposition = response.headers['content-disposition'];
    let fileName: string | null = null;

    if (typeof contentDisposition === 'string') {
        const encodedMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
        const quotedMatch = contentDisposition.match(/filename="([^"]+)"/i);
        const headerValue = encodedMatch?.[1] ?? quotedMatch?.[1];
        if (headerValue) {
            try {
                fileName = decodeURIComponent(headerValue);
            } catch {
                fileName = headerValue;
            }
            fileName = fileName.split(/[\\/]/).pop()?.replace(/[\u0000-\u001F\u007F]/g, '') || null;
        }
    }

    return { blob: response.data, fileName };
}

export async function previewProjectDocument(
    uploadId: string,
): Promise<ProjectDocumentPreviewResponse> {
    const response = await httpClient.post<ProjectDocumentPreviewResponse>(
        `/projects/uploads/${uploadId}/preview`,
    );

    return response.data;
}

export async function uploadProjectDocument(
    file: File,
): Promise<ProjectUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await httpClient.post<ProjectUploadResponse>(
        '/projects/uploads',
        formData,
    );

    return response.data;
}

export async function createProjectFromUpload(
    request: CreateProjectFromUploadRequest,
): Promise<ProjectResponse> {
    const response = await httpClient.post<ProjectResponse>(
        '/projects/from-upload',
        request,
    );

    return response.data;
}
