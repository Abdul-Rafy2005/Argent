import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { transactionApi } from '@/api/transactions'
import { Card, Table, Badge, Button, Select } from '@/components/ui'
import { formatCurrency, formatDateTime, truncateId } from '@/utils/format'
import { Transaction } from '@/types'
import { ColumnDef } from '@tanstack/react-table'

export default function Transactions() {
  const [page, setPage] = useState(0)
  const [typeFilter, setTypeFilter] = useState<string>('')
  const [statusFilter, setStatusFilter] = useState<string>('')

  const { data, isLoading } = useQuery({
    queryKey: ['transactions', page, typeFilter, statusFilter],
    queryFn: () => transactionApi.list({ 
      page, 
      pageSize: 20,
      ...(typeFilter ? { type: typeFilter as Transaction['type'] } : {}),
      ...(statusFilter ? { status: statusFilter as Transaction['status'] } : {})
    }).then(res => res.data.data),
  })

  const columns: ColumnDef<Transaction>[] = [
    {
      header: 'ID',
      accessorKey: 'id',
      cell: ({ row }) => <span className="font-mono text-xs">{truncateId(row.original.id)}</span>,
    },
    {
      header: 'Date',
      accessorKey: 'createdAt',
      cell: ({ row }) => formatDateTime(row.original.createdAt),
    },
    {
      header: 'Type',
      accessorKey: 'type',
    },
    {
      header: 'Status',
      accessorKey: 'status',
      cell: ({ row }) => (
        <Badge status={row.original.status.toLowerCase() as any}>
          {row.original.status}
        </Badge>
      ),
    },
    {
      header: 'Source',
      accessorKey: 'sourceWalletId',
      cell: ({ row }) => row.original.sourceWalletId ? (
        <span className="font-mono text-xs">{truncateId(row.original.sourceWalletId)}</span>
      ) : '-',
    },
    {
      header: 'Destination',
      accessorKey: 'destinationWalletId',
      cell: ({ row }) => row.original.destinationWalletId ? (
        <span className="font-mono text-xs">{truncateId(row.original.destinationWalletId)}</span>
      ) : '-',
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
  ]

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold text-neutral-900">Transactions</h1>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="px-6 py-4 border-b border-neutral-100 flex items-center gap-4 bg-neutral-25">
          <div className="w-48">
            <Select
              value={typeFilter}
              onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}
              options={[
                { value: '', label: 'All Types' },
                { value: 'DEPOSIT', label: 'Deposit' },
                { value: 'WITHDRAWAL', label: 'Withdrawal' },
                { value: 'TRANSFER', label: 'Transfer' },
                { value: 'REFUND', label: 'Refund' },
              ]}
            />
          </div>
          <div className="w-48">
             <Select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
              options={[
                { value: '', label: 'All Statuses' },
                { value: 'PENDING', label: 'Pending' },
                { value: 'COMPLETED', label: 'Completed' },
                { value: 'FAILED', label: 'Failed' },
              ]}
            />
          </div>
        </div>
        
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
