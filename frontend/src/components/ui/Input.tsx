import React from 'react'
import { twMerge } from 'tailwind-merge'

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helperText?: string
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, helperText, ...props }, ref) => {
    return (
      <div className="w-full">
        {label && (
          <label htmlFor={props.id || props.name} className="block text-sm font-medium text-neutral-700 mb-1">
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={props.id || props.name}
          className={twMerge(
            'w-full px-3 py-2 border border-neutral-200 rounded-lg text-sm',
            'placeholder:text-neutral-400',
            'focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-200',
            'disabled:bg-neutral-50 disabled:border-neutral-100 disabled:cursor-not-allowed',
            error && 'border-error-500 focus:border-error-500 focus:ring-error-500',
            className
          )}
          {...props}
        />
        {error && <p className="mt-1 text-xs text-error-700">{error}</p>}
        {helperText && !error && (
          <p className="mt-1 text-xs text-neutral-500">{helperText}</p>
        )}
      </div>
    )
  }
)
Input.displayName = 'Input'
