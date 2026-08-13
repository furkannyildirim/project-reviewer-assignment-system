import { httpClient } from '../../api/httpClient';
import type { PageResponse } from '../../types/api';
import type { ProjectListItem, ProjectPageParams } from './project.types';

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
