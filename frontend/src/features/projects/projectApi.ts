import { httpClient } from '../../api/httpClient';
import type { PageResponse } from '../../types/api';
import type {
    ProjectListItem,
    ProjectPageParams,
    ProjectDocumentPreviewResponse,
    ProjectUploadResponse,
    CreateProjectFromUploadRequest,
    ProjectResponse,
    ProjectReviewerRecommendationsResponse,
    ProjectReviewerAssignment,
    ProjectReviewerAssignmentHistory,
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

export async function getProjectReviewerRecommendations(
    id: number,
): Promise<ProjectReviewerRecommendationsResponse> {
    const response = await httpClient.get<ProjectReviewerRecommendationsResponse>(
        `/projects/${id}/reviewer-recommendations`,
    );
    return response.data;
}

export async function getActiveProjectReviewerAssignments(
    id: number,
): Promise<ProjectReviewerAssignment[]> {
    const response = await httpClient.get<ProjectReviewerAssignment[]>(
        `/projects/${id}/reviewer-assignments`,
    );
    return response.data;
}

export async function getProjectReviewerAssignmentHistory(
    id: number,
): Promise<ProjectReviewerAssignmentHistory[]> {
    const response = await httpClient.get<ProjectReviewerAssignmentHistory[]>(
        `/projects/${id}/reviewer-assignments/history`,
    );
    return response.data;
}

export async function assignProjectReviewer(
    projectId: number,
    academicianId: number,
): Promise<ProjectReviewerAssignment> {
    const response = await httpClient.post<ProjectReviewerAssignment>(
        `/projects/${projectId}/reviewer-assignments`,
        { academicianId },
    );
    return response.data;
}

export async function removeProjectReviewerAssignment(
    projectId: number,
    assignmentId: number,
): Promise<void> {
    await httpClient.post(
        `/projects/${projectId}/reviewer-assignments/${assignmentId}/remove`,
    );
}

export async function deleteProject(id: number): Promise<void> {
    await httpClient.delete(`/projects/${id}`);
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
