import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api, unwrap } from '../../lib/api'
import { formatCurrency, formatDateTime } from '../../lib/utils'
import type { Claim, ClaimStatus, PageResponse } from '../../types'
import { Card } from '../../components/ui/Card'
import { StatusBadge } from '../../components/ui/Badge'
import { Select } from '../../components/ui/Input'
import { TableSkeleton } from '../../components/ui/Loading'
import { Pagination } from '../../components/ui/Pagination'

const STATUSES: (ClaimStatus | '')[] = [
  '', 'SUBMITTED', 'UNDER_REVIEW', 'ADDITIONAL_INFO_REQUIRED', 'APPROVED', 'REJECTED', 'SETTLED',
]

export function Claims() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<ClaimStatus | ''>('')

  const { data, isLoading } = useQuery({
    queryKey: ['admin-claims', page, status],
    queryFn: () => unwrap<PageResponse<Claim>>(
      api.get('/admin/claims', { params: { page, size: 10, status: status || undefined } }),
    ),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-900">All Claims</h1>
          <p className="mt-0.5 text-sm text-slate-500">Platform-wide claim registry</p>
        </div>
        <Select value={status} onChange={(e) => { setStatus(e.target.value as ClaimStatus | ''); setPage(0) }} className="max-w-[200px]">
          <option value="">All statuses</option>
          {STATUSES.filter(Boolean).map((s) => (
            <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
          ))}
        </Select>
      </div>

      <Card>
        {isLoading || !data ? (
          <TableSkeleton />
        ) : data.totalElements === 0 ? (
          <div className="px-5 py-12 text-center text-sm text-slate-400">No claims found.</div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-[11px] uppercase tracking-wider text-slate-400">
                    <th className="px-5 py-3 font-semibold">Claim</th>
                    <th className="px-5 py-3 font-semibold">Customer</th>
                    <th className="px-5 py-3 font-semibold">Policy</th>
                    <th className="px-5 py-3 font-semibold">Incident</th>
                    <th className="px-5 py-3 font-semibold">Requested</th>
                    <th className="px-5 py-3 font-semibold">Officer</th>
                    <th className="px-5 py-3 font-semibold">Status</th>
                    <th className="px-5 py-3 font-semibold">Submitted</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {data.content.map((claim) => (
                    <tr key={claim.id} className="transition-colors hover:bg-slate-50">
                      <td className="px-5 py-3.5">
                        <Link to={`/admin/claims/${claim.id}`} className="font-semibold text-brand-600 hover:text-brand-700">
                          {claim.claimNumber}
                        </Link>
                      </td>
                      <td className="px-5 py-3.5 text-slate-600">{claim.customerName}</td>
                      <td className="px-5 py-3.5 font-mono text-[11px] text-slate-500">{claim.policyNumber}</td>
                      <td className="px-5 py-3.5 text-slate-600">{claim.incidentType}</td>
                      <td className="px-5 py-3.5 font-medium text-slate-700">{formatCurrency(claim.amountRequested)}</td>
                      <td className="px-5 py-3.5 text-slate-600">{claim.assignedOfficerName ?? <span className="text-slate-300">—</span>}</td>
                      <td className="px-5 py-3.5"><StatusBadge status={claim.status} /></td>
                      <td className="px-5 py-3.5 text-slate-500">{formatDateTime(claim.submittedAt)}</td>
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