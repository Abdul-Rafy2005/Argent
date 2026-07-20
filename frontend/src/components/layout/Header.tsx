import { useEffect } from 'react'
import { useAuthStore } from '@/store/authStore'
import { organizationApi } from '@/api/settings'

export default function Header() {
  const { user, organization, setOrganization, logout } = useAuthStore()

  useEffect(() => {
    if (!organization && user?.organizationId) {
      organizationApi.getById(user.organizationId)
        .then((res) => {
          setOrganization(res.data.data)
        })
        .catch(console.error)
    }
  }, [organization, setOrganization, user?.organizationId])

  return (
    <header className="bg-white border-b border-neutral-100 h-16 px-8 flex items-center justify-between sticky top-0 z-10">
      <div>
        {organization ? (
          <div className="flex items-center gap-2">
            <span className="w-8 h-8 rounded bg-brand-50 text-brand-700 flex items-center justify-center font-bold text-sm">
              {organization.name.charAt(0).toUpperCase()}
            </span>
            <span className="font-medium text-neutral-900">{organization.name}</span>
          </div>
        ) : (
          <div className="h-8 w-32 bg-neutral-100 animate-pulse rounded" />
        )}
      </div>

      <div className="flex items-center gap-4">
        {user ? (
          <div className="text-sm text-neutral-600">
            {user.email} <span className="text-neutral-300 mx-2">|</span> 
            <span className="font-medium">{user.role}</span>
          </div>
        ) : (
           <div className="h-5 w-32 bg-neutral-100 animate-pulse rounded" />
        )}
        <button 
          onClick={logout}
          className="text-sm text-neutral-500 hover:text-neutral-900 transition-colors"
        >
          Logout
        </button>
      </div>
    </header>
  )
}
