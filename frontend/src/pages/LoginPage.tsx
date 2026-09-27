import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Brand, Icon } from '../components/Workspace'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [visible, setVisible] = useState(false)
  const [remember, setRemember] = useState(true)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const loggedUser = await login(username, password, remember)
      navigate(loggedUser.role === 'ADMIN' ? '/admin' : '/driver', { replace: true })
    } catch (err: any) {
      setError(err.response?.data?.message || 'No se pudo iniciar sesion.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-screen">
      <section className="auth-story" aria-label="Ruta, gestión de entregas">
        <Brand />
        <div className="auth-headline"><p className="eyebrow">DEL PRIMER PASO AL ÚLTIMO KILÓMETRO</p><h1>Todo listo.<br />Vamos a <em>entregar.</em></h1><p>Tu equipo, tus pedidos y cada confirmación.<br />Todo conectado en una misma ruta.</p></div>
        <div className="route-art" aria-hidden="true"><svg className="route-lines" viewBox="0 0 560 260"><path d="M0 65H145Q175 65 175 95V175Q175 205 205 205H340Q375 205 375 170V95Q375 65 405 65H560" fill="none" stroke="currentColor" strokeWidth="2" strokeDasharray="7 7" /><circle cx="90" cy="65" r="9" fill="currentColor" /><circle cx="460" cy="65" r="9" fill="currentColor" /></svg><div className="route-package"><span className="route-package-icon"><Icon name="box" size={50} /></span><div><small>DE PUERTA A PUERTA</small><strong>Una entrega.<br />Una buena experiencia.</strong></div></div><div className="route-confirm"><span><Icon name="check" /></span><div><strong>En buenas manos</strong><small>Verificación con PIN</small></div></div><span className="route-point route-start"><Icon name="box" />Origen</span><span className="route-point route-end"><Icon name="pin" />Destino</span></div>
        <footer className="auth-story-footer"><span>Menos pasos. Más entregas.</span><span>01 / RUTA</span></footer>
      </section>
      <section className="auth-entry">
      <span className="auth-entry-label"><Icon name="shield" size={16} /> ACCESO AL EQUIPO</span>
      <form className="auth-card" onSubmit={handleSubmit}>
        <p className="eyebrow">QUÉ BUENO VERTE</p><h2>Tu jornada<br />empieza aquí.</h2>
        <p className="auth-subtitle">Ingresa con tu cuenta para continuar.</p>

        <label htmlFor="username">Usuario</label>
        <input
          id="username"
          placeholder="Tu nombre de usuario"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
          required
        />

        <label htmlFor="password">Contraseña</label>
        <div className="password-field">
        <input
          id="password"
          type={visible ? 'text' : 'password'}
          placeholder="Tu contraseña"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          required
        />
        <button type="button" className="icon-button" onClick={() => setVisible(!visible)} aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'} aria-pressed={visible}><Icon name="eye" /></button>
        </div>

        <label htmlFor="remember" className="auth-remember">
          <input
            id="remember"
            type="checkbox"
            checked={remember}
            onChange={(e) => setRemember(e.target.checked)}
          />
          Mantener mi sesión
        </label>

        {error && <p className="error-text" role="alert">{error}</p>}

        <button className="auth-submit" type="submit" disabled={loading}>
          {loading ? 'Ingresando…' : 'Entrar a mi espacio'}<Icon name="arrow" />
        </button>
        <p className="auth-help">¿Necesitas una cuenta? Contacta a tu administrador.</p>
      </form>
      <footer className="auth-entry-footer">Ruta · Gestión de entregas</footer>
      </section>
    </main>
  )
}
