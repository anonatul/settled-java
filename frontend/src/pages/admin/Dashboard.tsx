import { useQuery } from '@tanstack/react-query'
import { api, unwrap } from '../../lib/api'
import { formatCurrency, formatNumber } from '../../lib/utils'
import type { Analytics } from '../../types'
import { Card, CardHeader } from '../../components/ui/Card'
import { StatCard } from '../../components/ui/StatCard'
import { Spinner } from '../../components/ui/Loading'
import { ClaimsByStatusBar, MonthlyBarChart } from '../../components/Charts'

export function AdminDashboard() {
  const { data, isLoading } = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: () => unwrap<Analytics>(api.get('/admin/dashboard')),
  })

  if (isLoading || !data) return <Spinner />

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Platform Overview</h1>
        <p className="mt-0.5 text-sm text-slate-500">Admin dashboard — claims, users and settlements</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Total customers" value={formatNumber(data.totalCustomers)} tone="brand"
          icon={<svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" /></svg>} />
        <StatCard label="Total claims" value={formatNumber(data.totalClaims)} tone="slate"
          icon={<svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>} />
        <StatCard label="Settled amount" value={formatCurrency(data.totalSettledAmount)} tone="emerald"
          icon={<svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>} />
        <StatCard label="Total policies" value={formatNumber(data.totalPolicies)} tone="violet"
          icon={<svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" /></svg>} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card>
          <CardHeader title="Claims by status" />
          <div className="px-5 py-4">
            <ClaimsByStatusBar data={data.claimsByStatus} />
          </div>
        </Card>
        <Card>
          <CardHeader title="Claims per month" subtitle={data.monthlyClaims.length ? 'Last 12 months' : 'No data yet'} />
          <div className="px-2 py-2">
            {data.monthlyClaims.length ? (
              <MonthlyBarChart data={data.monthlyClaims} />
            ) : (
              <div className="flex h-[220px] items-center justify-center text-xs text-slate-400">No claim data</div>
            )}
          </div>
        </Card>
        <Card>
          <CardHeader title="Snapshot" />
          <dl className="space-y-2.5 px-5 py-4">
            {[
              ['Total users', data.totalUsers],
              ['Claim officers', data.totalOfficers],
              ['Pending claims', data.pendingClaims],
              ['Approved claims', data.approvedClaims],
              ['Rejected claims', data.rejectedClaims],
              ['Settled claims', data.settledClaims],
            ].map(([label, value]) => (
              <div key={label} className="flex items-center justify-between border-b border-slate-50 pb-2 last:border-0 last:pb-0">
                <dt className="text-xs text-slate-500">{label}</dt>
                <dd className="text-sm font-semibold text-slate-800">{formatNumber(value as number)}</dd>
              </div>
            ))}
          </dl>
        </Card>
      </div>
    </div>
  )
}