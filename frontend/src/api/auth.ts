import apiClient from './client'
import type { ApiResponse } from '@/types'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  name: string
  organizationName: string
}

export interface UserInfo {
  id: string
  email: string
  name: string
  role: string
  organizationId: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: UserInfo
}

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<ApiResponse<AuthResponse>>('/auth/login', data),

  register: (data: RegisterRequest) =>
    apiClient.post<ApiResponse<AuthResponse>>('/auth/register', data),

  refresh: (refreshToken: string) =>
    apiClient.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken }),

  getMe: () => apiClient.get<ApiResponse<UserInfo>>('/auth/me'),
}
