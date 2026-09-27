import { useEffect, useState, type FormEvent } from 'react'
import apiClient from '../../api/client'
import TableSkeleton from '../../components/TableSkeleton'
import { PageIntro, Stat } from '../../components/Workspace'
import type { Driver, Role } from '../../types/domain'

interface DriverForm {
  username: string
  password: string
  fullName: string
  role: Role
}

const emptyForm: DriverForm = { username: '', password: '', fullName: '', role: 'CONDUCTOR' }

export default function AdminDriversPage() {
  const [users, setUsers] = useState<Driver[]>([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState<DriverForm>(emptyForm)
  const [error, setError] = useState('')

  const loadUsers = async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await apiClient.get<Driver[]>('/admin/users')
      setUsers(data)
    } catch (err: any) {
      setError(err.response?.data?.message || 'No se pudo cargar el equipo.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadUsers()
  }, [])

  const handleCreate = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError('')
    try {
      await apiClient.post('/admin/users', form)
      setForm(emptyForm)
      loadUsers()
    } catch (err: any) {
      setError(err.response?.data?.message || 'No se pudo crear el usuario.')
    }
  }

  const toggleActive = async (user: Driver) => {
    setError('')
    try {
      await apiClient.put(`/admin/users/${user.id}`, {
        fullName: user.fullName,
        active: !user.active,
        password: null,
      })
      loadUsers()
    } catch (err: any) {
      setError(err.response?.data?.message || 'No se pudo actualizar el estado del usuario.')
    }
  }

  const removeUser = async (user: Driver) => {
    if (!window.confirm(`¿Eliminar al usuario ${user.username}?`)) return
    setError('')
    try {
      await apiClient.delete(`/admin/users/${user.id}`)
      loadUsers()
    } catch (err: any) {
      setError(err.response?.data?.message || 'No se pudo eliminar el usuario.')
    }
  }

  return (
    <div>
      <PageIntro eyebrow="LAS PERSONAS DETRÁS DE CADA ENTREGA" title="Tu equipo" description="Gestiona las cuentas y los accesos de tu operación." />
      <div className="stats-grid"><Stat label="Personas en el equipo" value={loading ? '—' : users.length} icon="users" /><Stat label="Cuentas activas" value={loading ? '—' : users.filter(u => u.active).length} icon="check" tone="mint" /><Stat label="Conductores" value={loading ? '—' : users.filter(u => u.role === 'CONDUCTOR').length} icon="box" /></div>
      <section className="team-create"><div><p className="eyebrow">CRECER JUNTOS</p><h2>Una persona más.<br />Un equipo más fuerte.</h2><p>Crea una cuenta y asigna su rol.</p></div><div>

      <form className="inline-form" onSubmit={handleCreate}>
        <label>Usuario<input
          placeholder="Usuario"
          value={form.username}
          onChange={(e) => setForm({ ...form, username: e.target.value })}
          required
        /></label>
        <label>Nombre completo<input
          placeholder="Nombre completo"
          value={form.fullName}
          onChange={(e) => setForm({ ...form, fullName: e.target.value })}
          required
        /></label>
        <label>Contraseña<input
          type="password"
          minLength={6}
          autoComplete="new-password"
          placeholder="Contrasena"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
          required
        /></label>
        <label>Rol<select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
          <option value="CONDUCTOR">Conductor</option>
          <option value="ADMIN">Admin</option>
        </select></label>
        <button type="submit">Agregar al equipo</button>
      </form>

      {error && <p className="error-text">{error}</p>}
      </div></section>
      <div className="section-heading"><h2>Personas y accesos</h2><span>{users.length} cuentas</span></div>

      {loading ? (
        <TableSkeleton rows={4} columns={5} widths={['50%', '65%', '40%', '35%', '60%']} />
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Usuario</th>
              <th>Nombre</th>
              <th>Rol</th>
              <th>Estado</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td className="mono">{u.username}</td>
                <td>{u.fullName}</td>
                <td>{u.role}</td>
                <td>
                  <span className={`status-pill ${u.active ? 'success' : 'error'}`}>
                    {u.active ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="actions-cell">
                  <button className="link-button" onClick={() => toggleActive(u)}>
                    {u.active ? 'Desactivar' : 'Activar'}
                  </button>
                  <button className="link-button danger" onClick={() => removeUser(u)}>
                    Eliminar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
