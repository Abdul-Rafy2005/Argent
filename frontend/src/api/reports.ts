import apiClient from './client'
import type { ApiResponse } from '@/types'

export interface DailyVolumeResponse {
  date: string
  transactionCount: number
  totalAmount: number
}

export interface WalletGrowthResponse {
  date: string
  walletsCreated: number
}

export const reportsApi = {
  getDailyVolume: (params?: { startDate?: string; endDate?: string }) =>
    apiClient.get<ApiResponse<DailyVolumeResponse[]>>('/reports/daily-volume', { params }),

  getWalletGrowth: (params?: { startDate?: string; endDate?: string }) =>
    apiClient.get<ApiResponse<WalletGrowthResponse[]>>('/reports/wallet-growth', { params }),
    
  exportStatementUrl: (startDate?: string, endDate?: string) => {
      const params = new URLSearchParams();
      if (startDate) params.append('startDate', startDate);
      if (endDate) params.append('endDate', endDate);
      return `/api/v1/statements${params.toString() ? `?${params.toString()}` : ''}`;
  }
}
