import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../utils'
import WalletDetail from '@/pages/WalletDetail'
import { walletApi } from '@/api/wallets'
import { balanceApi } from '@/api/settings'
import { transactionApi } from '@/api/transactions'
import * as router from 'react-router-dom'

vi.mock('@/api/wallets', () => ({ walletApi: { get: vi.fn() } }))
vi.mock('@/api/settings', () => ({ balanceApi: { get: vi.fn() } }))
vi.mock('@/api/transactions', () => ({
  transactionApi: {
    listByWallet: vi.fn(),
    deposit: vi.fn(),
    transfer: vi.fn(),
  },
}))
vi.mock('react-router-dom', async () => {
  const mod = await vi.importActual('react-router-dom')
  return { ...mod as any, useParams: vi.fn() }
})

describe('Wallet Deposit → Transfer Flow', () => {
  const walletId = 'wallet-source-123'

  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(router.useParams).mockReturnValue({ id: walletId })
  })

  it('deposit funds then transfer to another wallet with balance updates', async () => {
    const user = userEvent.setup()

    // ── Initial state: wallet with $0 balance ──
    vi.mocked(walletApi.get).mockResolvedValue({
      data: { data: { id: walletId, label: 'My Wallet', status: 'ACTIVE', type: 'CUSTOMER' } },
    } as any)

    vi.mocked(balanceApi.get).mockResolvedValueOnce({
      data: { data: { current: 0, available: 0, pending: 0, reserved: 0 } },
    } as any)

    vi.mocked(transactionApi.listByWallet).mockResolvedValueOnce({
      data: { data: { content: [], totalPages: 0 } },
    } as any)

    renderWithProviders(<WalletDetail />)

    // ── Verify initial $0 balance renders ──
    await waitFor(() => {
      expect(screen.getByText('My Wallet')).toBeInTheDocument()
      expect(screen.getByText('Available Balance')).toBeInTheDocument()
    })

    // ── Step 1: Deposit $500 ──
    await user.click(screen.getByRole('button', { name: /^Deposit$/i }))
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /Deposit Funds/i })).toBeInTheDocument()
    )

    // Mock deposit API success
    vi.mocked(transactionApi.deposit).mockResolvedValueOnce({
      data: {
        data: {
          id: 'txn-deposit-1',
          type: 'DEPOSIT',
          status: 'COMPLETED',
          amount: 500,
          destinationWalletId: walletId,
          createdAt: new Date().toISOString(),
        },
      },
    } as any)

    // After deposit, balance refetches → $500
    vi.mocked(balanceApi.get).mockResolvedValueOnce({
      data: { data: { current: 500, available: 500, pending: 0, reserved: 0 } },
    } as any)

    vi.mocked(transactionApi.listByWallet).mockResolvedValueOnce({
      data: {
        data: {
          content: [{
            id: 'txn-deposit-1',
            type: 'DEPOSIT',
            status: 'COMPLETED',
            amount: 500,
            destinationWalletId: walletId,
            createdAt: new Date().toISOString(),
          }],
          totalPages: 1,
        },
      },
    } as any)

    // Fill deposit form and submit
    await user.type(screen.getByLabelText(/Amount/i), '500')
    // The modal has a "Deposit" submit button — get the last one (the one inside the modal, not the page button)
    const depositBtns = screen.getAllByRole('button', { name: /^Deposit$/i })
    await user.click(depositBtns[depositBtns.length - 1]!)

    // Assert API was called correctly
    await waitFor(() => {
      expect(transactionApi.deposit).toHaveBeenCalledWith({
        walletId,
        amount: 500,
      })
    })

    // Verify balance updated to $500 (appears in Available Balance and Current Balance)
    await waitFor(() => {
      const matches = screen.getAllByText('$500.00')
      expect(matches.length).toBeGreaterThanOrEqual(2) // Available + Current
    })

    // Verify success toast
    expect(screen.getByText(/Deposit successful/i)).toBeInTheDocument()

    // ── Step 2: Transfer $200 to wallet-dest ──
    await user.click(screen.getByRole('button', { name: /^Transfer$/i }))
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /Transfer Funds/i })).toBeInTheDocument()
    )

    // Mock transfer API success
    vi.mocked(transactionApi.transfer).mockResolvedValueOnce({
      data: {
        data: {
          id: 'txn-transfer-1',
          type: 'TRANSFER',
          status: 'COMPLETED',
          amount: 200,
          sourceWalletId: walletId,
          destinationWalletId: 'wallet-dest-456',
          createdAt: new Date().toISOString(),
        },
      },
    } as any)

    // After transfer, balance refetches → $300 ($500 - $200)
    vi.mocked(balanceApi.get).mockResolvedValueOnce({
      data: { data: { current: 300, available: 300, pending: 0, reserved: 0 } },
    } as any)

    vi.mocked(transactionApi.listByWallet).mockResolvedValueOnce({
      data: {
        data: {
          content: [
            {
              id: 'txn-transfer-1',
              type: 'TRANSFER',
              status: 'COMPLETED',
              amount: 200,
              sourceWalletId: walletId,
              destinationWalletId: 'wallet-dest-456',
              createdAt: new Date().toISOString(),
            },
            {
              id: 'txn-deposit-1',
              type: 'DEPOSIT',
              status: 'COMPLETED',
              amount: 500,
              destinationWalletId: walletId,
              createdAt: new Date().toISOString(),
            },
          ],
          totalPages: 1,
        },
      },
    } as any)

    // Fill transfer form
    await user.type(screen.getByLabelText(/Destination Wallet ID/i), 'wallet-dest-456')
    await user.type(screen.getByLabelText(/Amount/i), '200')

    const transferBtns = screen.getAllByRole('button', { name: /^Transfer$/i })
    await user.click(transferBtns[transferBtns.length - 1]!)

    // Assert API was called correctly
    await waitFor(() => {
      expect(transactionApi.transfer).toHaveBeenCalledWith({
        sourceWalletId: walletId,
        destinationWalletId: 'wallet-dest-456',
        amount: 200,
      })
    })

    // Verify balance updated to $300 after transfer ($500 - $200)
    await waitFor(() => {
      const matches = screen.getAllByText('$300.00')
      expect(matches.length).toBeGreaterThanOrEqual(2) // Available + Current
    })

    // Verify success toast
    expect(screen.getByText(/Transfer successful/i)).toBeInTheDocument()

    // Verify transaction table shows both transactions
    await waitFor(() => {
      expect(screen.getByText('DEPOSIT')).toBeInTheDocument()
      expect(screen.getByText('TRANSFER')).toBeInTheDocument()
    })
  })
})
