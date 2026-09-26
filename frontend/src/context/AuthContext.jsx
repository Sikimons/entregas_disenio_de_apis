import { createContext, useContext, useMemo, useState } from 'react'
import apiClient from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user') || sessionStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  // "Recuerdame" marcado -> localStorage (sobrevive a cerrar el navegador).
  // Sin marcar -> sessionStorage (se olvida al cerrar la pestana/navegador).
  const login = async (username, password, remember = true) => {
    const { data } = await apiClient.post('/auth/login', { username, password })
    const storage = remember ? localStorage : sessionStorage
    storage.setItem('token', data.token)
    const loggedUser = { username: data.username, fullName: data.fullName, role: data.role }
    storage.setItem('user', JSON.stringify(loggedUser))
    setUser(loggedUser)
    return loggedUser
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user')
    setUser(null)
  }

  const value = useMemo(() => ({ user, login, logout }), [user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth debe usarse dentro de AuthProvider')
  }
  return ctx
}
