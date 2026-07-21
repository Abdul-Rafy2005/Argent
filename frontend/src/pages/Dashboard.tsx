import { useQuery } from '@tanstack/react-query'
import { reportsApi } from '@/api/reports'
import { walletApi } from '@/api/wallets'
import { transactionApi } from '@/api/transactions'
import { Card, Table, Badge, Skeleton } from '@/components/ui'
import { formatCurrency, formatDateTime } from '@/utils/format'
import { Link } from 'react-router-dom'
import { Transaction } from '@/types'
import { ColumnDef } from '@tanstack/react-table'

export default function Dashboard() {
  const { data: volumeData, isLoading: isVolumeLoading } = useQuery({
    queryKey: ['dailyVolume'],
    queryFn: () => reportsApi.getDailyVolume().then(res => res.data.data),
  })

  const { data: walletsData, isLoading: isWalletsLoading } = useQuery({
    queryKey: ['wallets', 0, 100],
    queryFn: () => walletApi.list(0, 100).then(res => res.data.data),
  })

  const { data: transactionsData, isLoading: isTransactionsLoading } = useQuery({
    queryKey: ['transactions', 0, 5],
    queryFn: () => transactionApi.list({ page: 0, pageSize: 5 }).then(res => res.data.data),
  })

  const totalWallets = walletsData?.total || 0
  const activeWallets = walletsData?.content.filter(w => w.status === 'ACTIVE').length || 0
  
  // Quick calculation from daily volume for today (assuming last element is today)
  const todayVolume = volumeData && volumeData.length > 0 
    ? volumeData[volumeData.length - 1]?.transactionCount || 0
    : 0
    
  const totalBalance = walletsData?.content?.reduce((sum, w) => sum + (w.balance || 0), 0) ?? 0

  const columns: ColumnDef<Transaction>[] = [
    {
      header: 'ID',
      accessorKey: 'id',
      cell: ({ row }) => <span className="font-mono text-xs">{row.original.id.substring(0, 8)}...</span>,
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
      header: 'Amount',
      accessorKey: 'amount',
      cell: ({ row }) => (
        <div className="font-mono text-right w-full">
           {row.original.type === 'DEPOSIT' || row.original.type === 'REFUND' ? (
             <span className="text-success-700">+{formatCurrency(row.original.amount)}</span>
           ) : row.original.type === 'WITHDRAWAL' ? (
             <span className="text-error-700">-{formatCurrency(row.original.amount)}</span>
           ) : (
             formatCurrency(row.original.amount)
           )}
        </div>
      ),
    },
  ]

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold text-neutral-900">Dashboard</h1>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <Card>
          <p className="text-sm text-neutral-500 mb-1">Total Wallets</p>
          {isWalletsLoading ? <Skeleton className="h-8 w-16" /> : (
            <p className="text-2xl font-semibold text-neutral-900">{totalWallets}</p>
          )}
        </Card>
        <Card>
          <p className="text-sm text-neutral-500 mb-1">Active Wallets</p>
          {isWalletsLoading ? <Skeleton className="h-8 w-16" /> : (
            <p className="text-2xl font-semibold text-neutral-900">{activeWallets}</p>
          )}
        </Card>
        <Card>
          <p className="text-sm text-neutral-500 mb-1">Transactions Today</p>
          {isVolumeLoading ? <Skeleton className="h-8 w-16" /> : (
            <p className="text-2xl font-semibold text-neutral-900">{todayVolume}</p>
          )}
        </Card>
        <Card>
          <p className="text-sm text-neutral-500 mb-1">Total Balance</p>
          {isWalletsLoading ? <Skeleton className="h-8 w-16" /> : (
            <p className="text-2xl font-semibold text-neutral-900 font-mono">{formatCurrency(totalBalance)}</p>
          )}
        </Card>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="px-6 py-5 border-b border-neutral-100 flex items-center justify-between">
          <h2 className="text-lg font-medium text-neutral-900">Recent Transactions</h2>
          <Link to="/transactions" className="text-sm font-medium text-brand-500 hover:text-brand-600">
            View all
          </Link>
        </div>
        
        <Table 
          columns={columns} 
          data={transactionsData?.content || []} 
          isLoading={isTransactionsLoading}
        />
      </Card>
    </div>
  )
}
