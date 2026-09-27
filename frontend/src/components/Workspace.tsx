import { NavLink } from 'react-router-dom'
import { useEffect, useRef, type ReactNode, type SVGProps } from 'react'
import { useAuth } from '../context/AuthContext'

const paths = {
  arrow: 'M5 12h14m-6-6 6 6-6 6',
  box: 'm12 3 9 5-9 5-9-5 9-5Zm-9 5v9l9 5 9-5V8M12 13v9M7.5 5.5l9 5',
  search: 'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
  map: 'm9 18-6 3V6l6-3 6 3 6-3v15l-6 3-6-3Zm0 0V3m6 3v15',
  history: 'M3 11a9 9 0 1 1 2 7M3 4v7h7m2-4v6l4 2',
  users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2m20 0v-2a4 4 0 0 0-3-3.87M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8m8-7.87a4 4 0 0 1 0 7.75',
  logout: 'M9 21H4V3h5m7 4 5 5-5 5M8 12h13',
  check: 'm5 12 4 4L19 6',
  camera: 'M14 4h-4L8 7H3v14h18V7h-5l-2-3Zm2 10a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  pin: 'M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Zm-5 0a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  shield: 'm12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Zm-4 9 3 3 5-6',
  eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Zm13 0a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
}

export type IconName = keyof typeof paths

interface IconProps extends SVGProps<SVGSVGElement> {
  name?: IconName
  size?: number
}

export function Icon({ name = 'box', size = 20, ...props }: IconProps) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}><path d={paths[name] || paths.box} /></svg>
}
export function Brand() {
  return <div className="brand"><span className="brand-symbol"><Icon name="box" size={25} /></span><span className="brand-word">ruta<span className="brand-period">.</span><small>GESTIÓN DE ENTREGAS</small></span></div>
}
export function PageIntro({ eyebrow, title, description, children }: { eyebrow: string; title: string; description: string; children?: ReactNode }) {
  return <header className="page-intro"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p className="page-description">{description}</p></div>{children}</header>
}
export function Stat({ label, value, icon = 'box', tone = '' }: { label: string; value: ReactNode; icon?: IconName; tone?: string }) {
  return <div className={`stat-card ${tone}`}><span className="stat-icon"><Icon name={icon} /></span><div><span className="stat-label">{label}</span><strong>{value}</strong></div></div>
}
export function Modal({ children, onClose, label }: { children: ReactNode; onClose: () => void; label: string }) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const dialog = ref.current
    dialog?.showModal()
    return () => dialog?.close()
  }, [])
  return <dialog ref={ref} className="photo-modal" aria-label={label} onCancel={event => { event.preventDefault(); onClose() }} onClick={event => {
    if (event.target !== ref.current) return
    const bounds = ref.current!.getBoundingClientRect()
    if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) onClose()
  }}>{children}</dialog>
}
export function DriverShell({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth()
  return <div className="driver-workspace">
    <aside className="driver-rail"><Brand /><p className="rail-caption">TU ESPACIO DE TRABAJO</p>
      <nav aria-label="Navegación del conductor"><NavLink to="/driver" end><Icon name="box" /><span>Mis entregas</span></NavLink><NavLink to="/driver/history"><Icon name="history" /><span>Mi historial</span></NavLink>{user?.role === 'ADMIN' && <NavLink to="/admin/dashboard"><Icon name="map" /><span>Administración</span></NavLink>}</nav>
      <div className="rail-note"><Icon name="shield" size={28} /><strong>Cada entrega cuenta.</strong><p>Verifica los productos y confirma con el PIN de tu cliente.</p></div>
      <div className="rail-user"><span className="avatar">{user?.fullName?.slice(0, 1) || 'U'}</span><div><strong>{user?.fullName}</strong><small>{user?.role === 'ADMIN' ? 'Administrador' : 'Conductor'}</small></div><button className="icon-button" onClick={logout} aria-label="Cerrar sesión"><Icon name="logout" /></button></div>
    </aside><main className="driver-screen" id="main-content">{children}</main>
  </div>
}
