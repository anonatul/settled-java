import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, unwrap } from '../../lib/api'
import { formatDateTime } from '../../lib/utils'
import type { AuditLog, PageResponse } from '../../types'
import { Card } from '../../components/ui/Card'
import { Select, Input } from '../../components/ui/Input'
import { TableSkeleton } from '../../components/ui/Loading'
import { Pagination } from '../../components/ui/Pagination'

const ACTIONS = [
  '', 'LOGIN', 'USER_REGISTERED', 'CLAIM_SUBMITTED', 'CLAIM_ASSIGNED',
  'CLAIM_APPROVED', 'CLAIM_REJECTED', 'CLAIM_STATUS_CHANGED', 'CLAIM_SETTLED',
  'DOCUMENT_UPLOADED', 'POLICY_CREATED', 'USER_ROLE_CHANGED',
]

export function AuditLogs() {
  const [page, setPage] = useState(0)
  const [action, setAction] = useState('')
  const [search, setSearch] = useState('')
  const [query, setQuery] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['admin-audit', page, action, query],
    queryFn: () => unwrap<PageResponse<AuditLog>>(
      api.get('/admin/audit-logs', {
        params: { page, size: 20, action: action || undefined, q: query || undefined },
      }),
    ),
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Audit Logs</h1>
        <p className="mt-0.5 text-sm text-slate-500">Immutable trail of every important action</p>
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-100 px-5 py-4">
          <Select value={action} onChange={(e) => { setAction(e.target.value); setPage(0) }} className="max-w-[220px]">
            <option value="">All actions</option>
            {ACTIONS.filter(Boolean).map((a) => (
              <option key={a} value={a}>{a.replace(/_/g, ' ')}</option>
            ))}
          </Select>
          <form
            className="flex-1 sm:max-w-xs"
            onSubmit={(e) => { e.preventDefault(); setPage(0); setQuery(search) }}
          >
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search email…"
              className="w-full"
            />
          </form>
        </div>
        {isLoading || !data ? (
          <TableSkeleton />
        ) : data.totalElements === 0 ? (
          <div className="px-5 py-12 text-center text-sm text-slate-400">No log entries found.</div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-[11px] uppercase tracking-wider text-slate-400">
                    <th className="px-5 py-3 font-semibold">When</th>
                    <th className="px-5 py-3 font-semibold">Action</th>
                    <th className="px-5 py-3 font-semibold">Actor</th>
                    <th className="px-5 py-3 font-semibold">Target</th>
                    <th className="px-5 py-3 font-semibold">Details</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {data.content.map((log) => (
                    <tr key={log.id} className="transition-colors hover:bg-slate-50">
                      <td className="whitespace-nowrap px-5 py-3 text-xs text-slate-500">{formatDateTime(log.createdAt)}</td>
                      <td className="px-5 py-3">
                        <span className="inline-flex rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-slate-600">
                          {log.action.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td className="px-5 py-3">
                        <p className="text-xs font-semibold text-slate-700">{log.actorName}</p>
                        <p className="text-[10px] text-slate-400">{log.actorEmail}</p>
                      </td>
                      <td className="px-5 py-3">
                        <p className="text-xs text-slate-600">{log.entityType ?? '—'}</p>
                        {log.entityId && <p className="font-mono text-[10px] text-slate-400">{log.entityId.slice(0, 8)}</p>}
                      </td>
                      <td className="max-w-[300px] px-5 py-3 text-xs text-slate-500">
                        <span className="line-clamp-2">{log.details || '—'}</span>
                      </td>
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