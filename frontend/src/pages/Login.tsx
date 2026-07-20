import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'
import { useToast } from '@/components/ui'
import { Button, Input } from '@/components/ui'

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export default function Login() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const { setTokens } = useAuthStore()
  
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  })

  const onSubmit = async (data: LoginFormValues) => {
    try {
      const response = await authApi.login(data)
      const { accessToken, refreshToken, user } = response.data.data
      setTokens(accessToken, refreshToken)
      useAuthStore.getState().setUser(user as any)
      toast({ title: 'Welcome back!', type: 'success' })
      navigate('/')
    } catch (error: any) {
      toast({
        title: 'Login failed',
        message: error.response?.data?.error?.message || 'Invalid credentials',
        type: 'error',
      })
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-neutral-25">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-neutral-900 tracking-tight">Argent</h1>
          <p className="text-neutral-500 mt-2">Financial Infrastructure</p>
        </div>

        <div className="card">
          <h2 className="text-xl font-semibold text-neutral-900 mb-6">Sign in</h2>
          
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Email"
              type="email"
              placeholder="you@example.com"
              error={errors.email?.message}
              {...register('email')}
            />
            
            <Input
              label="Password"
              type="password"
              placeholder="••••••••"
              error={errors.password?.message}
              {...register('password')}
            />

            <Button type="submit" className="w-full" isLoading={isSubmitting}>
              Sign in
            </Button>
          </form>

          <div className="mt-6 text-center text-sm text-neutral-500">
            Don't have an account?{' '}
            <a href="/register" className="text-brand-500 hover:text-brand-600 font-medium">
              Sign up
            </a>
          </div>
        </div>
      </div>
    </div>
  )
}
