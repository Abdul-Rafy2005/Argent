import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '../utils'
import WalletDetail from '@/pages/WalletDetail'
import { walletApi } from '@/api/wallets'
import { balanceApi } from '@/api/settings'
import { transactionApi } from '@/api/transactions'
import * as router from 'react-router-dom'

vi.mock('@/api/wallets', () => ({ walletApi: { get: vi.fn() } }))
vi.mock('@/api/settings', () => ({ balanceApi: { get: vi.fn() } }))
vi.mock('@/api/transactions', () => ({ transactionApi: { listByWallet: vi.fn() } }))
vi.mock('react-router-dom', async () => {
  const mod = await vi.importActual('react-router-dom')
  return { ...mod as any, useParams: vi.fn() }
})

describe('Wallet Detail Component', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(router.useParams).mockReturnValue({ id: 'test-wallet-id' })
  })

  it('displays all balance types correctly', async () => {
    vi.mocked(walletApi.get).mockResolvedValueOnce({
      data: { data: { id: 'w1', label: 'Test Wallet', status: 'ACTIVE' } }
    } as any)
    
    vi.mocked(transactionApi.listByWallet).mockResolvedValueOnce({
      data: { data: { content: [], totalPages: 0 } }
    } as any)

    vi.mocked(balanceApi.get).mockResolvedValueOnce({
      data: {
        data: {
          current: 1500.50,
          available: 1200.00,
          pending: 200.50,
          reserved: 100.00,
        }
      }
    } as any)

    renderWithProviders(<WalletDetail />)

    await waitFor(() => {
      // Check current
      expect(screen.getByText('Current Balance')).toBeInTheDocument()
      expect(screen.getByText('$1,500.50')).toBeInTheDocument()
      
      // Check available
      expect(screen.getByText('Available Balance')).toBeInTheDocument()
      expect(screen.getByText('$1,200.00')).toBeInTheDocument()
      
      // Check pending
      expect(screen.getByText('Pending')).toBeInTheDocument()
      expect(screen.getByText('$200.50')).toBeInTheDocument()
      
      // Check reserved
      expect(screen.getByText('Reserved')).toBeInTheDocument()
      expect(screen.getByText('$100.00')).toBeInTheDocument()
    })
  })
})
