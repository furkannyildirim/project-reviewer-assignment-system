import { httpClient } from '../../api/httpClient';
import type {
    CreateUserRequest,
    UpdateUserRequest,
    User,
} from './user.types';

export async function getUsers(): Promise<User[]> {
    const response = await httpClient.get<User[]>('/users');

    return response.data;
}

export async function createUser(
    request: CreateUserRequest,
): Promise<User> {
    const response = await httpClient.post<User>('/users', request);

    return response.data;
}

export async function updateUser(
    id: number,
    request: UpdateUserRequest,
): Promise<User> {
    const response = await httpClient.put<User>(`/users/${id}`, request);

    return response.data;
}

export async function deleteUser(id: number): Promise<void> {
    await httpClient.delete(`/users/${id}`);
}
