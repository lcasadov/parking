import { apiClient } from './apiClient';
import type { ChangePasswordRequest, CurrentUser, LoginRequest } from '../types/auth';

// Endpoints de Auth segun docs/openapi.yaml.

export async function getCurrentUser(): Promise<CurrentUser> {
  const { data } = await apiClient.get<CurrentUser>('/auth/me');
  return data;
}

export async function login(body: LoginRequest): Promise<CurrentUser> {
  const { data } = await apiClient.post<CurrentUser>('/auth/login', body);
  return data;
}

export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout');
}

export async function changePassword(body: ChangePasswordRequest): Promise<void> {
  await apiClient.post('/auth/change-password', body);
}
