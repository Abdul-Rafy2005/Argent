import apiClient from './client'
import type { ApiResponse, ApiKey, Organization, Balance } from '@/types'

export const organizationApi = {
  get: () => apiClient.get<ApiResponse<Organization[]>>(`/organizations`),
  getById: (id: string) => apiClient.get<ApiResponse<Organization>>(`/organizations/${id}`),
}

export const apiKeyApi = {
  list: () => apiClient.get<ApiResponse<ApiKey[]>>('/api-keys'),
  create: (data: { name: string; environment: 'SANDBOX' | 'PRODUCTION' }) =>
    apiClient.post<ApiResponse<ApiKey & { rawKey: string }>>('/api-keys', data),
  revoke: (id: string) => apiClient.delete<ApiResponse<void>>(`/api-keys/${id}`),
}

export const balanceApi = {
    get: (walletId: string) => apiClient.get<ApiResponse<Balance>>(`/balances/${walletId}`)
}
