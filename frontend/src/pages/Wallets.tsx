import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { walletApi, CreateWalletRequest } from '@/api/wallets'
import { Card, Table, Badge, Button, Modal, Input, Select, useToast } from '@/components/ui'
import { formatDateTime, truncateId } from '@/utils/format'
import { Wallet } from '@/types'
import { ColumnDef } from '@tanstack/react-table'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'

const createWalletSchema = z.object({
  label: z.string().min(2, 'Label must be at least 2 characters'),
  type: z.enum(['CUSTOMER', 'MERCHANT', 'ESCROW', 'REWARD', 'CREDIT', 'PLATFORM']),
})

type CreateWalletFormValues = z.infer<typeof createWalletSchema>

export default function Wallets() {
  const [page, setPage] = useState(0)
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const { toast } = useToast()
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['wallets', page],
    queryFn: () => walletApi.list(page, 20).then(res => res.data.data),
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateWalletFormValues>({
    resolver: zodResolver(createWalletSchema),
    defaultValues: { type: 'CUSTOMER' },
  })

  const createMutation = useMutation({
    mutationFn: (data: CreateWalletRequest) => walletApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallets'] })
      toast({ title: 'Wallet created', type: 'success' })
      setIsCreateModalOpen(false)
      reset()
    },
    onError: (error: any) => {
      toast({
        title: 'Failed to create wallet',
        message: error.response?.data?.error?.message || 'Something went wrong',
        type: 'error',
      })
    },
  })

  const onSubmit = (data: CreateWalletFormValues) => {
    createMutation.mutate(data)
  }

  const columns: ColumnDef<Wallet>[] = [
    {
      header: 'ID',
      accessorKey: 'id',
      cell: ({ row }) => (
        <Link to={`/wallets/${row.original.id}`} className="font-mono text-brand-500 hover:underline">
          {truncateId(row.original.id)}
        </Link>
      ),
    },
    {
      header: 'Label',
      accessorKey: 'label',
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
      header: 'Created At',
      accessorKey: 'createdAt',
      cell: ({ row }) => formatDateTime(row.original.createdAt),
    },
  ]

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold text-neutral-900">Wallets</h1>
        <Button className="flex items-center gap-2" onClick={() => setIsCreateModalOpen(true)}>
          <Plus className="w-4 h-4" />
          Create Wallet
        </Button>
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

      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create Wallet"
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input
            label="Label"
            placeholder="e.g. User Main Wallet"
            error={errors.label?.message}
            {...register('label')}
          />
          
          <Select
            label="Type"
            error={errors.type?.message}
            {...register('type')}
            options={[
              { value: 'CUSTOMER', label: 'Customer' },
              { value: 'MERCHANT', label: 'Merchant' },
              { value: 'ESCROW', label: 'Escrow' },
              { value: 'REWARD', label: 'Reward' },
              { value: 'CREDIT', label: 'Credit' },
              { value: 'PLATFORM', label: 'Platform' },
            ]}
          />

          <div className="flex justify-end gap-3 mt-6">
            <Button variant="ghost" type="button" onClick={() => setIsCreateModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Create Wallet
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
