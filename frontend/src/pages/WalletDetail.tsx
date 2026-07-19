import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { walletApi } from '@/api/wallets'
import { balanceApi } from '@/api/settings'
import { transactionApi } from '@/api/transactions'
import { Card, Badge, Button, Table, Modal, Input, useToast, Skeleton } from '@/components/ui'
import { formatCurrency, formatDateTime, truncateId } from '@/utils/format'
import { Transaction } from '@/types'
import { ColumnDef } from '@tanstack/react-table'

export default function WalletDetail() {
  const { id } = useParams<{ id: string }>()
  const { toast } = useToast()
  const queryClient = useQueryClient()
  
  const [isDepositModalOpen, setIsDepositModalOpen] = useState(false)
  const [depositAmount, setDepositAmount] = useState('')
  const [isTransferModalOpen, setIsTransferModalOpen] = useState(false)
  const [transferAmount, setTransferAmount] = useState('')
  const [transferDestination, setTransferDestination] = useState('')

  const { data: walletData, isLoading: isWalletLoading } = useQuery({
    queryKey: ['wallet', id],
    queryFn: () => walletApi.get(id!).then(res => res.data.data),
    enabled: !!id,
  })

  const { data: balanceData, isLoading: isBalanceLoading } = useQuery({
    queryKey: ['balance', id],
    queryFn: () => balanceApi.get(id!).then(res => res.data.data),
    enabled: !!id,
  })

  const { data: transactionsData, isLoading: isTransactionsLoading } = useQuery({
    queryKey: ['wallet-transactions', id],
    queryFn: () => transactionApi.listByWallet(id!, 0, 10).then(res => res.data.data),
    enabled: !!id,
  })

  const depositMutation = useMutation({
    mutationFn: () => transactionApi.deposit({ walletId: id!, amount: parseFloat(depositAmount) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['balance', id] })
      queryClient.invalidateQueries({ queryKey: ['wallet-transactions', id] })
      toast({ title: 'Deposit successful', type: 'success' })
      setIsDepositModalOpen(false)
      setDepositAmount('')
    },
    onError: (error: any) => {
      toast({
        title: 'Deposit failed',
        message: error.response?.data?.error?.message || 'Something went wrong',
        type: 'error',
      })
    }
  })

  const transferMutation = useMutation({
    mutationFn: () => transactionApi.transfer({
      sourceWalletId: id!,
      destinationWalletId: transferDestination,
      amount: parseFloat(transferAmount),
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['balance', id] })
      queryClient.invalidateQueries({ queryKey: ['wallet-transactions', id] })
      toast({ title: 'Transfer successful', type: 'success' })
      setIsTransferModalOpen(false)
      setTransferAmount('')
      setTransferDestination('')
    },
    onError: (error: any) => {
      toast({
        title: 'Transfer failed',
        message: error.response?.data?.error?.message || 'Something went wrong',
        type: 'error',
      })
    }
  })

  const handleDeposit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!depositAmount || isNaN(parseFloat(depositAmount)) || parseFloat(depositAmount) <= 0) {
      toast({ title: 'Invalid amount', type: 'error' })
      return
    }
    depositMutation.mutate()
  }

  const handleTransfer = (e: React.FormEvent) => {
    e.preventDefault()
    if (!transferAmount || isNaN(parseFloat(transferAmount)) || parseFloat(transferAmount) <= 0) {
      toast({ title: 'Invalid amount', type: 'error' })
      return
    }
    if (!transferDestination.trim()) {
      toast({ title: 'Destination wallet ID is required', type: 'error' })
      return
    }
    transferMutation.mutate()
  }

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
      header: 'Amount',
      accessorKey: 'amount',
      cell: ({ row }) => (
        <div className="font-mono text-right w-full">
           {row.original.destinationWalletId === id ? (
             <span className="text-success-700">+{formatCurrency(row.original.amount)}</span>
           ) : (
             <span className="text-error-700">-{formatCurrency(row.original.amount)}</span>
           )}
        </div>
      ),
    },
  ]

  if (isWalletLoading || !walletData) return <div className="p-8"><Skeleton className="h-10 w-48 mb-6" /><Skeleton className="h-64 w-full" /></div>

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-neutral-900 flex items-center gap-3">
            {walletData.label}
            <Badge status={walletData.status.toLowerCase() as any}>{walletData.status}</Badge>
          </h1>
          <p className="text-sm text-neutral-500 font-mono mt-1">ID: {walletData.id}</p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="secondary" onClick={() => setIsTransferModalOpen(true)} disabled={walletData.status !== 'ACTIVE'}>
            Transfer
          </Button>
          <Button onClick={() => setIsDepositModalOpen(true)} disabled={walletData.status !== 'ACTIVE'}>
            Deposit
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <Card className="md:col-span-1 bg-neutral-950 text-white">
          <p className="text-sm text-neutral-400 mb-1">Available Balance</p>
          {isBalanceLoading ? <Skeleton className="h-8 w-24 bg-neutral-800" /> : (
            <p className="text-3xl font-semibold font-mono">
              {formatCurrency(balanceData?.available || 0)}
            </p>
          )}
        </Card>
        
        <Card className="md:col-span-3 grid grid-cols-3 gap-4">
          <div>
            <p className="text-sm text-neutral-500 mb-1">Current Balance</p>
            <p className="text-xl font-semibold font-mono text-neutral-900">
              {formatCurrency(balanceData?.current || 0)}
            </p>
          </div>
          <div>
            <p className="text-sm text-neutral-500 mb-1">Pending</p>
            <p className="text-xl font-semibold font-mono text-neutral-900">
              {formatCurrency(balanceData?.pending || 0)}
            </p>
          </div>
          <div>
            <p className="text-sm text-neutral-500 mb-1">Reserved</p>
            <p className="text-xl font-semibold font-mono text-neutral-900">
              {formatCurrency(balanceData?.reserved || 0)}
            </p>
          </div>
        </Card>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="px-6 py-5 border-b border-neutral-100">
          <h2 className="text-lg font-medium text-neutral-900">Recent Transactions</h2>
        </div>
        
        <Table 
          columns={columns} 
          data={transactionsData?.content || []} 
          isLoading={isTransactionsLoading}
        />
      </Card>

      <Modal isOpen={isDepositModalOpen} onClose={() => setIsDepositModalOpen(false)} title="Deposit Funds">
        <form onSubmit={handleDeposit} className="space-y-4">
          <Input
            label="Amount (USD)"
            name="depositAmount"
            type="number"
            step="0.01"
            min="0.01"
            placeholder="0.00"
            value={depositAmount}
            onChange={(e) => setDepositAmount(e.target.value)}
            required
          />
          <div className="flex justify-end gap-3 mt-6">
            <Button variant="ghost" type="button" onClick={() => setIsDepositModalOpen(false)}>Cancel</Button>
            <Button type="submit" isLoading={depositMutation.isPending}>Deposit</Button>
          </div>
        </form>
      </Modal>

      <Modal isOpen={isTransferModalOpen} onClose={() => setIsTransferModalOpen(false)} title="Transfer Funds">
        <form onSubmit={handleTransfer} className="space-y-4">
          <Input
            label="Destination Wallet ID"
            name="destinationWalletId"
            placeholder="e.g. wal_abc123"
            value={transferDestination}
            onChange={(e) => setTransferDestination(e.target.value)}
            required
          />
          <Input
            label="Amount (USD)"
            name="transferAmount"
            type="number"
            step="0.01"
            min="0.01"
            placeholder="0.00"
            value={transferAmount}
            onChange={(e) => setTransferAmount(e.target.value)}
            required
          />
          <div className="flex justify-end gap-3 mt-6">
            <Button variant="ghost" type="button" onClick={() => setIsTransferModalOpen(false)}>Cancel</Button>
            <Button type="submit" isLoading={transferMutation.isPending}>Transfer</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
