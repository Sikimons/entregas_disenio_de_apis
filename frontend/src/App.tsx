import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './context/useAuth'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'

// Carga perezosa por ruta: separa el bundle del conductor (foto/OCR/GPS) del bundle del
// panel admin (que ademas arrastra Leaflet, ~150 kB) para que ninguno de los dos roles
// descargue codigo del otro en el primer login. LoginPage se mantiene estatico porque es
// la pantalla de entrada de ambos roles.
const DriverHomePage = lazy(() => import('./pages/driver/DriverHomePage'))
const DriverHistoryPage = lazy(() => import('./pages/driver/DriverHistoryPage'))
const AdminLayout = lazy(() => import('./pages/admin/AdminLayout'))
const AdminDriversPage = lazy(() => import('./pages/admin/AdminDriversPage'))
const AdminDeliveriesPage = lazy(() => import('./pages/admin/AdminDeliveriesPage'))
const AdminDashboardPage = lazy(() => import('./pages/admin/AdminDashboardPage'))
const AdminInvoicesPage = lazy(() => import('./pages/admin/AdminInvoicesPage'))

function HomeRedirect() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/driver'} replace />
}

function AppRoutes() {
  return (
    <Suspense fallback={null}>
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
    </Suspense>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}
