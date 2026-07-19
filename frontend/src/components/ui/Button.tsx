import React from 'react'
import { twMerge } from 'tailwind-merge'

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'link'
  size?: 'sm' | 'md' | 'lg'
  isLoading?: boolean
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', isLoading, children, disabled, ...props }, ref) => {
    const baseStyles = 'inline-flex items-center justify-center rounded-lg font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed'
    
    const variants = {
      primary: 'bg-brand-500 text-white hover:bg-brand-600 active:bg-brand-700 focus:ring-brand-200',
      secondary: 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200 active:bg-neutral-300 focus:ring-neutral-200',
      ghost: 'bg-transparent text-neutral-600 hover:bg-neutral-50 active:bg-neutral-100 focus:ring-neutral-200',
      danger: 'bg-error-500 text-white hover:bg-error-700 active:bg-error-700 focus:ring-error-500',
      link: 'bg-transparent text-brand-500 hover:underline p-0 focus:ring-brand-200',
    }

    const sizes = {
      sm: 'h-8 px-3 text-[13px]',
      md: 'h-9 px-4 text-sm',
      lg: 'h-10 px-5 text-sm',
    }

    return (
      <button
        ref={ref}
        disabled={disabled || isLoading}
        className={twMerge(
          baseStyles,
          variant !== 'link' && sizes[size],
          variants[variant],
          className
        )}
        {...props}
      >
        {isLoading && (
          <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
        )}
        {children}
      </button>
    )
  }
)
Button.displayName = 'Button'
