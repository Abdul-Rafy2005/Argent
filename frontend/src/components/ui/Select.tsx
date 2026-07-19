import React from 'react'
import { twMerge } from 'tailwind-merge'

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  helperText?: string
  options: { value: string; label: string }[]
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, label, error, helperText, options, ...props }, ref) => {
    return (
      <div className="w-full">
        {label && (
          <label htmlFor={props.id || props.name} className="block text-sm font-medium text-neutral-700 mb-1">
            {label}
          </label>
        )}
        <select
          ref={ref}
          id={props.id || props.name}
          className={twMerge(
            'w-full px-3 py-2 border border-neutral-200 rounded-lg text-sm bg-white',
            'focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-200',
            'disabled:bg-neutral-50 disabled:border-neutral-100 disabled:cursor-not-allowed',
            error && 'border-error-500 focus:border-error-500 focus:ring-error-500',
            className
          )}
          {...props}
        >
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        {error && <p className="mt-1 text-xs text-error-700">{error}</p>}
        {helperText && !error && (
          <p className="mt-1 text-xs text-neutral-500">{helperText}</p>
        )}
      </div>
    )
  }
)
Select.displayName = 'Select'
