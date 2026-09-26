import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { Brand, Icon } from '../../components/Workspace'

export default function AdminLayout() {
  const { user, logout } = useAuth()

  return (
    <div className="admin-shell">
      <header className="admin-topbar"><Brand /><span className="workspace-tag">CENTRO DE OPERACIONES</span><div className="admin-profile"><span className="avatar">{user?.fullName?.slice(0, 1)}</span><span>{user?.fullName}<small>Administrador</small></span><button className="icon-button" onClick={logout} aria-label="Cerrar sesión"><Icon name="logout" /></button></div></header>
        <nav className="admin-nav" aria-label="Administración">
          <NavLink to="/admin/dashboard"><Icon name="map" />Panorama</NavLink>
          <NavLink to="/admin/invoices"><Icon name="box" />Facturas</NavLink>
          <NavLink to="/admin/drivers" className={({ isActive }) => (isActive ? 'active' : '')}>
            <Icon name="users" />Equipo
          </NavLink>
          <NavLink to="/admin/deliveries" className={({ isActive }) => (isActive ? 'active' : '')}>
            <Icon name="history" />Entregas
          </NavLink>
          <NavLink to="/driver" className="driver-switch">
            Vista de conductor<Icon name="arrow" />
          </NavLink>
        </nav>
      <main className="admin-content" id="main-content">
        <Outlet />
      </main>
      <footer className="workspace-footer"><span>ruta. / Operaciones</span><span>Una entrega a la vez.</span></footer>
    </div>
  )
}
