import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { api, errorMessage, unwrap } from '../../lib/api'
import { formatCurrency } from '../../lib/utils'
import type { Claim, Policy, PolicyType } from '../../types'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { Field, Input, Select, Textarea } from '../../components/ui/Input'
import { useToast } from '../../components/ui/Toast'

export function NewClaim() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [policyId, setPolicyId] = useState('')
  const [incidentType, setIncidentType] = useState('')
  const [incidentDate, setIncidentDate] = useState('')
  const [amountRequested, setAmountRequested] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')

  const { data: policies } = useQuery({
    queryKey: ['policies', 0],
    queryFn: () => unwrap<{ content: Policy[] }>(api.get('/policies', { params: { size: 50 } })),
  })
  const { data: policyTypes } = useQuery({
    queryKey: ['policy-types'],
    queryFn: () => unwrap<PolicyType[]>(api.get('/policy-types')),
  })

  const activePolicies = policies?.content.filter((p) => p.status === 'ACTIVE') ?? []

  const mutation = useMutation({
    mutationFn: () =>
      unwrap<Claim>(api.post('/claims', {
        policyId,
        incidentType,
        incidentDate,
        amountRequested: Number(amountRequested),
        description,
      })),
    onSuccess: (claim) => {
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      toast.success(`Claim ${claim.claimNumber} submitted`)
      navigate(`/claims/${claim.id}`)
    },
    onError: (err) => setError(errorMessage(err)),
  })

  const selectedPolicy = activePolicies.find((p) => p.id === policyId)

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    mutation.mutate()
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Submit a claim</h1>
        <p className="mt-0.5 text-sm text-slate-500">
          Tell us what happened — a claim officer will review your request.
        </p>
      </div>

      <form onSubmit={onSubmit}>
        <Card className="p-6">
          <div className="space-y-5">
            <Field label="Policy" required>
              <Select value={policyId} onChange={(e) => setPolicyId(e.target.value)} required>
                <option value="">Select a policy…</option>
                {activePolicies.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.policyNumber} — {p.policyTypeName} ({formatCurrency(p.sumInsured)})
                  </option>
                ))}
              </Select>
            </Field>

            {selectedPolicy && (
              <div className="rounded-lg bg-brand-50 px-4 py-3 text-xs text-brand-800">
                Sum insured on this policy: <strong>{formatCurrency(selectedPolicy.sumInsured)}</strong>. You can claim
                up to this amount.
              </div>
            )}

            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Incident type" required>
                <Select value={incidentType} onChange={(e) => setIncidentType(e.target.value)} required>
                  <option value="">Select…</option>
                  {policyTypes?.map((t) => <option key={t.id} value={t.name}>{t.name}</option>)}
                  <option value="Fire Damage">Fire Damage</option>
                  <option value="Theft">Theft</option>
                  <option value="Water Damage">Water Damage</option>
                  <option value="Vehicle Accident">Vehicle Accident</option>
                  <option value="Hospitalization">Hospitalization</option>
                  <option value="Critical Illness">Critical Illness</option>
                  <option value="Other">Other</option>
                </Select>
              </Field>
              <Field label="Incident date" required>
                <Input type="date" value={incidentDate} max={new Date().toISOString().slice(0, 10)} onChange={(e) => setIncidentDate(e.target.value)} required />
              </Field>
            </div>

            <Field label="Amount requested (₹)" required>
              <Input
                type="number"
                min="1"
                step="0.01"
                value={amountRequested}
                onChange={(e) => setAmountRequested(e.target.value)}
                placeholder="e.g. 50000"
                required
              />
            </Field>

            <Field label="Description" required>
              <Textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Describe the incident in detail — what happened, when, and what was affected…"
                maxLength={2000}
                required
              />
            </Field>

            {error && <div className="rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-700">{error}</div>}

            <div className="flex justify-end gap-3 pt-2">
              <Button type="button" variant="outline" onClick={() => navigate(-1)}>Cancel</Button>
              <Button type="submit" loading={mutation.isPending}>Submit claim</Button>
            </div>
          </div>
        </Card>
      </form>
    </div>
  )
}