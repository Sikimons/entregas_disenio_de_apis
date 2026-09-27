import { useState, type FormEvent } from 'react'
import apiClient from '../../api/client'
import TableSkeleton from '../../components/TableSkeleton'
import { PageIntro, Stat } from '../../components/Workspace'
import { useAsyncData } from '../../hooks/useAsyncData'
import { getErrorMessage } from '../../utils/errors'
import type { Driver, PageResponse, Role } from '../../types/domain'

interface DriverForm {
  username: string
  password: string
  fullName: string
  role: Role
}

const emptyForm: DriverForm = { username: '', password: '', fullName: '', role: 'CONDUCTOR' }
const PAGE_SIZE = 100
const EMPTY_PAGE: PageResponse<Driver> = { content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 }

export default function AdminDriversPage() {
  const [page, setPage] = useState(0)
  const [form, setForm] = useState<DriverForm>(emptyForm)
  const [actionError, setActionError] = useState('')

  const { data = EMPTY_PAGE, loading, error, reload } = useAsyncData(
    () => apiClient.get<PageResponse<Driver>>('/admin/users', { params: { page, size: PAGE_SIZE } }).then((res) => res.data),
    [page]
  )
  const users = data.content

  const handleCreate = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setActionError('')
    try {
      await apiClient.post('/admin/users', form)
      setForm(emptyForm)
      reload()
    } catch (err: unknown) {
      setActionError(getErrorMessage(err, 'No se pudo crear el usuario.'))
    }
  }

  const toggleActive = async (user: Driver) => {
    setActionError('')
    try {
      await apiClient.put(`/admin/users/${user.id}`, {
        fullName: user.fullName,
        active: !user.active,
        password: null,
      })
      reload()
    } catch (err: unknown) {
      setActionError(getErrorMessage(err, 'No se pudo actualizar el estado del usuario.'))
    }
  }

  const removeUser = async (user: Driver) => {
    if (!window.confirm(`¿Eliminar al usuario ${user.username}?`)) return
    setActionError('')
    try {
      await apiClient.delete(`/admin/users/${user.id}`)
      reload()
    } catch (err: unknown) {
      setActionError(getErrorMessage(err, 'No se pudo eliminar el usuario.'))
    }
  }

  return (
    <div>
      <PageIntro eyebrow="LAS PERSONAS DETRÁS DE CADA ENTREGA" title="Tu equipo" description="Gestiona las cuentas y los accesos de tu operación." />
      {/* "Personas en el equipo" usa totalElements (correcto sin importar la pagina); las otras
          dos son exactas mientras el equipo quepa en una pagina (PAGE_SIZE=100, el tope del
          dominio) -- un trade-off aceptado para no requerir un endpoint de agregados aparte. */}
      <div className="stats-grid"><Stat label="Personas en el equipo" value={loading ? '—' : data.totalElements} icon="users" /><Stat label="Cuentas activas" value={loading ? '—' : users.filter(u => u.active).length} icon="check" tone="mint" /><Stat label="Conductores" value={loading ? '—' : users.filter(u => u.role === 'CONDUCTOR').length} icon="box" /></div>
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

      {(error || actionError) && <p role="alert" className="error-text">{error || actionError}</p>}
      </div></section>
      <div className="section-heading"><h2>Personas y accesos</h2><span>{data.totalElements} cuentas</span></div>

      {loading ? (
        <TableSkeleton rows={4} columns={5} widths={['50%', '65%', '40%', '35%', '60%']} />
      ) : (
        <>
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

          {data.totalPages > 1 && (
            <div className="pagination">
              <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Anterior</button>
              <span>Pagina {page + 1} de {Math.max(data.totalPages, 1)}</span>
              <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>Siguiente</button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
