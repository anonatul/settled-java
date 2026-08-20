import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import type { Role } from '../types'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }
  return <>{children}</>
}

export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== role) {
    return <Navigate to={homeFor(user.role)} replace />
  }
  return <>{children}</>
}

export function homeFor(role: Role): string {
  switch (role) {
    case 'CUSTOMER':
      return '/dashboard'
    case 'CLAIM_OFFICER':
      return '/officer/dashboard'
    case 'ADMIN':
      return '/admin/dashboard'
  }
}