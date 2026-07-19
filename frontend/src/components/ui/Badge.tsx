import React from 'react'
import { twMerge } from 'tailwind-merge'

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  status: 'active' | 'pending' | 'failed' | 'frozen' | 'closed' | 'success' | 'warning' | 'error' | 'info' | 'neutral'
}

export function Badge({ status, className, children, ...props }: BadgeProps) {
  const styles = {
    active: 'bg-success-50 text-success-700',
    success: 'bg-success-50 text-success-700',
    pending: 'bg-warning-50 text-warning-700',
    warning: 'bg-warning-50 text-warning-700',
    failed: 'bg-error-50 text-error-700',
    error: 'bg-error-50 text-error-700',
    frozen: 'bg-info-50 text-info-700',
    info: 'bg-info-50 text-info-700',
    closed: 'bg-neutral-100 text-neutral-600',
    neutral: 'bg-neutral-100 text-neutral-600',
  }

  const dotColors = {
    active: 'bg-success-500',
    success: 'bg-success-500',
    pending: 'bg-warning-500',
    warning: 'bg-warning-500',
    failed: 'bg-error-500',
    error: 'bg-error-500',
    frozen: 'bg-info-500',
    info: 'bg-info-500',
    closed: 'bg-neutral-400',
    neutral: 'bg-neutral-400',
  }

  return (
    <span
      className={twMerge(
        'inline-flex items-center gap-1.5 px-2 py-1 rounded text-xs font-medium',
        styles[status],
        className
      )}
      {...props}
    >
      <span className={twMerge('w-1.5 h-1.5 rounded-full', dotColors[status])} />
      {children}
    </span>
  )
}
