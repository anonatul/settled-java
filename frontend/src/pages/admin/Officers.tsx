import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, errorMessage, unwrap } from '../../lib/api'
import type { PageResponse, User } from '../../types'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { TableSkeleton } from '../../components/ui/Loading'
import { Pagination } from '../../components/ui/Pagination'
import { useToast } from '../../components/ui/Toast'
import { Modal } from '../../components/ui/Modal'

export function Officers() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [pending, setPending] = useState<User | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['admin-officers', page],
    queryFn: () => unwrap<PageResponse<User>>(api.get('/admin/officers', { params: { page, size: 12 } })),
  })

  const statusMutation = useMutation({
    mutationFn: (status: 'ACTIVE' | 'LOCKED') => unwrap(api.put(`/admin/users/${pending!.id}/status`, null, { params: { status } })),
    onSuccess: () => {
      toast.success(`${pending!.fullName} ${pending!.status === 'ACTIVE' ? 'locked' : 'activated'}`)
      setPending(null)
      queryClient.invalidateQueries({ queryKey: ['admin-officers'] })
    },
    onError: (err) => toast.error(errorMessage(err)),
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Claim Officers</h1>
        <p className="mt-0.5 text-sm text-slate-500">Staff who review and settle claims</p>
      </div>

      <Card>
        {isLoading || !data ? (
          <TableSkeleton />
        ) : data.totalElements === 0 ? (
          <div className="px-5 py-12 text-center text-sm text-slate-400">No officers found.</div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-[11px] uppercase tracking-wider text-slate-400">
                    <th className="px-5 py-3 font-semibold">Officer</th>
                    <th className="px-5 py-3 font-semibold">Email</th>
                    <th className="px-5 py-3 font-semibold">Phone</th>
                    <th className="px-5 py-3 font-semibold">Joined</th>
                    <th className="px-5 py-3 font-semibold">Status</th>
                    <th className="px-5 py-3 font-semibold text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {data.content.map((o) => (
                    <tr key={o.id} className="transition-colors hover:bg-slate-50">
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-xs font-bold text-brand-700">
                            {o.firstName[0]}{o.lastName[0]}
                          </span>
                          <span className="font-semibold text-slate-800">{o.fullName}</span>
                        </div>
                      </td>
                      <td className="px-5 py-3.5 text-slate-600">{o.email}</td>
                      <td className="px-5 py-3.5 text-slate-600">{o.phone ?? '—'}</td>
                      <td className="px-5 py-3.5 text-slate-500">{new Date(o.createdAt).toLocaleDateString()}</td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide ${o.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>
                          {o.status}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-right">
                        {o.status === 'ACTIVE' ? (
                          <Button size="sm" variant="outline" onClick={() => setPending(o)}>Lock</Button>
                        ) : (
                          <Button size="sm" onClick={() => setPending(o)}>Activate</Button>
                        )}
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

      <Modal
        open={!!pending}
        onClose={() => setPending(null)}
        title={pending?.status === 'ACTIVE' ? 'Lock officer' : 'Activate officer'}
        subtitle={pending ? `${pending.fullName} (${pending.email})` : undefined}
      >
        <p className="text-sm text-slate-600">
          {pending?.status === 'ACTIVE'
            ? 'A locked officer cannot sign in. Their currently assigned claims remain assigned.'
            : 'The officer will regain access to the officer dashboard.'}
        </p>
        <div className="mt-5 flex justify-end gap-3">
          <Button variant="outline" onClick={() => setPending(null)}>Cancel</Button>
          <Button
            variant={pending?.status === 'ACTIVE' ? 'danger' : 'primary'}
            loading={statusMutation.isPending}
            onClick={() => statusMutation.mutate(pending!.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE')}
          >
            {pending?.status === 'ACTIVE' ? 'Lock officer' : 'Activate'}
          </Button>
        </div>
      </Modal>
    </div>
  )
}