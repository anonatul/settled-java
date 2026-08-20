import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { api, unwrap } from '../../lib/api'
import { formatCurrency, formatDate } from '../../lib/utils'
import type { Policy } from '../../types'
import { Card } from '../../components/ui/Card'
import { StatusBadge } from '../../components/ui/Badge'
import { Spinner } from '../../components/ui/Loading'
import { Button } from '../../components/ui/Button'
import { Link } from 'react-router-dom'

export function PolicyDetail() {
  const { id } = useParams<{ id: string }>()
  const { data: policy, isLoading } = useQuery({
    queryKey: ['policy', id],
    queryFn: () => unwrap<Policy>(api.get(`/policies/${id}`)),
    enabled: !!id,
  })

  if (isLoading || !policy) return <Spinner />

  const rows: [string, React.ReactNode][] = [
    ['Policy number', <span className="font-semibold text-slate-800">{policy.policyNumber}</span>],
    ['Policy type', policy.policyTypeName],
    ['Status', <StatusBadge status={policy.status} type="policy" />],
    ['Start date', formatDate(policy.startDate)],
    ['End date', formatDate(policy.endDate)],
    ['Sum insured', <span className="font-semibold text-slate-800">{formatCurrency(policy.sumInsured)}</span>],
    ['Annual premium', formatCurrency(policy.premium)],
    ['Customer', policy.customerName],
    ['Customer number', policy.customerNumber],
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-900">{policy.policyNumber}</h1>
          <p className="mt-0.5 text-sm text-slate-500">{policy.policyTypeName}</p>
        </div>
        <Link to="/claims/new">
          <Button>Claim against this policy</Button>
        </Link>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <h3 className="mb-4 text-sm font-semibold text-slate-900">Policy details</h3>
          <dl className="divide-y divide-slate-100">
            {rows.map(([label, value]) => (
              <div key={label} className="flex items-center justify-between py-2.5">
                <dt className="text-xs text-slate-500">{label}</dt>
                <dd className="text-sm text-slate-700">{value}</dd>
              </div>
            ))}
          </dl>
        </Card>
        <Card className="overflow-hidden">
          <div className="bg-gradient-to-br from-brand-600 to-brand-800 p-6 text-white">
            <p className="text-xs font-medium uppercase tracking-wider text-brand-200">Sum insured</p>
            <p className="mt-2 text-3xl font-bold">{formatCurrency(policy.sumInsured)}</p>
            <div className="mt-6 space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-brand-200">Premium</span>
                <span className="font-semibold">{formatCurrency(policy.premium)}/year</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-brand-200">Coverage period</span>
                <span className="font-semibold">
                  {formatDate(policy.startDate)} — {formatDate(policy.endDate)}
                </span>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </div>
  )
}