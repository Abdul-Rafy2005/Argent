import apiClient from './client'
import type { ApiResponse, PagedResponse, AuditLog } from '@/types'

export const auditApi = {
  list: (params?: {
    page?: number
    pageSize?: number
    entityType?: string
    action?: string
    performedBy?: string
    startDate?: string
    endDate?: string
  }) => apiClient.get<ApiResponse<PagedResponse<AuditLog>>>('/audit-logs', { params }),

  get: (id: string) => apiClient.get<ApiResponse<AuditLog>>(`/audit-logs/${id}`),
}
