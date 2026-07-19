export interface ApiResponse<T> {
  success: boolean
  data: T
  error?: ErrorInfo
  meta?: Meta
}

export interface ErrorInfo {
  code: string
  message: string
  details?: Record<string, unknown>
}

export interface Meta {
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface Organization {
  id: string
  name: string
  slug: string
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED'
  createdAt: string
  updatedAt: string
}

export interface User {
  id: string
  organizationId: string
  email: string
  name: string
  role: 'OWNER' | 'ADMIN' | 'DEVELOPER'
  status: 'ACTIVE' | 'INVITED' | 'DISABLED'
  createdAt: string
}

export interface ApiKey {
  id: string
  organizationId: string
  name: string
  keyPrefix: string
  environment: 'SANDBOX' | 'PRODUCTION'
  status: 'ACTIVE' | 'REVOKED'
  createdAt: string
}

export interface Wallet {
  id: string
  organizationId: string
  accountId: string
  label: string
  type: 'CUSTOMER' | 'MERCHANT' | 'ESCROW' | 'REWARD' | 'CREDIT' | 'PLATFORM'
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED'
  metadata?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface Account {
  id: string
  organizationId: string
  type: 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE'
  name: string
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED'
  createdAt: string
}

export interface Transaction {
  id: string
  organizationId: string
  type: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER' | 'REFUND' | 'ADJUSTMENT' | 'FEE' | 'COMMISSION' | 'BONUS' | 'REVERSAL'
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  sourceWalletId?: string
  destinationWalletId?: string
  amount: number
  idempotencyKey?: string
  reference?: string
  description?: string
  metadata?: Record<string, unknown>
  createdAt: string
  completedAt?: string
}

export interface LedgerEntry {
  id: string
  organizationId: string
  transactionId: string
  accountId: string
  type: 'DEBIT' | 'CREDIT'
  amount: number
  balanceAfter: number
  description?: string
  createdAt: string
}

export interface Balance {
  id: string
  accountId: string
  current: number
  available: number
  pending: number
  reserved: number
  updatedAt: string
}

export interface AuditLog {
  id: string
  organizationId: string
  entityType: string
  entityId: string
  action: string
  performedBy?: string
  previousState?: Record<string, unknown>
  newState?: Record<string, unknown>
  ipAddress?: string
  userAgent?: string
  createdAt: string
}

export interface Webhook {
  id: string
  organizationId: string
  url: string
  events: string[]
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
}
