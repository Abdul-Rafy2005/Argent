import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '@/App'
import { authApi } from '@/api/auth'
import { walletApi } from '@/api/wallets'
import { transactionApi } from '@/api/transactions'
import { useAuthStore } from '@/store/authStore'

vi.mock('@/api/auth', () => ({ authApi: { login: vi.fn() } }))
vi.mock('@/api/wallets', () => ({ walletApi: { list: vi.fn(), create: vi.fn(), get: vi.fn() } }))
vi.mock('@/api/settings', () => ({
  organizationApi: { get: vi.fn() },
  balanceApi: { get: vi.fn() },
}))
vi.mock('@/api/reports', () => ({ reportsApi: { getDailyVolume: vi.fn() } }))
vi.mock('@/api/transactions', () => ({
  transactionApi: {
    list: vi.fn(),
    listByWallet: vi.fn(),
    deposit: vi.fn(),
    transfer: vi.fn(),
  },
}))

import { MemoryRouter } from 'react-router-dom'

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual as any,
    BrowserRouter: ({ children }: any) => <MemoryRouter>{children}</MemoryRouter>,
  }
})

describe('Full Integration Flow', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    useAuthStore.getState().logout()
  })

  it('login → dashboard → create wallet', async () => {
    const user = userEvent.setup()

    // ── Mock login ──
    vi.mocked(authApi.login).mockResolvedValueOnce({
      data: { data: { accessToken: 'a', refreshToken: 'r', role: 'OWNER' } },
    } as any)

    // ── Mock dashboard APIs ──
    const { organizationApi } = await import('@/api/settings')
    const { reportsApi } = await import('@/api/reports')

    vi.mocked(organizationApi.get).mockResolvedValue({
      data: { data: { id: 'org-1', name: 'Test Org' } },
    } as any)

    vi.mocked(reportsApi.getDailyVolume).mockResolvedValue({
      data: { data: [] },
    } as any)

    vi.mocked(walletApi.list).mockResolvedValueOnce({
      data: { data: { content: [], totalPages: 0 } },
    } as any)

    vi.mocked(transactionApi.list).mockResolvedValueOnce({
      data: { data: { content: [], totalPages: 0 } },
    } as any)

    // ── Render ──
    const { QueryClient, QueryClientProvider } = await import('@tanstack/react-query')
    const { ToastProvider } = await import('@/components/ui')
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <App />
        </ToastProvider>
      </QueryClientProvider>
    )

    // ── Step 1: Login ──
    expect(screen.getByRole('heading', { name: /Sign in/i })).toBeInTheDocument()
    await user.type(screen.getByLabelText(/Email/i), 'test@test.com')
    await user.type(screen.getByLabelText(/Password/i), 'pass123')
    await user.click(screen.getByRole('button', { name: /Sign in/i }))

    // ── Step 2: Dashboard loads ──
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Dashboard/i })).toBeInTheDocument()
    })

    // ── Step 3: Navigate to Wallets ──
    vi.mocked(walletApi.list).mockResolvedValueOnce({
      data: { data: { content: [], totalPages: 0 } },
    } as any)
    await user.click(screen.getByRole('link', { name: /Wallets/i }))

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Wallets/i })).toBeInTheDocument()
    })

    // ── Step 4: Create a wallet ──
    await user.click(screen.getByRole('button', { name: /Create Wallet/i }))
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Create Wallet' })).toBeInTheDocument()
    )

    vi.mocked(walletApi.create).mockResolvedValueOnce({
      data: {
        data: {
          id: 'wallet-1',
          label: 'Source Wallet',
          type: 'CUSTOMER',
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
        },
      },
    } as any)

    // After creation the list refetches
    vi.mocked(walletApi.list).mockResolvedValue({
      data: {
        data: {
          content: [{ id: 'wallet-1', label: 'Source Wallet', type: 'CUSTOMER', status: 'ACTIVE', createdAt: new Date().toISOString() }],
          totalPages: 1,
        },
      },
    } as any)

    await user.type(screen.getByLabelText(/Label/i), 'Source Wallet')
    const createBtns = screen.getAllByRole('button', { name: /Create Wallet/i })
    await user.click(createBtns[createBtns.length - 1]!)

    await waitFor(() => {
      expect(walletApi.create).toHaveBeenCalledWith({
        label: 'Source Wallet',
        type: 'CUSTOMER',
      })
    })

    // Wallet shows up in list
    await waitFor(() => {
      expect(screen.getByText('Source Wallet')).toBeInTheDocument()
    })
  })
})
