import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiKeyApi } from '@/api/settings'
import { useAuthStore } from '@/store/authStore'
import { Card, Button, Input, Select, Badge, Table, Modal, useToast } from '@/components/ui'
import { formatDateTime } from '@/utils/format'
import { ApiKey } from '@/types'
import { ColumnDef } from '@tanstack/react-table'

export default function Settings() {
  const { organization } = useAuthStore()
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [keyName, setKeyName] = useState('')
  const [keyEnvironment, setKeyEnvironment] = useState<'SANDBOX' | 'PRODUCTION'>('SANDBOX')
  const [newKeyData, setNewKeyData] = useState<{ name: string, rawKey: string } | null>(null)
  const { toast } = useToast()
  const queryClient = useQueryClient()

  const { data: apiKeys, isLoading } = useQuery({
    queryKey: ['api-keys'],
    queryFn: () => apiKeyApi.list().then(res => res.data.data),
  })

  const createMutation = useMutation({
    mutationFn: () => apiKeyApi.create({ name: keyName, environment: keyEnvironment }),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] })
      setNewKeyData({ name: res.data.data.name, rawKey: res.data.data.rawKey })
      setIsCreateModalOpen(false)
      setKeyName('')
      toast({ title: 'API Key generated', type: 'success' })
    },
    onError: (error: any) => {
      toast({
        title: 'Failed to generate API Key',
        message: error.response?.data?.error?.message || 'Something went wrong',
        type: 'error',
      })
    }
  })

  const revokeMutation = useMutation({
    mutationFn: (id: string) => apiKeyApi.revoke(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] })
      toast({ title: 'API Key revoked', type: 'success' })
    }
  })

  const columns: ColumnDef<ApiKey>[] = [
    {
      header: 'Name',
      accessorKey: 'name',
      cell: ({ row }) => <span className="font-medium text-neutral-900">{row.original.name}</span>,
    },
    {
      header: 'Environment',
      accessorKey: 'environment',
      cell: ({ row }) => (
        <Badge status={row.original.environment === 'PRODUCTION' ? 'warning' : 'info'}>
          {row.original.environment}
        </Badge>
      ),
    },
    {
      header: 'Prefix',
      accessorKey: 'keyPrefix',
      cell: ({ row }) => <span className="font-mono text-xs text-neutral-500">{row.original.keyPrefix}••••••••</span>,
    },
    {
      header: 'Status',
      accessorKey: 'status',
      cell: ({ row }) => (
        <Badge status={row.original.status === 'ACTIVE' ? 'active' : 'closed'}>
          {row.original.status}
        </Badge>
      ),
    },
    {
      header: 'Created At',
      accessorKey: 'createdAt',
      cell: ({ row }) => formatDateTime(row.original.createdAt),
    },
    {
      id: 'actions',
      cell: ({ row }) => row.original.status === 'ACTIVE' && (
        <div className="text-right">
          <Button 
            variant="danger" 
            size="sm" 
            onClick={() => {
              if (window.confirm('Are you sure you want to revoke this key?')) {
                revokeMutation.mutate(row.original.id)
              }
            }}
            isLoading={revokeMutation.isPending}
          >
            Revoke
          </Button>
        </div>
      )
    },
  ]

  return (
    <div>
      <h1 className="text-2xl font-semibold text-neutral-900 mb-6">Settings</h1>

      <div className="space-y-6">
        <Card>
          <h2 className="text-lg font-medium text-neutral-900 mb-4">Organization Profile</h2>
          {organization && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Input label="Name" value={organization.name} disabled />
              </div>
              <div>
                <Input label="Organization ID" value={organization.id} disabled />
              </div>
              <div>
                <Input label="Status" value={organization.status} disabled />
              </div>
              <div>
                <Input label="Created At" value={formatDateTime(organization.createdAt)} disabled />
              </div>
            </div>
          )}
        </Card>

        <Card className="p-0 overflow-hidden">
          <div className="px-6 py-5 border-b border-neutral-100 flex items-center justify-between">
            <div>
              <h2 className="text-lg font-medium text-neutral-900">API Keys</h2>
              <p className="text-sm text-neutral-500 mt-1">Manage keys used to authenticate API requests.</p>
            </div>
            <Button onClick={() => setIsCreateModalOpen(true)}>Generate Key</Button>
          </div>
          <Table 
            columns={columns} 
            data={apiKeys || []} 
            isLoading={isLoading} 
          />
        </Card>
      </div>

      <Modal isOpen={isCreateModalOpen} onClose={() => setIsCreateModalOpen(false)} title="Generate API Key">
        <form onSubmit={(e) => { e.preventDefault(); createMutation.mutate(); }} className="space-y-4">
          <Input
            label="Key Name"
            placeholder="e.g. Production Main Key"
            value={keyName}
            onChange={(e) => setKeyName(e.target.value)}
            required
          />
          <Select
            label="Environment"
            value={keyEnvironment}
            onChange={(e) => setKeyEnvironment(e.target.value as any)}
            options={[
              { value: 'SANDBOX', label: 'Sandbox' },
              { value: 'PRODUCTION', label: 'Production' },
            ]}
          />
          <div className="flex justify-end gap-3 mt-6">
            <Button variant="ghost" type="button" onClick={() => setIsCreateModalOpen(false)}>Cancel</Button>
            <Button type="submit" isLoading={createMutation.isPending}>Generate</Button>
          </div>
        </form>
      </Modal>

      <Modal isOpen={!!newKeyData} onClose={() => setNewKeyData(null)} title="Save your API Key">
        {newKeyData && (
          <div className="space-y-4">
            <div className="bg-warning-50 text-warning-700 p-4 rounded-lg text-sm mb-4">
              Please copy this API key and save it somewhere safe. For security reasons, <strong>we cannot show it to you again</strong>.
            </div>
            
            <Input
              label={newKeyData.name}
              value={newKeyData.rawKey}
              readOnly
              onClick={(e) => (e.target as HTMLInputElement).select()}
            />

            <div className="flex justify-end mt-6">
              <Button onClick={() => {
                navigator.clipboard.writeText(newKeyData.rawKey)
                toast({ title: 'Copied to clipboard', type: 'info' })
              }}>
                Copy to Clipboard
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
