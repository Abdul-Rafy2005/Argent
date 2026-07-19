import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../utils'
import Wallets from '@/pages/Wallets'
import { walletApi } from '@/api/wallets'

vi.mock('@/api/wallets', () => ({
  walletApi: {
    list: vi.fn(),
    create: vi.fn(),
  },
}))

describe('Wallets Component', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('renders wallets from API', async () => {
    vi.mocked(walletApi.list).mockResolvedValueOnce({
      data: {
        data: {
          content: [
            { id: 'wallet-1', label: 'Main Wallet', status: 'ACTIVE', type: 'CUSTOMER', createdAt: new Date().toISOString() },
            { id: 'wallet-2', label: 'Savings', status: 'FROZEN', type: 'CUSTOMER', createdAt: new Date().toISOString() },
          ],
          totalPages: 1
        }
      }
    } as any)

    renderWithProviders(<Wallets />)

    await waitFor(() => {
      expect(screen.getByText('Main Wallet')).toBeInTheDocument()
      expect(screen.getByText('Savings')).toBeInTheDocument()
    })

    // Check status badges
    const table = screen.getByRole('table')
    expect(within(table).getByText('ACTIVE')).toBeInTheDocument()
    expect(within(table).getByText('FROZEN')).toBeInTheDocument()
  })

  it('validates and creates wallet', async () => {
    vi.mocked(walletApi.list).mockResolvedValueOnce({
      data: { data: { content: [], totalPages: 0 } }
    } as any)

    const user = userEvent.setup()
    renderWithProviders(<Wallets />)

    // Open Modal
    await user.click(screen.getByRole('button', { name: /Create Wallet/i }))
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Create Wallet' })).toBeInTheDocument())

    // Submit empty to trigger validation
    // The create button is the second one, or use specific query
    const createBtns = screen.getAllByRole('button', { name: /Create Wallet/i })
    await user.click(createBtns[createBtns.length - 1]!)
    
    await waitFor(() => {
      expect(screen.getByText(/Label must be at least 2 characters/i)).toBeInTheDocument()
    })

    // Fill valid data
    vi.mocked(walletApi.create).mockResolvedValueOnce({} as any)
    
    const labelInput = screen.getByLabelText(/Label/i)
    await user.type(labelInput, 'My New Wallet')
    
    await user.click(screen.getAllByRole('button', { name: /Create Wallet/i })[1]!)

    await waitFor(() => {
      expect(walletApi.create).toHaveBeenCalledWith({
        label: 'My New Wallet',
        type: 'CUSTOMER'
      })
      // Modal should be closed (or success toast shown)
      expect(screen.getByText(/Wallet created/i)).toBeInTheDocument()
    })
  })
})
