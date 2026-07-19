import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { reportsApi } from '@/api/reports'
import { Card, Button, Input } from '@/components/ui'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { formatCurrency, formatDate } from '@/utils/format'

export default function Reports() {
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  const { data: volumeData, isLoading: isVolumeLoading } = useQuery({
    queryKey: ['daily-volume', startDate, endDate],
    queryFn: () => reportsApi.getDailyVolume(
      (startDate || endDate) ? { startDate, endDate } : undefined
    ).then(res => res.data.data),
  })

  const handleExport = () => {
    const url = reportsApi.exportStatementUrl(startDate, endDate)
    window.open(url, '_blank')
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-neutral-900">Reports</h1>
          <p className="text-sm text-neutral-500 mt-1">Analytics and data exports.</p>
        </div>
        <Button onClick={handleExport}>
          Export CSV Statement
        </Button>
      </div>

      <Card className="mb-6 bg-neutral-25">
        <div className="flex items-center gap-4">
          <div className="w-48">
            <Input
              type="date"
              label="Start Date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
          </div>
          <div className="w-48">
            <Input
              type="date"
              label="End Date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
            />
          </div>
          <div className="pt-6">
             <Button variant="ghost" onClick={() => { setStartDate(''); setEndDate(''); }}>
               Clear Filters
             </Button>
          </div>
        </div>
      </Card>

      <div className="grid grid-cols-1 gap-6">
        <Card>
          <h2 className="text-lg font-medium text-neutral-900 mb-6">Transaction Volume</h2>
          <div className="h-[400px] w-full">
            {isVolumeLoading ? (
               <div className="w-full h-full bg-neutral-50 animate-pulse rounded" />
            ) : volumeData && volumeData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={volumeData} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#F2F4F7" vertical={false} />
                  <XAxis 
                    dataKey="date" 
                    tickFormatter={(val) => formatDate(val)}
                    tick={{ fontSize: 12, fill: '#667085' }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis 
                    tickFormatter={(val) => `$${val}`}
                    tick={{ fontSize: 12, fill: '#667085' }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <Tooltip 
                    formatter={(value: number) => [formatCurrency(value), 'Volume']}
                    labelFormatter={(label) => formatDate(label)}
                    contentStyle={{ borderRadius: '8px', border: '1px solid #F2F4F7', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="totalAmount" 
                    stroke="#6366F1" 
                    strokeWidth={2}
                    dot={{ r: 4, fill: '#6366F1', strokeWidth: 0 }}
                    activeDot={{ r: 6, strokeWidth: 0 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div className="w-full h-full flex items-center justify-center text-neutral-500">
                No data available for the selected period.
              </div>
            )}
          </div>
        </Card>
      </div>
    </div>
  )
}
