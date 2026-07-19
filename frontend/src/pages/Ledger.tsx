import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { ledgerApi } from '@/api/ledger'
import { Card, Table, Button } from '@/components/ui'
import { formatCurrency, formatDateTime, truncateId } from '@/utils/format'
import { LedgerEntry } from '@/types'
import { ColumnDef } from '@tanstack/react-table'

export default function Ledger() {
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['ledger', page],
    queryFn: () => ledgerApi.list({ page, pageSize: 20 }).then(res => res.data.data),
  })

  const columns: ColumnDef<LedgerEntry>[] = [
    {
      header: 'ID',
      accessorKey: 'id',
      cell: ({ row }) => <span className="font-mono text-xs text-neutral-500">{truncateId(row.original.id)}</span>,
    },
    {
      header: 'Date',
      accessorKey: 'createdAt',
      cell: ({ row }) => formatDateTime(row.original.createdAt),
    },
    {
      header: 'Account',
      accessorKey: 'accountId',
      cell: ({ row }) => <span className="font-mono text-xs">{truncateId(row.original.accountId)}</span>,
    },
    {
      header: 'Transaction',
      accessorKey: 'transactionId',
      cell: ({ row }) => <span className="font-mono text-xs text-brand-500">{truncateId(row.original.transactionId)}</span>,
    },
    {
      header: 'Type',
      accessorKey: 'type',
      cell: ({ row }) => (
        <span className={`font-medium ${row.original.type === 'CREDIT' ? 'text-success-700' : 'text-error-700'}`}>
          {row.original.type}
        </span>
      )
    },
    {
      header: 'Amount',
      accessorKey: 'amount',
      cell: ({ row }) => (
        <div className="font-mono text-right w-full">
           {formatCurrency(row.original.amount)}
        </div>
      ),
    },
    {
      header: 'Balance After',
      accessorKey: 'balanceAfter',
      cell: ({ row }) => (
        <div className="font-mono text-right w-full text-neutral-500">
           {formatCurrency(row.original.balanceAfter)}
        </div>
      ),
    },
  ]

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-neutral-900">Ledger</h1>
          <p className="text-sm text-neutral-500 mt-1">Immutable double-entry ledger records.</p>
        </div>
      </div>

      <Card className="p-0 overflow-hidden">
        <Table 
          columns={columns} 
          data={data?.content || []} 
          isLoading={isLoading} 
        />
        
        {data && data.totalPages > 1 && (
          <div className="px-6 py-4 border-t border-neutral-100 flex items-center justify-between">
             <Button
               variant="secondary"
               disabled={page === 0}
               onClick={() => setPage(p => p - 1)}
             >
               Previous
             </Button>
             <span className="text-sm text-neutral-500">
               Page {page + 1} of {data.totalPages}
             </span>
             <Button
               variant="secondary"
               disabled={page >= data.totalPages - 1}
               onClick={() => setPage(p => p + 1)}
             >
               Next
             </Button>
          </div>
        )}
      </Card>
    </div>
  )
}
