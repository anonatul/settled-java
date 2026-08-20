import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { api, errorMessage, unwrap } from '../../lib/api'
import { formatBytes, formatCurrency, formatDate } from '../../lib/utils'
import type { Claim, ClaimDocument, ClaimNote, Settlement, StatusHistory } from '../../types'
import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { StatusBadge } from '../../components/ui/Badge'
import { Spinner } from '../../components/ui/Loading'
import { ClaimTimeline } from '../../components/ClaimTimeline'
import { Field, Textarea } from '../../components/ui/Input'
import { useToast } from '../../components/ui/Toast'
import { useState } from 'react'

export function ClaimDetail() {
  const { id } = useParams<{ id: string }>()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [infoNote, setInfoNote] = useState('')
  const [newNote, setNewNote] = useState('')
  const [file, setFile] = useState<File | null>(null)

  const { data: claim, isLoading } = useQuery({
    queryKey: ['claim', id],
    queryFn: () => unwrap<Claim>(api.get(`/claims/${id}`)),
    enabled: !!id,
  })
  const { data: history } = useQuery({
    queryKey: ['claim-history', id],
    queryFn: () => unwrap<StatusHistory[]>(api.get(`/claims/${id}/history`)),
    enabled: !!id,
  })
  const { data: documents } = useQuery({
    queryKey: ['claim-documents', id],
    queryFn: () => unwrap<ClaimDocument[]>(api.get(`/claims/${id}/documents`)),
    enabled: !!id,
  })
  const { data: notes } = useQuery({
    queryKey: ['claim-notes', id],
    queryFn: () => unwrap<ClaimNote[]>(api.get(`/claims/${id}/notes`)),
    enabled: !!id,
  })
  const { data: settlement } = useQuery({
    queryKey: ['claim-settlement', id],
    queryFn: () => unwrap<Settlement>(api.get(`/claims/${id}/settlement`)),
    enabled: !!id,
    retry: false,
  })

  const respondMutation = useMutation({
    mutationFn: () => unwrap<Claim>(api.post(`/claims/${id}/respond-info`, { note: infoNote })),
    onSuccess: () => {
      toast.success('Information provided — claim is back under review')
      setInfoNote('')
      invalidateAll()
    },
    onError: (err) => toast.error(errorMessage(err)),
  })

  const noteMutation = useMutation({
    mutationFn: () => unwrap<ClaimNote>(api.post(`/claims/${id}/notes`, { note: newNote, internal: false })),
    onSuccess: () => {
      toast.success('Note added')
      setNewNote('')
      invalidateAll()
    },
    onError: (err) => toast.error(errorMessage(err)),
  })

  const uploadMutation = useMutation({
    mutationFn: () => {
      const form = new FormData()
      form.append('file', file!)
      return unwrap<ClaimDocument>(
        api.post(`/claims/${id}/documents`, form, { headers: { 'Content-Type': 'multipart/form-data' } }),
      )
    },
    onSuccess: () => {
      toast.success('Document uploaded')
      setFile(null)
      invalidateAll()
    },
    onError: (err) => toast.error(errorMessage(err)),
  })

  function invalidateAll() {
    queryClient.invalidateQueries({ queryKey: ['claim'] })
    queryClient.invalidateQueries({ queryKey: ['claim-history'] })
    queryClient.invalidateQueries({ queryKey: ['claim-documents'] })
    queryClient.invalidateQueries({ queryKey: ['claim-notes'] })
    queryClient.invalidateQueries({ queryKey: ['claim-settlement'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  if (isLoading || !claim || !history) return <Spinner />

  const details: [string, React.ReactNode][] = [
    ['Claim number', <span className="font-semibold">{claim.claimNumber}</span>],
    ['Policy', claim.policyNumber],
    ['Policy type', claim.policyTypeName],
    ['Incident type', claim.incidentType],
    ['Incident date', formatDate(claim.incidentDate)],
    ['Submitted', formatDate(claim.submittedAt)],
    ['Assigned officer', claim.assignedOfficerName ?? 'Not assigned yet'],
  ]

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-bold text-slate-900">{claim.claimNumber}</h1>
            <StatusBadge status={claim.status} />
          </div>
          <p className="mt-0.5 text-sm text-slate-500">{claim.incidentType} · {formatDate(claim.submittedAt)}</p>
        </div>
        {claim.status === 'ADDITIONAL_INFO_REQUIRED' && (
          <div className="rounded-xl border border-violet-200 bg-violet-50 px-4 py-2.5 text-xs font-medium text-violet-700">
            A claim officer has requested additional information
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="space-y-6 xl:col-span-2">
          <Card>
            <CardHeader title="Claim details" />
            <dl className="grid grid-cols-1 gap-x-8 px-5 py-2 sm:grid-cols-2">
              {details.map(([label, value]) => (
                <div key={label} className="flex items-center justify-between border-b border-slate-50 py-2.5 last:border-0">
                  <dt className="text-xs text-slate-500">{label}</dt>
                  <dd className="text-sm text-slate-700">{value}</dd>
                </div>
              ))}
              <div className="flex items-center justify-between py-2.5">
                <dt className="text-xs text-slate-500">Amount requested</dt>
                <dd className="text-sm font-semibold text-slate-800">{formatCurrency(claim.amountRequested)}</dd>
              </div>
              <div className="flex items-center justify-between py-2.5">
                <dt className="text-xs text-slate-500">Amount approved</dt>
                <dd className="text-sm font-semibold text-emerald-700">
                  {claim.amountApproved != null ? formatCurrency(claim.amountApproved) : '—'}
                </dd>
              </div>
            </dl>
            <div className="border-t border-slate-100 px-5 py-4">
              <p className="text-xs font-medium text-slate-500">Description</p>
              <p className="mt-1.5 text-sm leading-relaxed text-slate-700">{claim.description}</p>
            </div>
          </Card>

          {claim.status === 'ADDITIONAL_INFO_REQUIRED' && (
            <Card className="border-violet-200">
              <CardHeader title="Respond to information request" subtitle="Provide the requested details to continue the review" />
              <div className="space-y-3 px-5 py-4">
                <Field label="Your response" required>
                  <Textarea value={infoNote} onChange={(e) => setInfoNote(e.target.value)} placeholder="Attach or describe the requested information…" />
                </Field>
                <Button onClick={() => respondMutation.mutate()} loading={respondMutation.isPending} disabled={!infoNote.trim()}>
                  Submit response
                </Button>
              </div>
            </Card>
          )}

          <Card>
            <CardHeader title="Status timeline" subtitle="Every status change on this claim" />
            <div className="px-5 py-4">
              <ClaimTimeline history={history} />
            </div>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader title="Documents" subtitle={`${documents?.length ?? 0} file(s)`} />
            <div className="px-5 py-4">
              <div className="space-y-2">
                {documents?.map((doc) => (
                  <a
                    key={doc.id}
                    href={`/api/v1/claims/${claim.id}/documents/${doc.id}/download`}
                    className="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-3 py-2.5 transition-colors hover:border-brand-300 hover:bg-brand-50/50"
                  >
                    <div className="flex min-w-0 items-center gap-2.5">
                      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-500">
                        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                        </svg>
                      </span>
                      <div className="min-w-0">
                        <p className="truncate text-xs font-semibold text-slate-700">{doc.fileName}</p>
                        <p className="text-[10px] text-slate-400">{formatBytes(doc.size)} · {doc.uploadedByName}</p>
                      </div>
                    </div>
                    <svg className="h-4 w-4 shrink-0 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                    </svg>
                  </a>
                ))}
                {documents?.length === 0 && (
                  <p className="py-4 text-center text-xs text-slate-400">No documents uploaded yet</p>
                )}
              </div>
              {claim.status !== 'SETTLED' && claim.status !== 'REJECTED' && (
                <div className="mt-4">
                  <input
                    type="file"
                    id="claim-file"
                    className="hidden"
                    onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                  />
                  <label htmlFor="claim-file">
                    <span className="block cursor-pointer rounded-lg border border-dashed border-slate-300 px-3 py-3 text-center text-xs font-medium text-slate-500 transition-colors hover:border-brand-400 hover:text-brand-600">
                      {file ? file.name : 'Upload a document (PDF / image)'}
                    </span>
                  </label>
                  <Button className="mt-2 w-full" size="sm" variant="outline" disabled={!file} loading={uploadMutation.isPending} onClick={() => uploadMutation.mutate()}>
                    Upload
                  </Button>
                </div>
              )}
            </div>
          </Card>

          {settlement && (
            <Card className="border-emerald-200 bg-emerald-50/40">
              <CardHeader title="Settlement" subtitle="This claim has been settled" />
              <dl className="px-5 py-4">
                <div className="flex items-center justify-between py-1.5">
                  <dt className="text-xs text-slate-500">Settlement number</dt>
                  <dd className="text-sm font-semibold text-slate-800">{settlement.settlementNumber}</dd>
                </div>
                <div className="flex items-center justify-between py-1.5">
                  <dt className="text-xs text-slate-500">Approved amount</dt>
                  <dd className="text-sm text-slate-700">{formatCurrency(settlement.approvedAmount)}</dd>
                </div>
                <div className="flex items-center justify-between py-1.5">
                  <dt className="text-xs text-slate-500">Settled amount</dt>
                  <dd className="text-sm font-bold text-emerald-700">{formatCurrency(settlement.settledAmount)}</dd>
                </div>
                <div className="flex items-center justify-between py-1.5">
                  <dt className="text-xs text-slate-500">Payment reference</dt>
                  <dd className="text-sm text-slate-700">{settlement.paymentReference ?? '—'}</dd>
                </div>
                <div className="flex items-center justify-between py-1.5">
                  <dt className="text-xs text-slate-500">Settlement date</dt>
                  <dd className="text-sm text-slate-700">{formatDate(settlement.settlementDate)}</dd>
                </div>
              </dl>
            </Card>
          )}

          <Card>
            <CardHeader title="Notes" subtitle={`${notes?.length ?? 0} note(s)`} />
            <div className="space-y-3 px-5 py-4">
              {notes?.map((note) => (
                <div key={note.id} className="rounded-lg bg-slate-50 px-3 py-2.5">
                  <p className="text-xs leading-relaxed text-slate-700">{note.note}</p>
                  <p className="mt-1 text-[10px] text-slate-400">{note.authorName} · {formatDate(note.createdAt)}</p>
                </div>
              ))}
              {notes?.length === 0 && <p className="py-2 text-center text-xs text-slate-400">No notes yet</p>}
              {claim.status !== 'SETTLED' && claim.status !== 'REJECTED' && (
                <div className="space-y-2">
                  <Textarea value={newNote} onChange={(e) => setNewNote(e.target.value)} placeholder="Add a note…" className="min-h-[60px]" />
                  <Button size="sm" variant="outline" disabled={!newNote.trim()} loading={noteMutation.isPending} onClick={() => noteMutation.mutate()}>
                    Add note
                  </Button>
                </div>
              )}
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}