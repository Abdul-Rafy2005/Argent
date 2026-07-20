import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'
import { useToast } from '@/components/ui'
import { Button, Input } from '@/components/ui'

const registerSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters'),
  email: z.string().email('Invalid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  organizationName: z.string().min(2, 'Organization name is required'),
})

type RegisterFormValues = z.infer<typeof registerSchema>

export default function Register() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const { setTokens } = useAuthStore()
  
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
  })

  const onSubmit = async (data: RegisterFormValues) => {
    try {
      const response = await authApi.register(data)
      const { accessToken, refreshToken, user } = response.data.data
      setTokens(accessToken, refreshToken)
      useAuthStore.getState().setUser(user as any)
      toast({ title: 'Account created successfully!', type: 'success' })
      navigate('/')
    } catch (error: any) {
      toast({
        title: 'Registration failed',
        message: error.response?.data?.error?.message || 'Something went wrong',
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
          <h2 className="text-xl font-semibold text-neutral-900 mb-6">Create your account</h2>
          
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Full Name"
              placeholder="Jane Doe"
              error={errors.name?.message}
              {...register('name')}
            />
            
            <Input
              label="Email"
              type="email"
              placeholder="you@example.com"
              error={errors.email?.message}
              {...register('email')}
            />
            
            <Input
              label="Organization Name"
              placeholder="Acme Inc."
              error={errors.organizationName?.message}
              {...register('organizationName')}
            />

            <Input
              label="Password"
              type="password"
              placeholder="••••••••"
              error={errors.password?.message}
              {...register('password')}
            />

            <Button type="submit" className="w-full" isLoading={isSubmitting}>
              Create Account
            </Button>
          </form>

          <div className="mt-6 text-center text-sm text-neutral-500">
            Already have an account?{' '}
            <a href="/login" className="text-brand-500 hover:text-brand-600 font-medium">
              Sign in
            </a>
          </div>
        </div>
      </div>
    </div>
  )
}
