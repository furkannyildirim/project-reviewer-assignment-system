import { httpClient } from '../../api/httpClient';
import type { PageResponse } from '../../types/api';
import type {
    ProjectListItem,
    ProjectPageParams,
    ProjectDocumentPreviewResponse,
    ProjectUploadResponse,
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
