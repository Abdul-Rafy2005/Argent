import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { auditApi } from '@/api/audit'
import { Card, Table, Button, Modal } from '@/components/ui'
import { formatDateTime, truncateId } from '@/utils/format'
import { AuditLog } from '@/types'
import { ColumnDef } from '@tanstack/react-table'

export default function AuditLogs() {
  const [page, setPage] = useState(0)
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['audit-logs', page],
    queryFn: () => auditApi.list({ page, pageSize: 20 }).then(res => res.data.data),
  })

  const columns: ColumnDef<AuditLog>[] = [
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
      header: 'Action',
      accessorKey: 'action',
      cell: ({ row }) => <span className="font-medium text-neutral-900">{row.original.action}</span>,
    },
    {
      header: 'Entity Type',
      accessorKey: 'entityType',
    },
    {
      header: 'Entity ID',
      accessorKey: 'entityId',
      cell: ({ row }) => <span className="font-mono text-xs">{truncateId(row.original.entityId)}</span>,
    },
    {
      header: 'Performed By',
      accessorKey: 'performedBy',
      cell: ({ row }) => <span className="text-sm text-neutral-500">{row.original.performedBy || 'System'}</span>,
    },
    {
      id: 'details',
      cell: ({ row }) => (
        <Button variant="ghost" size="sm" onClick={() => setSelectedLog(row.original)}>
          View
        </Button>
      ),
    },
  ]

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-neutral-900">Audit Logs</h1>
          <p className="text-sm text-neutral-500 mt-1">System activity and modifications.</p>
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

      <Modal 
        isOpen={!!selectedLog} 
        onClose={() => setSelectedLog(null)} 
        title="Audit Log Details"
        maxWidth="lg"
      >
        {selectedLog && (
          <div className="space-y-6">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-xs font-medium text-neutral-500 uppercase tracking-wider mb-1">Action</p>
                <p className="text-sm font-medium text-neutral-900">{selectedLog.action}</p>
              </div>
              <div>
                <p className="text-xs font-medium text-neutral-500 uppercase tracking-wider mb-1">Date</p>
                <p className="text-sm text-neutral-900">{formatDateTime(selectedLog.createdAt)}</p>
              </div>
              <div>
                <p className="text-xs font-medium text-neutral-500 uppercase tracking-wider mb-1">Entity Type</p>
                <p className="text-sm text-neutral-900">{selectedLog.entityType}</p>
              </div>
              <div>
                <p className="text-xs font-medium text-neutral-500 uppercase tracking-wider mb-1">Entity ID</p>
                <p className="text-sm font-mono text-neutral-900">{selectedLog.entityId}</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <p className="text-xs font-medium text-neutral-500 uppercase tracking-wider mb-2">Previous State</p>
                <pre className="bg-neutral-950 text-neutral-300 p-4 rounded-lg text-xs font-mono overflow-auto max-h-64">
                  {selectedLog.previousState ? JSON.stringify(selectedLog.previousState, null, 2) : 'null'}
                </pre>
              </div>
              <div>
                <p className="text-xs font-medium text-neutral-500 uppercase tracking-wider mb-2">New State</p>
                <pre className="bg-neutral-950 text-neutral-300 p-4 rounded-lg text-xs font-mono overflow-auto max-h-64">
                  {selectedLog.newState ? JSON.stringify(selectedLog.newState, null, 2) : 'null'}
                </pre>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
