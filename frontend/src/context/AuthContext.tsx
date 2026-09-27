import { useMemo, useState, type ReactNode } from 'react'
import apiClient from '../api/client'
import type { User } from '../types/domain'
import { AuthContext } from './authContextInstance'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const stored = localStorage.getItem('user') || sessionStorage.getItem('user')
    return stored ? (JSON.parse(stored) as User) : null
  })

  // "Recuerdame" marcado -> el backend emite la cookie de sesion con expiracion (sobrevive
  // a cerrar el navegador) y aqui se refleja en localStorage; sin marcar -> cookie de
  // sesion pura (AuthController.setAuthCookie) y sessionStorage. El backend decide la
  // duracion real de la cookie (auditoria tecnica, hallazgo P1: el JWT ya no lo maneja
  // este JS, solo el perfil visible para la UI).
  const login = async (username: string, password: string, remember = true): Promise<User> => {
    const { data } = await apiClient.post<User>('/auth/login', { username, password, remember })
    const storage = remember ? localStorage : sessionStorage
    const loggedUser: User = { username: data.username, fullName: data.fullName, role: data.role }
    storage.setItem('user', JSON.stringify(loggedUser))
    setUser(loggedUser)
    return loggedUser
  }

  const logout = async () => {
    try {
      // Limpia la cookie HttpOnly del lado del servidor; este JS no puede borrarla por su
      // cuenta. Si la llamada falla (red caida), se limpia igual el estado local: no tiene
      // sentido dejar a alguien "atrapado" logueado en la UI por un error de red al salir.
      await apiClient.post('/auth/logout')
    } catch {
      // intencional: ver comentario arriba
    }
    localStorage.removeItem('user')
    sessionStorage.removeItem('user')
    setUser(null)
  }

  const value = useMemo(() => ({ user, login, logout }), [user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
