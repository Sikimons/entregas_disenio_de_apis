import { useMemo, useState, type ReactNode } from 'react'
import apiClient from '../api/client'
import type { User } from '../types/domain'
import { AuthContext } from './authContextInstance'

interface LoginResponse extends User {
  token: string
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const stored = localStorage.getItem('user') || sessionStorage.getItem('user')
    return stored ? (JSON.parse(stored) as User) : null
  })

  // "Recuerdame" marcado -> localStorage (sobrevive a cerrar el navegador).
  // Sin marcar -> sessionStorage (se olvida al cerrar la pestana/navegador).
  const login = async (username: string, password: string, remember = true): Promise<User> => {
    const { data } = await apiClient.post<LoginResponse>('/auth/login', { username, password })
    const storage = remember ? localStorage : sessionStorage
    storage.setItem('token', data.token)
    const loggedUser: User = { username: data.username, fullName: data.fullName, role: data.role }
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
