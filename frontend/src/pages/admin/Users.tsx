import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, errorMessage, unwrap } from '../../lib/api'
import { formatDate } from '../../lib/utils'
import type { PageResponse, Role, User } from '../../types'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Select } from '../../components/ui/Input'
import { TableSkeleton } from '../../components/ui/Loading'
import { Pagination } from '../../components/ui/Pagination'
import { useToast } from '../../components/ui/Toast'
import { Modal } from '../../components/ui/Modal'

export function Users() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [role, setRole] = useState<Role | ''>('')
  const [pending, setPending] = useState<User | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['admin-users', page, role],
    queryFn: () => unwrap<PageResponse<User>>(
      api.get('/admin/users', { params: { page, size: 10, role: role || undefined } }),
    ),
  })

  const statusMutation = useMutation({
    mutationFn: (status: 'ACTIVE' | 'LOCKED') => unwrap(api.put(`/admin/users/${pending!.id}/status`, null, { params: { status } })),
    onSuccess: () => {
      toast.success(`${pending!.fullName} ${pending!.status === 'ACTIVE' ? 'locked' : 'activated'}`)
      setPending(null)
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
    onError: (err) => toast.error(errorMessage(err)),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Users</h1>
          <p className="mt-0.5 text-sm text-slate-500">Every account on the platform</p>
        </div>
        <Select value={role} onChange={(e) => { setRole(e.target.value as Role | ''); setPage(0) }} className="max-w-[180px]">
          <option value="">All roles</option>
          <option value="CUSTOMER">Customer</option>
          <option value="CLAIM_OFFICER">Claim officer</option>
          <option value="ADMIN">Admin</option>
        </Select>
      </div>

      <Card>
        {isLoading || !data ? (
          <TableSkeleton />
        ) : data.totalElements === 0 ? (
          <div className="px-5 py-12 text-center text-sm text-slate-400">No users found.</div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-[11px] uppercase tracking-wider text-slate-400">
                    <th className="px-5 py-3 font-semibold">User</th>
                    <th className="px-5 py-3 font-semibold">Email</th>
                    <th className="px-5 py-3 font-semibold">Phone</th>
                    <th className="px-5 py-3 font-semibold">Role</th>
                    <th className="px-5 py-3 font-semibold">Joined</th>
                    <th className="px-5 py-3 font-semibold">Status</th>
                    <th className="px-5 py-3 font-semibold text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {data.content.map((u) => (
                    <tr key={u.id} className="transition-colors hover:bg-slate-50">
                      <td className="px-5 py-3.5 font-semibold text-slate-800">{u.fullName}</td>
                      <td className="px-5 py-3.5 text-slate-600">{u.email}</td>
                      <td className="px-5 py-3.5 text-slate-600">{u.phone ?? '—'}</td>
                      <td className="px-5 py-3.5">
                        <span className="inline-flex rounded-full bg-brand-50 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-brand-700">
                          {u.role.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-slate-500">{formatDate(u.createdAt)}</td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide ${u.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>
                          {u.status}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-right">
                        {u.role !== 'ADMIN' && (u.status === 'ACTIVE' ? (
                          <Button size="sm" variant="outline" onClick={() => setPending(u)}>Lock</Button>
                        ) : (
                          <Button size="sm" onClick={() => setPending(u)}>Activate</Button>
                        ))}
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
        title={pending?.status === 'ACTIVE' ? 'Lock account' : 'Activate account'}
        subtitle={pending ? `${pending.fullName} (${pending.email})` : undefined}
      >
        <p className="text-sm text-slate-600">
          {pending?.status === 'ACTIVE'
            ? 'A locked user cannot sign in or use the platform until reactivated.'
            : 'This user will regain full access to the platform.'}
        </p>
        <div className="mt-5 flex justify-end gap-3">
          <Button variant="outline" onClick={() => setPending(null)}>Cancel</Button>
          <Button
            variant={pending?.status === 'ACTIVE' ? 'danger' : 'primary'}
            loading={statusMutation.isPending}
            onClick={() => statusMutation.mutate(pending!.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE')}
          >
            {pending?.status === 'ACTIVE' ? 'Lock account' : 'Activate'}
          </Button>
        </div>
      </Modal>
    </div>
  )
}