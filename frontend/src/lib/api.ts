import axios from 'axios'
import type { ApiEnvelope } from '../types'

export const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('settled_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('settled_token')
      localStorage.removeItem('settled_user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<T> {
  const { data } = await promise
  if (!data.success) {
    throw new Error(data.message ?? 'Request failed')
  }
  return data.data
}

export function errorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const envelope = err.response?.data as ApiEnvelope<unknown> | undefined
    if (envelope?.message) return envelope.message
    if (err.response?.status === 429) return 'Too many attempts. Please try again later.'
    if (err.response?.status === 403) return 'You do not have permission to do this.'
  }
  return err instanceof Error ? err.message : 'Something went wrong'
}