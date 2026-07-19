import apiClient from './client'
import type { ApiResponse, PagedResponse, LedgerEntry } from '@/types'

export const ledgerApi = {
  list: (params?: {
    page?: number
    pageSize?: number
    accountId?: string
    transactionId?: string
    startDate?: string
    endDate?: string
  }) => apiClient.get<ApiResponse<PagedResponse<LedgerEntry>>>('/ledger/entries', { params }),

  get: (id: string) => apiClient.get<ApiResponse<LedgerEntry>>(`/ledger/entries/${id}`),
}
