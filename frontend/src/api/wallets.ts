import apiClient from './client'
import type { ApiResponse, Wallet, PagedResponse } from '@/types'

export interface CreateWalletRequest {
  label: string
  type: Wallet['type']
  metadata?: Record<string, unknown>
}

export interface UpdateWalletRequest {
  label?: string
  metadata?: Record<string, unknown>
}

export const walletApi = {
  list: (page = 0, pageSize = 20) =>
    apiClient.get<ApiResponse<PagedResponse<Wallet>>>('/wallets', {
      params: { page, pageSize },
    }),

  get: (id: string) =>
    apiClient.get<ApiResponse<Wallet>>(`/wallets/${id}`),

  create: (data: CreateWalletRequest) =>
    apiClient.post<ApiResponse<Wallet>>('/wallets', data),

  update: (id: string, data: UpdateWalletRequest) =>
    apiClient.patch<ApiResponse<Wallet>>(`/wallets/${id}`, data),

  freeze: (id: string) =>
    apiClient.post<ApiResponse<Wallet>>(`/wallets/${id}/freeze`),

  unfreeze: (id: string) =>
    apiClient.post<ApiResponse<Wallet>>(`/wallets/${id}/unfreeze`),

  close: (id: string) =>
    apiClient.post<ApiResponse<Wallet>>(`/wallets/${id}/close`),
}
