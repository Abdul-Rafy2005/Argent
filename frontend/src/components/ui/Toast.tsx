import { createContext, useContext, useState, useCallback, ReactNode } from 'react'
import { X, CheckCircle2, AlertCircle, Info } from 'lucide-react'

type ToastType = 'success' | 'error' | 'info'

interface ToastItem {
  id: string
  title: string
  message?: string
  type: ToastType
}

interface ToastContextType {
  toast: (options: Omit<ToastItem, 'id'>) => void
}

const ToastContext = createContext<ToastContextType | undefined>(undefined)

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) throw new Error('useToast must be used within ToastProvider')
  return context
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])

  const toast = useCallback(({ title, message, type }: Omit<ToastItem, 'id'>) => {
    const id = Math.random().toString(36).substr(2, 9)
    setToasts((prev) => [...prev, { id, title, message, type }])

    if (type !== 'error') {
      setTimeout(() => {
        removeToast(id)
      }, 5000)
    }
  }, [])

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <div className="fixed top-4 right-4 z-50 space-y-4 max-w-[380px] w-full">
        {toasts.map((t) => (
          <div
            key={t.id}
            className="flex items-start p-4 bg-white rounded-lg shadow-lg border border-neutral-100 relative overflow-hidden"
          >
            <div 
              className={`absolute left-0 top-0 bottom-0 w-1 ${
                t.type === 'success' ? 'bg-success-500' :
                t.type === 'error' ? 'bg-error-500' : 'bg-info-500'
              }`}
            />
            <div className="flex-shrink-0 mr-3 ml-1">
              {t.type === 'success' && <CheckCircle2 className="w-5 h-5 text-success-500" />}
              {t.type === 'error' && <AlertCircle className="w-5 h-5 text-error-500" />}
              {t.type === 'info' && <Info className="w-5 h-5 text-info-500" />}
            </div>
            <div className="flex-1 mr-4">
              <h3 className="text-sm font-medium text-neutral-900">{t.title}</h3>
              {t.message && <p className="mt-1 text-sm text-neutral-500">{t.message}</p>}
            </div>
            <button
              onClick={() => removeToast(t.id)}
              className="flex-shrink-0 text-neutral-400 hover:text-neutral-600"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
