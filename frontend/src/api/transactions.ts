import apiClient from './client'
import type { ApiResponse, PagedResponse, Transaction } from '@/types'

export interface DepositRequest {
  walletId: string
  amount: number
  idempotencyKey?: string
  reference?: string
  description?: string
  metadata?: Record<string, unknown>
}

export interface WithdrawRequest {
  walletId: string
  amount: number
  idempotencyKey?: string
  reference?: string
  description?: string
  metadata?: Record<string, unknown>
}

export interface TransferRequest {
  sourceWalletId: string
  destinationWalletId: string
  amount: number
  idempotencyKey?: string
  reference?: string
  description?: string
  metadata?: Record<string, unknown>
}

export interface RefundRequest {
  transactionId: string
  idempotencyKey?: string
  reference?: string
  description?: string
  metadata?: Record<string, unknown>
}

export const transactionApi = {
  list: (params?: {
    page?: number
    pageSize?: number
    type?: Transaction['type']
    status?: Transaction['status']
    startDate?: string
    endDate?: string
  }) =>
    apiClient.get<ApiResponse<PagedResponse<Transaction>>>('/transactions', { params }),

  listByWallet: (walletId: string, page = 0, pageSize = 20) =>
    apiClient.get<ApiResponse<PagedResponse<Transaction>>>(`/transactions/wallet/${walletId}`, {
      params: { page, pageSize },
    }),

  get: (id: string) =>
    apiClient.get<ApiResponse<Transaction>>(`/transactions/${id}`),

  deposit: (data: DepositRequest) =>
    apiClient.post<ApiResponse<Transaction>>('/transactions/deposit', data, {
      headers: data.idempotencyKey ? { 'Idempotency-Key': data.idempotencyKey } : {},
    }),

  withdraw: (data: WithdrawRequest) =>
    apiClient.post<ApiResponse<Transaction>>('/transactions/withdraw', data, {
      headers: data.idempotencyKey ? { 'Idempotency-Key': data.idempotencyKey } : {},
    }),

  transfer: (data: TransferRequest) =>
    apiClient.post<ApiResponse<Transaction>>('/transactions/transfer', data, {
      headers: data.idempotencyKey ? { 'Idempotency-Key': data.idempotencyKey } : {},
    }),

  refund: (data: RefundRequest) =>
    apiClient.post<ApiResponse<Transaction>>('/transactions/refund', data, {
      headers: data.idempotencyKey ? { 'Idempotency-Key': data.idempotencyKey } : {},
    }),
}
