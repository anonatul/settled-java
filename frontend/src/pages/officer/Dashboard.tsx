import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api, unwrap } from '../../lib/api'
import { formatCurrency, formatDate } from '../../lib/utils'
import type { OfficerDashboard } from '../../types'
import { Card, CardHeader } from '../../components/ui/Card'
import { StatCard } from '../../components/ui/StatCard'
import { Spinner } from '../../components/ui/Loading'
import { StatusBadge } from '../../components/ui/Badge'
import { ClaimsByStatusBar } from '../../components/Charts'

export function OfficerDashboard() {
  const { data, isLoading } = useQuery({
    queryKey: ['officer-dashboard'],
    queryFn: () => unwrap<OfficerDashboard>(api.get('/officer/dashboard')),
  })

  if (isLoading || !data) return <Spinner />

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Claims Overview</h1>
        <p className="mt-0.5 text-sm text-slate-500">Work your queue and keep customers informed</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <StatCard label="Assigned to me" value={data.assignedClaims} tone="brand"
          icon={<svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" /></svg>} />
        <StatCard label="Pending review" value={data.pendingReview} tone="amber"
          icon={<svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>} />
        <StatCard label="Awaiting info" value={data.awaitingInfo} tone="violet"
          icon={<svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader title="Assigned to me" subtitle="Claims awaiting your action" />
          {data.recentClaims.length === 0 ? (
            <div className="px-5 py-10 text-center text-sm text-slate-400">
              Nothing assigned — all caught up.
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {data.recentClaims.map((claim) => (
                <Link
                  key={claim.id}
                  to={`/claims/${claim.id}`}
                  className="flex items-center justify-between gap-4 px-5 py-3.5 transition-colors hover:bg-slate-50"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-slate-800">{claim.claimNumber}</p>
                    <p className="mt-0.5 truncate text-xs text-slate-500">
                      {claim.incidentType} · {formatDate(claim.submittedAt)}
                    </p>
                  </div>
                  <div className="flex items-center gap-4">
                    <span className="text-sm font-semibold text-slate-700">{formatCurrency(claim.amountRequested)}</span>
                    <StatusBadge status={claim.status} />
                  </div>
                </Link>
              ))}
            </div>
          )}
          <div className="border-t border-slate-100 px-5 py-3">
            <Link to="/claims" className="text-xs font-semibold text-brand-600 hover:text-brand-700">
              View all assigned claims →
            </Link>
          </div>
        </Card>
        <Card>
          <CardHeader title="Claims by status" />
          <div className="px-5 py-4">
            <ClaimsByStatusBar data={data.claimsByStatus} />
          </div>
        </Card>
      </div>
    </div>
  )
}