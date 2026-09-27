import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiClient from '../../api/client'
import AdminDriversPage from './AdminDriversPage'
import type { Driver, PageResponse } from '../../types/domain'

// Respuesta minima pero tipada (sin "as any"): getErrorMessage solo lee err.response?.data,
// pero AxiosError exige un AxiosResponse completo -- se completa el resto con valores
// neutros en vez de deshabilitar el chequeo de tipos con un cast generico.
function axiosErrorWithMessage(message: string): AxiosError {
  const response: AxiosResponse<{ message: string }> = {
    data: { message },
    status: 401,
    statusText: 'Unauthorized',
    headers: {},
    config: {} as InternalAxiosRequestConfig,
  }
  return new AxiosError(message, undefined, undefined, undefined, response)
}

vi.mock('../../api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

const mockedApiClient = vi.mocked(apiClient, { deep: true })

const activeUser: Driver = { id: 1, username: 'conductor1', fullName: 'Conductor Uno', role: 'CONDUCTOR', active: true }
const onePage = (content: Driver[]): PageResponse<Driver> => ({ content, page: 0, size: 100, totalElements: content.length, totalPages: 1 })

describe('AdminDriversPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra un mensaje de error si falla la carga del equipo', async () => {
    mockedApiClient.get.mockRejectedValueOnce(axiosErrorWithMessage('Sesion expirada'))

    render(<AdminDriversPage />)

    expect(await screen.findByText('Sesion expirada')).toBeInTheDocument()
  })

  it('muestra el equipo cuando la carga es exitosa', async () => {
    mockedApiClient.get.mockResolvedValueOnce({ data: onePage([activeUser]) } as AxiosResponse<PageResponse<Driver>>)

    render(<AdminDriversPage />)

    expect(await screen.findByText('conductor1')).toBeInTheDocument()
  })

  it('muestra un error si falla desactivar un usuario, sin romper la pagina', async () => {
    mockedApiClient.get.mockResolvedValue({ data: onePage([activeUser]) } as AxiosResponse<PageResponse<Driver>>)
    mockedApiClient.put.mockRejectedValueOnce(axiosErrorWithMessage('No puedes desactivar o eliminar tu propia cuenta.'))

    render(<AdminDriversPage />)
    await screen.findByText('conductor1')

    await userEvent.click(screen.getByRole('button', { name: 'Desactivar' }))

    expect(await screen.findByText('No puedes desactivar o eliminar tu propia cuenta.')).toBeInTheDocument()
  })
})
