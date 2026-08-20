import { StatusBadge } from './ui/Badge'
import { formatDateTime } from '../lib/utils'
import type { StatusHistory } from '../types'

const dotStyles: Record<string, string> = {
  SUBMITTED: 'bg-sky-500 ring-sky-100',
  UNDER_REVIEW: 'bg-amber-500 ring-amber-100',
  ADDITIONAL_INFO_REQUIRED: 'bg-violet-500 ring-violet-100',
  APPROVED: 'bg-emerald-500 ring-emerald-100',
  REJECTED: 'bg-red-500 ring-red-100',
  SETTLED: 'bg-slate-700 ring-slate-100',
}

export function ClaimTimeline({ history }: { history: StatusHistory[] }) {
  if (history.length === 0) {
    return <p className="py-6 text-center text-xs text-slate-400">No status history recorded</p>
  }
  return (
    <ol className="relative space-y-5 pl-1">
      {history.map((h, i) => (
        <li key={h.id} className="relative flex gap-3.5">
          {i < history.length - 1 && (
            <span className="absolute left-[7px] top-5 h-full w-px bg-slate-200" />
          )}
          <span
            className={`relative mt-1.5 h-3.5 w-3.5 shrink-0 rounded-full ring-4 ${dotStyles[h.toStatus] ?? 'bg-slate-400 ring-slate-100'}`}
          />
          <div className="min-w-0 flex-1 pb-1">
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge status={h.toStatus} />
              {h.fromStatus && (
                <span className="text-[11px] text-slate-400">
                  from <span className="font-medium text-slate-500">{h.fromStatus.replace(/_/g, ' ')}</span>
                </span>
              )}
            </div>
            {h.note && <p className="mt-1 text-xs text-slate-500">{h.note}</p>}
            <p className="mt-0.5 text-[11px] text-slate-400">
              {formatDateTime(h.changedAt)} · {h.changedByName}
            </p>
          </div>
        </li>
      ))}
    </ol>
  )
}