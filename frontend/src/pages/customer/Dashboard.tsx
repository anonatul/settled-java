import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api, unwrap } from '../../lib/api'
import { formatCurrency, formatDate, titleCase } from '../../lib/utils'
import type { CustomerDashboard } from '../../types'
import { Card, CardHeader } from '../../components/ui/Card'
import { StatCard } from '../../components/ui/StatCard'
import { Spinner } from '../../components/ui/Loading'
import { StatusBadge } from '../../components/ui/Badge'
import { ClaimDonut } from '../../components/Charts'
import { Button } from '../../components/ui/Button'

export function CustomerDashboard() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => unwrap<CustomerDashboard>(api.get('/dashboard')),
  })

  if (isLoading || !data) return <Spinner />

  return (
    <div className="space-y-6">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Dashboard</h1>
          <p className="mt-0.5 text-sm text-slate-500">Your insurance overview</p>
        </div>
        <Link to="/claims/new">
          <Button>Submit new claim</Button>
        </Link>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Active policies" value={data.policies} tone="brand"
          icon={<svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>} />
        <StatCard label="Total claims" value={data.totalClaims} tone="slate"
          icon={<svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" /></svg>} />
        <StatCard label="Pending" value={data.pendingClaims} tone="amber"
          icon={<svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>} />
        <StatCard label="Approved & settled" value={data.approvedClaims} tone="emerald"
          icon={<svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader title="Recent claims" subtitle="Your latest submissions" />
          {data.recentClaims.length === 0 ? (
            <div className="px-5 py-10 text-center text-sm text-slate-400">
              No claims yet — submit your first claim to get started.
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
                      {titleCase(claim.incidentType)} · {formatDate(claim.submittedAt)}
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
        </Card>
        <Card>
          <CardHeader title="Claims by status" />
          <div className="px-5 py-4">
            <ClaimDonut data={data.claimsByStatus} />
          </div>
        </Card>
      </div>
    </div>
  )
}