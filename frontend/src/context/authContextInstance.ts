import { createContext } from 'react'
import type { User } from '../types/domain'

export interface AuthContextValue {
  user: User | null
  login: (username: string, password: string, remember?: boolean) => Promise<User>
  logout: () => Promise<void>
}

/**
 * El objeto de contexto vive en su propio archivo (RA4,
 * docs/EVALUACION_TECNICA.md §18.3/§21/§22): antes AuthContext.tsx exportaba, ademas del
 * componente AuthProvider, el hook useAuth, lo que dispara el warning de lint
 * react/only-export-components (Fast Refresh solo funciona si un archivo solo exporta
 * componentes).
 */
export const AuthContext = createContext<AuthContextValue | null>(null)
