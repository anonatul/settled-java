import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, unwrap } from '../../lib/api'
import { formatCurrency, formatDate } from '../../lib/utils'
import type { PageResponse, Policy, PolicyStatus } from '../../types'
import { Card } from '../../components/ui/Card'
import { StatusBadge } from '../../components/ui/Badge'
import { Select } from '../../components/ui/Input'
import { TableSkeleton } from '../../components/ui/Loading'
import { Pagination } from '../../components/ui/Pagination'

export function Policies() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<PolicyStatus | ''>('')

  const { data, isLoading } = useQuery({
    queryKey: ['admin-policies', page, status],
    queryFn: () => unwrap<PageResponse<Policy>>(
      api.get('/admin/policies', { params: { page, size: 10, status: status || undefined } }),
    ),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Policies</h1>
          <p className="mt-0.5 text-sm text-slate-500">Every policy issued on the platform</p>
        </div>
        <Select value={status} onChange={(e) => { setStatus(e.target.value as PolicyStatus | ''); setPage(0) }} className="max-w-[160px]">
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="EXPIRED">Expired</option>
          <option value="CANCELLED">Cancelled</option>
        </Select>
      </div>

      <Card>
        {isLoading || !data ? (
          <TableSkeleton />
        ) : data.totalElements === 0 ? (
          <div className="px-5 py-12 text-center text-sm text-slate-400">No policies found.</div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-[11px] uppercase tracking-wider text-slate-400">
                    <th className="px-5 py-3 font-semibold">Policy</th>
                    <th className="px-5 py-3 font-semibold">Type</th>
                    <th className="px-5 py-3 font-semibold">Customer</th>
                    <th className="px-5 py-3 font-semibold">Coverage</th>
                    <th className="px-5 py-3 font-semibold">Premium</th>
                    <th className="px-5 py-3 font-semibold">Valid until</th>
                    <th className="px-5 py-3 font-semibold">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {data.content.map((p) => (
                    <tr key={p.id} className="transition-colors hover:bg-slate-50">
                      <td className="px-5 py-3.5 font-mono text-xs font-semibold text-slate-800">{p.policyNumber}</td>
                      <td className="px-5 py-3.5 text-slate-600">{p.policyTypeName}</td>
                      <td className="px-5 py-3.5">
                        <p className="text-slate-700">{p.customerName}</p>
                        <p className="font-mono text-[10px] text-slate-400">{p.customerNumber}</p>
                      </td>
                      <td className="px-5 py-3.5 font-medium text-slate-700">{formatCurrency(p.sumInsured)}</td>
                      <td className="px-5 py-3.5 text-slate-600">{formatCurrency(p.premium)}</td>
                      <td className="px-5 py-3.5 text-slate-500">{formatDate(p.endDate)}</td>
                      <td className="px-5 py-3.5"><StatusBadge status={p.status} type="policy" /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
          </>
        )}
      </Card>
    </div>
  )
}