import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, errorMessage, unwrap } from '../../lib/api'
import { formatDate } from '../../lib/utils'
import type { Customer } from '../../types'
import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { Field, Input } from '../../components/ui/Input'
import { Spinner } from '../../components/ui/Loading'
import { useToast } from '../../components/ui/Toast'
import { useAuth } from '../../auth/AuthContext'

export function Profile() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const { data: profile, isLoading } = useQuery({
    queryKey: ['profile'],
    queryFn: () => unwrap<Customer>(api.get('/customers/me')),
  })
  const [form, setForm] = useState<{
    address: string; city: string; state: string; postalCode: string; country: string; phone: string
  } | null>(null)

  const mutation = useMutation({
    mutationFn: () =>
      unwrap<Customer>(api.put('/customers/me', {
        address: form!.address || null,
        city: form!.city || null,
        state: form!.state || null,
        postalCode: form!.postalCode || null,
        country: form!.country || null,
        phone: form!.phone || null,
      })),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] })
      toast.success('Profile updated')
      setForm(null)
    },
    onError: (err) => toast.error(errorMessage(err)),
  })

  if (isLoading || !profile) return <Spinner />

  const editing = form ?? {
    address: profile.address ?? '',
    city: profile.city ?? '',
    state: profile.state ?? '',
    postalCode: profile.postalCode ?? '',
    country: profile.country ?? '',
    phone: user?.phone ?? '',
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    mutation.mutate()
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Profile</h1>
        <p className="mt-0.5 text-sm text-slate-500">Your personal and contact information</p>
      </div>

      <Card>
        <CardHeader title="Account details" />
        <dl className="grid grid-cols-1 gap-x-8 px-5 py-3 sm:grid-cols-2">
          <div className="flex items-center justify-between border-b border-slate-50 py-2.5">
            <dt className="text-xs text-slate-500">Customer number</dt>
            <dd className="text-sm font-semibold text-slate-700">{profile.customerNumber}</dd>
          </div>
          <div className="flex items-center justify-between border-b border-slate-50 py-2.5">
            <dt className="text-xs text-slate-500">Email</dt>
            <dd className="text-sm text-slate-700">{user?.email}</dd>
          </div>
          <div className="flex items-center justify-between border-b border-slate-50 py-2.5">
            <dt className="text-xs text-slate-500">Date of birth</dt>
            <dd className="text-sm text-slate-700">{profile.dateOfBirth ? formatDate(profile.dateOfBirth) : '—'}</dd>
          </div>
          <div className="flex items-center justify-between border-b border-slate-50 py-2.5">
            <dt className="text-xs text-slate-500">Member since</dt>
            <dd className="text-sm text-slate-700">{user?.createdAt ? formatDate(user.createdAt) : '—'}</dd>
          </div>
        </dl>
      </Card>

      <form onSubmit={onSubmit}>
        <Card className="p-6">
          <CardHeader title="Contact & address" subtitle="Update your phone number and residential address" />
          <div className="mt-4 space-y-5">
            <Field label="Phone">
              <Input value={editing.phone} onChange={(e) => setForm({ ...editing, phone: e.target.value })} placeholder="9876543210" />
            </Field>
            <Field label="Address">
              <Input value={editing.address} onChange={(e) => setForm({ ...editing, address: e.target.value })} placeholder="House, street, area" />
            </Field>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="City">
                <Input value={editing.city} onChange={(e) => setForm({ ...editing, city: e.target.value })} />
              </Field>
              <Field label="State">
                <Input value={editing.state} onChange={(e) => setForm({ ...editing, state: e.target.value })} />
              </Field>
            </div>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Postal code">
                <Input value={editing.postalCode} onChange={(e) => setForm({ ...editing, postalCode: e.target.value })} />
              </Field>
              <Field label="Country">
                <Input value={editing.country} onChange={(e) => setForm({ ...editing, country: e.target.value })} placeholder="India" />
              </Field>
            </div>
            <div className="flex justify-end">
              <Button type="submit" loading={mutation.isPending}>Save changes</Button>
            </div>
          </div>
        </Card>
      </form>
    </div>
  )
}