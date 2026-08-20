import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'

const COLORS: Record<string, string> = {
  SUBMITTED: '#0ea5e9',
  UNDER_REVIEW: '#f59e0b',
  ADDITIONAL_INFO_REQUIRED: '#8b5cf6',
  APPROVED: '#10b981',
  REJECTED: '#ef4444',
  SETTLED: '#334155',
}

export function ClaimDonut({ data, height = 200 }: { data: Record<string, number>; height?: number }) {
  const entries = Object.entries(data)
    .map(([status, count]) => ({ name: status.replace(/_/g, ' '), value: count }))
    .filter((d) => d.value > 0)

  if (entries.length === 0) {
    return (
      <div className="flex h-[200px] items-center justify-center text-xs text-slate-400">
        No claims yet
      </div>
    )
  }

  return (
    <div style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie data={entries} dataKey="value" nameKey="name" innerRadius={50} outerRadius={75} paddingAngle={3}>
            {entries.map((entry) => (
              <Cell key={entry.name} fill={COLORS[entry.name.toUpperCase().replace(/ /g, '_')] ?? '#94a3b8'} />
            ))}
          </Pie>
          <Tooltip
            contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0', fontSize: 12 }}
          />
        </PieChart>
      </ResponsiveContainer>
      <div className="mt-2 flex flex-wrap justify-center gap-x-4 gap-y-1">
        {entries.map((entry) => (
          <span key={entry.name} className="flex items-center gap-1.5 text-[11px] text-slate-500">
            <span className="h-2 w-2 rounded-full" style={{ background: COLORS[entry.name.toUpperCase().replace(/ /g, '_')] ?? '#94a3b8' }} />
            {entry.name} ({entry.value})
          </span>
        ))}
      </div>
    </div>
  )
}

export function MonthlyBarChart({ data }: { data: { month: string; count: number }[] }) {
  return (
    <ResponsiveContainer width="100%" height={220}>
      <PieChart>
        <Pie
          data={data}
          dataKey="count"
          nameKey="month"
          innerRadius={60}
          outerRadius={85}
          paddingAngle={2}
          label={(props) => (props as { month?: string }).month?.slice(5) ?? ''}
        >
          {data.map((entry) => (
            <Cell key={entry.month} fill="#1b6df5" />
          ))}
        </Pie>
        <Tooltip
          contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0', fontSize: 12 }}
        />
      </PieChart>
    </ResponsiveContainer>
  )
}

export function ClaimsByStatusBar({ data }: { data: Record<string, number> }) {
  const rows = Object.entries(data).map(([status, count]) => ({
    status: status.replace(/_/g, ' '),
    count,
    color: COLORS[status] ?? '#94a3b8',
  }))
  const max = Math.max(1, ...rows.map((r) => r.count))

  return (
    <div className="space-y-2.5">
      {rows.map((row) => (
        <div key={row.status} className="flex items-center gap-3">
          <span className="w-36 shrink-0 text-[11px] font-medium text-slate-500">{row.status}</span>
          <div className="h-2.5 flex-1 overflow-hidden rounded-full bg-slate-100">
            <div
              className="h-full rounded-full transition-all"
              style={{ width: `${(row.count / max) * 100}%`, background: row.color }}
            />
          </div>
          <span className="w-8 text-right text-xs font-semibold text-slate-700">{row.count}</span>
        </div>
      ))}
    </div>
  )
}