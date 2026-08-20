import { titleCase } from '../../lib/utils'
import type { ClaimStatus, PolicyStatus, UserStatus } from '../../types'

const claimStyles: Record<ClaimStatus, string> = {
  SUBMITTED: 'bg-sky-50 text-sky-700 ring-sky-200',
  UNDER_REVIEW: 'bg-amber-50 text-amber-700 ring-amber-200',
  ADDITIONAL_INFO_REQUIRED: 'bg-violet-50 text-violet-700 ring-violet-200',
  APPROVED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  REJECTED: 'bg-red-50 text-red-700 ring-red-200',
  SETTLED: 'bg-slate-800 text-white ring-slate-800',
}

const policyStyles: Record<PolicyStatus, string> = {
  ACTIVE: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  EXPIRED: 'bg-slate-100 text-slate-600 ring-slate-200',
  CANCELLED: 'bg-red-50 text-red-700 ring-red-200',
}

const statusStyles: Record<UserStatus, string> = {
  ACTIVE: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  LOCKED: 'bg-red-50 text-red-700 ring-red-200',
}

export function StatusBadge({
  status,
  type = 'claim',
}: {
  status: string
  type?: 'claim' | 'policy' | 'user'
}) {
  const map = type === 'policy' ? policyStyles : type === 'user' ? statusStyles : claimStyles
  const style = map[status as keyof typeof map] ?? 'bg-slate-100 text-slate-600 ring-slate-200'
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[11px] font-semibold ring-1 ring-inset whitespace-nowrap ${style}`}
    >
      {titleCase(status)}
    </span>
  )
}

export function Badge({
  children,
  tone = 'slate',
}: {
  children: React.ReactNode
  tone?: 'slate' | 'blue' | 'green' | 'red' | 'amber' | 'violet'
}) {
  const tones: Record<string, string> = {
    slate: 'bg-slate-100 text-slate-700',
    blue: 'bg-brand-50 text-brand-700',
    green: 'bg-emerald-50 text-emerald-700',
    red: 'bg-red-50 text-red-700',
    amber: 'bg-amber-50 text-amber-700',
    violet: 'bg-violet-50 text-violet-700',
  }
  return (
    <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-semibold ${tones[tone]}`}>
      {children}
    </span>
  )
}