export type Role = 'CUSTOMER' | 'CLAIM_OFFICER' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'LOCKED'
export type ClaimStatus =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'ADDITIONAL_INFO_REQUIRED'
  | 'APPROVED'
  | 'REJECTED'
  | 'SETTLED'
export type PolicyStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED'

export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  fullName: string
  phone?: string
  role: Role
  status: UserStatus
  createdAt: string
}

export interface Customer {
  id: string
  userId: string
  customerNumber: string
  dateOfBirth?: string
  address?: string
  city?: string
  state?: string
  postalCode?: string
  country?: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: User
  customer: Customer | null
}

export interface PolicyType {
  id: string
  code: string
  name: string
  description?: string
  coverageAmount: number
  premiumRate: number
  active: boolean
}

export interface Policy {
  id: string
  policyNumber: string
  status: PolicyStatus
  startDate: string
  endDate: string
  premium: number
  sumInsured: number
  customerId: string
  customerNumber: string
  customerName: string
  policyTypeId: string
  policyTypeCode: string
  policyTypeName: string
}

export interface Claim {
  id: string
  claimNumber: string
  status: ClaimStatus
  incidentDate: string
  incidentType: string
  description: string
  amountRequested: number
  amountApproved?: number
  submittedAt: string
  decidedAt?: string
  settledAt?: string
  customerId: string
  customerNumber: string
  customerName: string
  policyId: string
  policyNumber: string
  policyTypeName: string
  policySumInsured: number
  assignedOfficerId?: string
  assignedOfficerName?: string
  settlementId?: string
  settlementNumber?: string
  settledAmount?: number
  createdAt: string
}

export interface ClaimDocument {
  id: string
  fileName: string
  contentType: string
  size: number
  uploadedByName: string
  uploadedAt: string
}

export interface ClaimNote {
  id: string
  note: string
  internal: boolean
  authorName: string
  createdAt: string
}

export interface StatusHistory {
  id: string
  fromStatus?: ClaimStatus
  toStatus: ClaimStatus
  changedByName: string
  note?: string
  changedAt: string
}

export interface Settlement {
  id: string
  claimId: string
  settlementNumber: string
  approvedAmount: number
  settledAmount: number
  settlementDate: string
  paymentReference?: string
  processedByName: string
  createdAt: string
}

export interface AuditLog {
  id: string
  actorEmail: string
  actorName: string
  action: string
  entityType?: string
  entityId?: string
  details?: string
  ipAddress?: string
  createdAt: string
}

export interface Analytics {
  totalUsers: number
  totalCustomers: number
  totalOfficers: number
  totalPolicies: number
  totalClaims: number
  pendingClaims: number
  approvedClaims: number
  rejectedClaims: number
  settledClaims: number
  totalSettledAmount: number
  claimsByStatus: Record<string, number>
  monthlyClaims: { month: string; count: number }[]
}

export interface CustomerDashboard {
  policies: number
  totalClaims: number
  pendingClaims: number
  approvedClaims: number
  claimsByStatus: Record<string, number>
  recentClaims: ClaimSummary[]
}

export interface OfficerDashboard {
  assignedClaims: number
  pendingReview: number
  awaitingInfo: number
  claimsByStatus: Record<string, number>
  recentClaims: ClaimSummary[]
}

export interface ClaimSummary {
  id: string
  claimNumber: string
  status: ClaimStatus
  incidentType: string
  amountRequested: number
  submittedAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface ApiEnvelope<T> {
  success: boolean
  message?: string
  data: T
  timestamp: string
}