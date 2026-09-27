import { beforeEach, describe, expect, it } from 'vitest'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'
import apiClient from './client'

// axios expone los interceptores registrados en .interceptors.<tipo>.handlers[i], pero
// ese array no forma parte de los tipos publicos (@types de axios); se define aqui una
// interfaz minima solo para el shape que se necesita, y se castea puntualmente.
interface InterceptorHandlers<V> {
  handlers: Array<{
    fulfilled: (value: V) => V | Promise<V>
    rejected: (error: AxiosError) => unknown
  } | null>
}

function requestFulfilled(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  const handlers = (apiClient.interceptors.request as unknown as InterceptorHandlers<InternalAxiosRequestConfig>).handlers
  // El interceptor real es sincrono (nunca devuelve una Promise); el cast solo
  // refleja eso, no cambia el comportamiento probado.
  return handlers[0]!.fulfilled(config) as InternalAxiosRequestConfig
}

function responseRejected(error: AxiosError): unknown {
  const handlers = (apiClient.interceptors.response as unknown as InterceptorHandlers<unknown>).handlers
  return handlers[0]!.rejected(error)
}

function setLocation(pathname: string) {
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: { pathname, href: '' },
  })
}

describe('apiClient', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    setLocation('/admin/dashboard')
  })

  it('adjunta el token Bearer cuando hay uno en localStorage', () => {
    localStorage.setItem('token', 'abc123')

    const config = requestFulfilled({ headers: {} } as InternalAxiosRequestConfig)

    expect(config.headers.Authorization).toBe('Bearer abc123')
  })

  it('adjunta el token Bearer cuando hay uno en sessionStorage', () => {
    sessionStorage.setItem('token', 'xyz789')

    const config = requestFulfilled({ headers: {} } as InternalAxiosRequestConfig)

    expect(config.headers.Authorization).toBe('Bearer xyz789')
  })

  it('no agrega Authorization si no hay token guardado', () => {
    const config = requestFulfilled({ headers: {} } as InternalAxiosRequestConfig)

    expect(config.headers.Authorization).toBeUndefined()
  })

  it('ante un 401 limpia la sesion y redirige a /login', async () => {
    localStorage.setItem('token', 'abc123')
    localStorage.setItem('user', '{"username":"admin"}')

    await expect(responseRejected({ response: { status: 401 } } as AxiosError)).rejects.toBeDefined()

    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
    expect(window.location.href).toBe('/login')
  })

  it('ante un 401 ya estando en /login, no redirige de nuevo', async () => {
    setLocation('/login')

    await expect(responseRejected({ response: { status: 401 } } as AxiosError)).rejects.toBeDefined()

    expect(window.location.href).toBe('')
  })

  it('ante otros codigos de error no toca la sesion', async () => {
    localStorage.setItem('token', 'abc123')

    await expect(responseRejected({ response: { status: 500 } } as AxiosError)).rejects.toBeDefined()

    expect(localStorage.getItem('token')).toBe('abc123')
    expect(window.location.href).toBe('')
  })
})
