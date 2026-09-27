import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './context/useAuth'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import DriverHomePage from './pages/driver/DriverHomePage'
import DriverHistoryPage from './pages/driver/DriverHistoryPage'
import AdminLayout from './pages/admin/AdminLayout'
import AdminDriversPage from './pages/admin/AdminDriversPage'
import AdminDeliveriesPage from './pages/admin/AdminDeliveriesPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import AdminInvoicesPage from './pages/admin/AdminInvoicesPage'

function HomeRedirect() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/driver'} replace />
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route
        path="/driver"
        element={
          <ProtectedRoute roles={['CONDUCTOR', 'ADMIN']}>
            <DriverHomePage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/driver/history"
        element={
          <ProtectedRoute roles={['CONDUCTOR', 'ADMIN']}>
            <DriverHistoryPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="drivers" element={<AdminDriversPage />} />
        <Route path="deliveries" element={<AdminDeliveriesPage />} />
        <Route path="dashboard" element={<AdminDashboardPage />} />
        <Route path="invoices" element={<AdminInvoicesPage />} />
      </Route>

      <Route path="/" element={<HomeRedirect />} />
      <Route path="*" element={<HomeRedirect />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}
