import { useState } from 'react'
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
import { Field, Input, Textarea } from '../../components/ui/Input'
import { useToast } from '../../components/ui/Toast'
import { Modal } from '../../components/ui/Modal'
import { useAuth } from '../../auth/AuthContext'

export function OfficerClaimDetail() {
  const { id } = useParams<{ id: string }>()
  const toast = useToast()
  const queryClient = useQueryClient()
  const { user } = useAuth()

  const [approveModal, setApproveModal] = useState(false)
  const [rejectModal, setRejectModal] = useState(false)
  const [infoModal, setInfoModal] = useState(false)
  const [settleModal, setSettleModal] = useState(false)
  const [actionNote, setActionNote] = useState('')
  const [approveAmount, setApproveAmount] = useState('')
  const [settleAmount, setSettleAmount] = useState('')
  const [settleDate, setSettleDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [paymentReference, setPaymentReference] = useState('')
  const [newNote, setNewNote] = useState('')

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

  function invalidateAll() {
    queryClient.invalidateQueries({ queryKey: ['claim'] })
    queryClient.invalidateQueries({ queryKey: ['claim-history'] })
    queryClient.invalidateQueries({ queryKey: ['claim-documents'] })
    queryClient.invalidateQueries({ queryKey: ['claim-notes'] })
    queryClient.invalidateQueries({ queryKey: ['claim-settlement'] })
    queryClient.invalidateQueries({ queryKey: ['officer-dashboard'] })
    queryClient.invalidateQueries({ queryKey: ['claims'] })
  }

  const assignMutation = useMutation({
    mutationFn: () => unwrap<Claim>(api.post(`/claims/${id}/assign`, { officerId: user?.id })),
    onSuccess: () => { toast.success('Claim assigned to you'); invalidateAll() },
    onError: (err) => toast.error(errorMessage(err)),
  })
  const approveMutation = useMutation({
    mutationFn: () => unwrap<Claim>(api.post(`/claims/${id}/approve`, { amountApproved: Number(approveAmount), note: actionNote || null })),
    onSuccess: () => { toast.success('Claim approved'); closeModals(); invalidateAll() },
    onError: (err) => toast.error(errorMessage(err)),
  })
  const rejectMutation = useMutation({
    mutationFn: () => unwrap<Claim>(api.post(`/claims/${id}/reject`, { reason: actionNote })),
    onSuccess: () => { toast.success('Claim rejected'); closeModals(); invalidateAll() },
    onError: (err) => toast.error(errorMessage(err)),
  })
  const infoMutation = useMutation({
    mutationFn: () => unwrap<Claim>(api.post(`/claims/${id}/request-info`, { note: actionNote })),
    onSuccess: () => { toast.success('Information requested from customer'); closeModals(); invalidateAll() },
    onError: (err) => toast.error(errorMessage(err)),
  })
  const settleMutation = useMutation({
    mutationFn: () => unwrap<Claim>(api.post(`/claims/${id}/settle`, {
      settledAmount: Number(settleAmount),
      paymentReference: paymentReference || null,
      settlementDate: settleDate,
    })),
    onSuccess: (c) => { toast.success(`Claim settled — ${c.settlementNumber ?? 'reference created'}`); closeModals(); invalidateAll() },
    onError: (err) => toast.error(errorMessage(err)),
  })
  const noteMutation = useMutation({
    mutationFn: () => unwrap<ClaimNote>(api.post(`/claims/${id}/notes`, { note: newNote, internal: true })),
    onSuccess: () => { toast.success('Internal note added'); setNewNote(''); invalidateAll() },
    onError: (err) => toast.error(errorMessage(err)),
  })

  function closeModals() {
    setApproveModal(false); setRejectModal(false); setInfoModal(false); setSettleModal(false)
    setActionNote(''); setApproveAmount(''); setSettleAmount(''); setPaymentReference('')
  }

  if (isLoading || !claim || !history) return <Spinner />

  const canAct = claim.status === 'SUBMITTED' || claim.status === 'UNDER_REVIEW' || claim.status === 'ADDITIONAL_INFO_REQUIRED'
  const isAssigned = !!claim.assignedOfficerId
  const isAdmin = user?.role === 'ADMIN'

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-bold text-slate-900">{claim.claimNumber}</h1>
            <StatusBadge status={claim.status} />
          </div>
          <p className="mt-0.5 text-sm text-slate-500">
            {claim.customerName} · {claim.incidentType} · {formatDate(claim.submittedAt)}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {!isAssigned && canAct && (
            isAdmin ? (
              <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-500">
                Unassigned — assign to an officer from their account
              </span>
            ) : (
              <Button onClick={() => assignMutation.mutate()} loading={assignMutation.isPending}>
                Assign to me
              </Button>
            )
          )}
          {isAssigned && canAct && (
            <>
              <Button variant="outline" onClick={() => { setApproveAmount(String(claim.amountRequested)); setApproveModal(true) }}>
                Approve
              </Button>
              <Button variant="outline" onClick={() => setInfoModal(true)}>
                Request info
              </Button>
              <Button variant="danger" onClick={() => setRejectModal(true)}>
                Reject
              </Button>
            </>
          )}
          {isAssigned && claim.status === 'APPROVED' && (
            <Button onClick={() => { setSettleAmount(String(claim.amountApproved ?? '')); setSettleModal(true) }}>
              Settle claim
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="space-y-6 xl:col-span-2">
          <Card>
            <CardHeader title="Claim details" />
            <dl className="grid grid-cols-1 gap-x-8 px-5 py-2 sm:grid-cols-2">
              {[
                ['Policy', claim.policyNumber],
                ['Policy type', claim.policyTypeName],
                ['Incident type', claim.incidentType],
                ['Incident date', formatDate(claim.incidentDate)],
                ['Submitted', formatDate(claim.submittedAt)],
                ['Assigned officer', claim.assignedOfficerName ?? 'Not assigned'],
                ['Customer', claim.customerName],
                ['Customer number', claim.customerNumber],
              ].map(([label, value]) => (
                <div key={label} className="flex items-center justify-between border-b border-slate-50 py-2.5">
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

          <Card>
            <CardHeader title="Status timeline" />
            <div className="px-5 py-4">
              <ClaimTimeline history={history} />
            </div>
          </Card>

          <Card>
            <CardHeader title="Internal notes" subtitle="Only visible to staff" />
            <div className="space-y-3 px-5 py-4">
              {notes?.map((note) => (
                <div key={note.id} className="rounded-lg border border-amber-100 bg-amber-50/50 px-3 py-2.5">
                  <p className="text-xs leading-relaxed text-slate-700">{note.note}</p>
                  <p className="mt-1 text-[10px] text-slate-400">{note.authorName} · {formatDate(note.createdAt)}</p>
                </div>
              ))}
              {notes?.length === 0 && <p className="py-2 text-center text-xs text-slate-400">No internal notes</p>}
              <div className="space-y-2">
                <Textarea value={newNote} onChange={(e) => setNewNote(e.target.value)} placeholder="Add an internal note…" className="min-h-[60px]" />
                <Button size="sm" variant="outline" disabled={!newNote.trim()} loading={noteMutation.isPending} onClick={() => noteMutation.mutate()}>
                  Add note
                </Button>
              </div>
            </div>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader title="Documents" subtitle={`${documents?.length ?? 0} file(s)`} />
            <div className="space-y-2 px-5 py-4">
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
              {documents?.length === 0 && <p className="py-2 text-center text-xs text-slate-400">No documents</p>}
            </div>
          </Card>

          {settlement && (
            <Card className="border-emerald-200 bg-emerald-50/40">
              <CardHeader title="Settlement" />
              <dl className="px-5 py-4">
                {[
                  ['Settlement number', settlement.settlementNumber],
                  ['Approved amount', formatCurrency(settlement.approvedAmount)],
                  ['Settled amount', formatCurrency(settlement.settledAmount)],
                  ['Payment reference', settlement.paymentReference ?? '—'],
                  ['Processed by', settlement.processedByName],
                  ['Date', formatDate(settlement.settlementDate)],
                ].map(([label, value]) => (
                  <div key={label} className="flex items-center justify-between py-1.5">
                    <dt className="text-xs text-slate-500">{label}</dt>
                    <dd className="text-sm font-semibold text-slate-700">{value}</dd>
                  </div>
                ))}
              </dl>
            </Card>
          )}
        </div>
      </div>

      <Modal open={approveModal} onClose={() => setApproveModal(false)} title="Approve claim" subtitle={`${claim.claimNumber} — ${claim.customerName}`}>
        <div className="space-y-4">
          <Field label="Approved amount (₹)" required>
            <Input type="number" min="1" step="0.01" value={approveAmount} onChange={(e) => setApproveAmount(e.target.value)} />
          </Field>
          <Field label="Note" hint="Visible to the customer">
            <Textarea value={actionNote} onChange={(e) => setActionNote(e.target.value)} placeholder="Approval remarks…" />
          </Field>
          <div className="flex justify-end gap-3 pt-1">
            <Button variant="outline" onClick={() => setApproveModal(false)}>Cancel</Button>
            <Button disabled={!approveAmount} loading={approveMutation.isPending} onClick={() => approveMutation.mutate()}>Approve</Button>
          </div>
        </div>
      </Modal>

      <Modal open={infoModal} onClose={() => setInfoModal(false)} title="Request additional information" subtitle="The customer will be asked to respond">
        <div className="space-y-4">
          <Field label="What do you need?" required>
            <Textarea value={actionNote} onChange={(e) => setActionNote(e.target.value)} placeholder="Describe the missing documents or details…" />
          </Field>
          <div className="flex justify-end gap-3 pt-1">
            <Button variant="outline" onClick={() => setInfoModal(false)}>Cancel</Button>
            <Button disabled={!actionNote.trim()} loading={infoMutation.isPending} onClick={() => infoMutation.mutate()}>Request information</Button>
          </div>
        </div>
      </Modal>

      <Modal open={rejectModal} onClose={() => setRejectModal(false)} title="Reject claim" subtitle="This action cannot be undone">
        <div className="space-y-4">
          <Field label="Reason" required>
            <Textarea value={actionNote} onChange={(e) => setActionNote(e.target.value)} placeholder="Why is this claim being rejected?" />
          </Field>
          <div className="flex justify-end gap-3 pt-1">
            <Button variant="outline" onClick={() => setRejectModal(false)}>Cancel</Button>
            <Button variant="danger" disabled={!actionNote.trim()} loading={rejectMutation.isPending} onClick={() => rejectMutation.mutate()}>Reject claim</Button>
          </div>
        </div>
      </Modal>

      <Modal open={settleModal} onClose={() => setSettleModal(false)} title="Settle claim" subtitle="Final payment against the approved amount">
        <div className="space-y-4">
          <Field label="Settled amount (₹)" required hint={`Approved amount is ${formatCurrency(claim.amountApproved ?? 0)}`}>
            <Input type="number" min="1" step="0.01" value={settleAmount} onChange={(e) => setSettleAmount(e.target.value)} />
          </Field>
          <Field label="Settlement date" required>
            <Input type="date" value={settleDate} max={new Date().toISOString().slice(0, 10)} onChange={(e) => setSettleDate(e.target.value)} />
          </Field>
          <Field label="Payment reference">
            <Input value={paymentReference} onChange={(e) => setPaymentReference(e.target.value)} placeholder="e.g. NEFT/UTR number" />
          </Field>
          <div className="flex justify-end gap-3 pt-1">
            <Button variant="outline" onClick={() => setSettleModal(false)}>Cancel</Button>
            <Button disabled={!settleAmount || !settleDate} loading={settleMutation.isPending} onClick={() => settleMutation.mutate()}>Settle claim</Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}