import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { ToastProvider } from './components/ui/Toast'
import { AppLayout } from './components/layout/AppLayout'
import { RequireAuth, RequireRole } from './components/ProtectedRoute'
import { Login } from './pages/auth/Login'
import { Register } from './pages/auth/Register'
import { CustomerDashboard } from './pages/customer/Dashboard'
import { Policies } from './pages/customer/Policies'
import { PolicyDetail } from './pages/customer/PolicyDetail'
import { Claims } from './pages/customer/Claims'
import { NewClaim } from './pages/customer/NewClaim'
import { ClaimDetail } from './pages/customer/ClaimDetail'
import { Profile } from './pages/customer/Profile'
import { OfficerDashboard } from './pages/officer/Dashboard'
import { Claims as OfficerClaims } from './pages/officer/Claims'
import { OfficerClaimDetail } from './pages/officer/ClaimDetail'
import { AdminDashboard } from './pages/admin/Dashboard'
import { Users as AdminUsers } from './pages/admin/Users'
import { Officers as AdminOfficers } from './pages/admin/Officers'
import { Policies as AdminPolicies } from './pages/admin/Policies'
import { Claims as AdminClaims } from './pages/admin/Claims'
import { AuditLogs as AdminAuditLogs } from './pages/admin/AuditLogs'
import { NotFound } from './pages/NotFound'

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          <Route
            element={
              <RequireAuth>
                <AppLayout />
              </RequireAuth>
            }
          >
            <Route path="/dashboard" element={<RequireRole role="CUSTOMER"><CustomerDashboard /></RequireRole>} />
            <Route path="/policies" element={<RequireRole role="CUSTOMER"><Policies /></RequireRole>} />
            <Route path="/policies/:id" element={<RequireRole role="CUSTOMER"><PolicyDetail /></RequireRole>} />
            <Route path="/claims" element={<RequireRole role="CUSTOMER"><Claims /></RequireRole>} />
            <Route path="/claims/new" element={<RequireRole role="CUSTOMER"><NewClaim /></RequireRole>} />
            <Route path="/claims/:id" element={<RequireRole role="CUSTOMER"><ClaimDetail /></RequireRole>} />
            <Route path="/profile" element={<RequireRole role="CUSTOMER"><Profile /></RequireRole>} />

            <Route path="/officer/dashboard" element={<RequireRole role="CLAIM_OFFICER"><OfficerDashboard /></RequireRole>} />
            <Route path="/officer/claims" element={<RequireRole role="CLAIM_OFFICER"><OfficerClaims /></RequireRole>} />
            <Route path="/officer/claims/:id" element={<RequireRole role="CLAIM_OFFICER"><OfficerClaimDetail /></RequireRole>} />

            <Route path="/admin/dashboard" element={<RequireRole role="ADMIN"><AdminDashboard /></RequireRole>} />
            <Route path="/admin/users" element={<RequireRole role="ADMIN"><AdminUsers /></RequireRole>} />
            <Route path="/admin/officers" element={<RequireRole role="ADMIN"><AdminOfficers /></RequireRole>} />
            <Route path="/admin/policies" element={<RequireRole role="ADMIN"><AdminPolicies /></RequireRole>} />
            <Route path="/admin/claims" element={<RequireRole role="ADMIN"><AdminClaims /></RequireRole>} />
            <Route path="/admin/claims/:id" element={<RequireRole role="ADMIN"><OfficerClaimDetail /></RequireRole>} />
            <Route path="/admin/audit-logs" element={<RequireRole role="ADMIN"><AdminAuditLogs /></RequireRole>} />
          </Route>

          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </ToastProvider>
    </AuthProvider>
  )
}