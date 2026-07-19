import { describe, it, expect, vi } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import { renderWithProviders } from '../utils'
import Transactions from '@/pages/Transactions'
import { transactionApi } from '@/api/transactions'

vi.mock('@/api/transactions', () => ({
  transactionApi: {
    list: vi.fn(),
  },
}))

describe('Transactions Table Component', () => {
  it('renders correct data in table', async () => {
    vi.mocked(transactionApi.list).mockResolvedValueOnce({
      data: {
        data: {
          content: [
            {
              id: 'txn-1',
              type: 'DEPOSIT',
              status: 'COMPLETED',
              amount: 100.00,
              createdAt: new Date('2024-01-01').toISOString(),
              destinationWalletId: 'dest-1'
            },
            {
              id: 'txn-2',
              type: 'WITHDRAWAL',
              status: 'PENDING',
              amount: 50.25,
              createdAt: new Date('2024-01-02').toISOString(),
              sourceWalletId: 'src-1'
            }
          ],
          totalPages: 1
        }
      }
    } as any)

    renderWithProviders(<Transactions />)

    await waitFor(() => {
      expect(screen.getByText('txn-1', { exact: false })).toBeInTheDocument()
      expect(screen.getByText('txn-2', { exact: false })).toBeInTheDocument()
    })

    const table = screen.getByRole('table')
    
    // Check amounts are formatted
    expect(within(table).getByText('$100.00')).toBeInTheDocument()
    expect(within(table).getByText('$50.25')).toBeInTheDocument()
    
    // Check statuses
    expect(within(table).getByText('COMPLETED')).toBeInTheDocument()
    expect(within(table).getByText('PENDING')).toBeInTheDocument()
  })
})
