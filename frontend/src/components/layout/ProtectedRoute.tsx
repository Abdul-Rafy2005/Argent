import { useEffect } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { authApi } from '@/api/auth'

export default function ProtectedRoute() {
  const { isAuthenticated, user, setUser } = useAuthStore()
  const location = useLocation()

  useEffect(() => {
    if (isAuthenticated && !user) {
      authApi.getMe().then((res) => {
        setUser(res.data.data as any)
      }).catch(console.error)
    }
  }, [isAuthenticated, user, setUser])

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <Outlet />
}
