import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { homeFor } from '../../components/ProtectedRoute'
import { Button } from '../../components/ui/Button'
import { Field, Input } from '../../components/ui/Input'
import { useToast } from '../../components/ui/Toast'

export function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      const user = await login(email, password)
      toast.success(`Welcome back, ${user.firstName}`)
      navigate(homeFor(user.role), { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    }
  }

  return (
    <div className="flex min-h-screen">
      <div className="hidden flex-1 flex-col justify-between bg-slate-900 p-12 lg:flex">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-600 text-white">
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <div>
            <p className="text-lg font-bold text-white">Settled</p>
            <p className="text-xs text-slate-400">Insurance Claims Platform</p>
          </div>
        </div>
        <div className="max-w-md">
          <h1 className="text-3xl font-bold leading-tight text-white">
            Claims handled with clarity and speed.
          </h1>
          <p className="mt-4 text-sm leading-relaxed text-slate-400">
            Submit claims, upload documents, track approvals and view settlements — all in one
            secure place.
          </p>
          <div className="mt-8 space-y-3">
            {['Submit claims in minutes', 'Track every status change', 'Secure role-based access'].map((f) => (
              <div key={f} className="flex items-center gap-3 text-sm text-slate-300">
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-400">
                  <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                  </svg>
                </span>
                {f}
              </div>
            ))}
          </div>
        </div>
        <p className="text-xs text-slate-600">
          Demo accounts: customer1@settled.io · officer1@settled.io · admin@settled.io (password123)
        </p>
      </div>

      <div className="flex flex-1 items-center justify-center bg-slate-50 p-8">
        <div className="w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <p className="text-2xl font-bold text-slate-900">Settled</p>
          </div>
          <h2 className="text-2xl font-bold text-slate-900">Sign in</h2>
          <p className="mt-1 text-sm text-slate-500">Access your insurance workspace</p>

          <form onSubmit={onSubmit} className="mt-8 space-y-4">
            <Field label="Email" required>
              <Input
                type="email"
                name="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                required
                autoFocus
              />
            </Field>
            <Field label="Password" required>
              <Input
                type="password"
                name="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                required
              />
            </Field>
            {error && (
              <div className="rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-700">
                {error}
              </div>
            )}
            <Button type="submit" className="w-full" size="lg">
              Sign in
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            New customer?{' '}
            <Link to="/register" className="font-semibold text-brand-600 hover:text-brand-700">
              Create an account
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}