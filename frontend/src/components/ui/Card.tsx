import React from 'react'
import { twMerge } from 'tailwind-merge'

export function Card({ className, children, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={twMerge('bg-white border border-neutral-100 rounded-lg p-6', className)}
      {...props}
    >
      {children}
    </div>
  )
}
