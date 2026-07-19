import { describe, it, expect, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../utils'
import Login from '@/pages/Login'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
  },
}))

describe('Login Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('submits form and sets auth state on success', async () => {
    const user = userEvent.setup()
    
    // Mock successful login response
    vi.mocked(authApi.login).mockResolvedValueOnce({
      data: {
        success: true,
        data: {
          accessToken: 'fake-access',
          refreshToken: 'fake-refresh',
          userId: '1',
          organizationId: '1',
          role: 'OWNER',
        },
      },
    } as any)

    renderWithProviders(<Login />)

    const emailInput = screen.getByLabelText(/Email/i)
    const passwordInput = screen.getByLabelText(/Password/i)
    const submitButton = screen.getByRole('button', { name: /Sign in/i })

    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'password123')
    await user.click(submitButton)

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password123',
      })
    })

    // Assert that the tokens were set in the store (and therefore local storage)
    expect(useAuthStore.getState().isAuthenticated).toBe(true)
    expect(useAuthStore.getState().accessToken).toBe('fake-access')
  })

  it('displays validation errors on empty submission', async () => {
    const user = userEvent.setup()
    renderWithProviders(<Login />)

    const submitButton = screen.getByRole('button', { name: /Sign in/i })
    await user.click(submitButton)

    await waitFor(() => {
      expect(screen.getByText(/Invalid email address/i)).toBeInTheDocument()
      expect(screen.getByText(/Password is required/i)).toBeInTheDocument()
    })
    
    expect(authApi.login).not.toHaveBeenCalled()
  })

  it('displays network error from API', async () => {
    const user = userEvent.setup()
    
    // Mock failed login
    vi.mocked(authApi.login).mockRejectedValueOnce({
      response: { data: { error: { message: 'Invalid credentials' } } }
    })

    renderWithProviders(<Login />)

    const emailInput = screen.getByLabelText(/Email/i)
    const passwordInput = screen.getByLabelText(/Password/i)
    const submitButton = screen.getByRole('button', { name: /Sign in/i })

    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'wrongpassword')
    await user.click(submitButton)

    await waitFor(() => {
      expect(screen.getByText(/Invalid credentials/i)).toBeInTheDocument()
    })
  })
})
