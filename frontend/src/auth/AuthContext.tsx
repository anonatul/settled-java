import { createContext, useContext, useState, type ReactNode } from 'react'
import { api, errorMessage } from '../lib/api'
import type { AuthResponse, User } from '../types'

interface AuthContextValue {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<User>
  register: (payload: Record<string, unknown>) => Promise<User>
  logout: () => void
  refreshUser: (user: User) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function loadStoredUser(): User | null {
  try {
    const raw = localStorage.getItem('settled_user')
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(loadStoredUser)
  const [loading, setLoading] = useState(false)

  async function login(email: string, password: string) {
    setLoading(true)
    try {
      const { data } = await api.post<{ data: AuthResponse }>('/auth/login', { email, password })
      const payload = data.data
      localStorage.setItem('settled_token', payload.token)
      localStorage.setItem('settled_user', JSON.stringify(payload.user))
      setUser(payload.user)
      return payload.user
    } catch (err) {
      throw new Error(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  async function register(payload: Record<string, unknown>) {
    setLoading(true)
    try {
      const { data } = await api.post<{ data: AuthResponse }>('/auth/register', payload)
      const auth = data.data
      localStorage.setItem('settled_token', auth.token)
      localStorage.setItem('settled_user', JSON.stringify(auth.user))
      setUser(auth.user)
      return auth.user
    } catch (err) {
      throw new Error(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  function logout() {
    localStorage.removeItem('settled_token')
    localStorage.removeItem('settled_user')
    setUser(null)
  }

  function refreshUser(next: User) {
    localStorage.setItem('settled_user', JSON.stringify(next))
    setUser(next)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}