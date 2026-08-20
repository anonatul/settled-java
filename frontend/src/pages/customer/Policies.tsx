import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api, unwrap } from '../../lib/api'
import { formatCurrency, formatDate } from '../../lib/utils'
import type { PageResponse, Policy } from '../../types'
import { Card } from '../../components/ui/Card'
import { StatusBadge } from '../../components/ui/Badge'
import { TableSkeleton } from '../../components/ui/Loading'
import { Pagination } from '../../components/ui/Pagination'

export function Policies() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useQuery({
    queryKey: ['policies', page],
    queryFn: () => unwrap<PageResponse<Policy>>(api.get('/policies', { params: { page, size: 10 } })),
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">My Policies</h1>
        <p className="mt-0.5 text-sm text-slate-500">All insurance policies in your name</p>
      </div>

      <Card>
        {isLoading || !data ? (
          <TableSkeleton />
        ) : data.totalElements === 0 ? (
          <div className="px-5 py-12 text-center">
            <p className="text-sm font-medium text-slate-500">No policies yet</p>
            <p className="mt-1 text-xs text-slate-400">Policies are issued by your insurance provider.</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-[11px] uppercase tracking-wider text-slate-400">
                    <th className="px-5 py-3 font-semibold">Policy</th>
                    <th className="px-5 py-3 font-semibold">Type</th>
                    <th className="px-5 py-3 font-semibold">Status</th>
                    <th className="px-5 py-3 font-semibold">Coverage</th>
                    <th className="px-5 py-3 font-semibold">Premium</th>
                    <th className="px-5 py-3 font-semibold">Valid until</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {data.content.map((policy) => (
                    <tr key={policy.id} className="transition-colors hover:bg-slate-50">
                      <td className="px-5 py-3.5">
                        <Link to={`/policies/${policy.id}`} className="font-semibold text-brand-600 hover:text-brand-700">
                          {policy.policyNumber}
                        </Link>
                      </td>
                      <td className="px-5 py-3.5 text-slate-600">{policy.policyTypeName}</td>
                      <td className="px-5 py-3.5">
                        <StatusBadge status={policy.status} type="policy" />
                      </td>
                      <td className="px-5 py-3.5 font-medium text-slate-700">{formatCurrency(policy.sumInsured)}</td>
                      <td className="px-5 py-3.5 text-slate-600">{formatCurrency(policy.premium)}/yr</td>
                      <td className="px-5 py-3.5 text-slate-600">{formatDate(policy.endDate)}</td>
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