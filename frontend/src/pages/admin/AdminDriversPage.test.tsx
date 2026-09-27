import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiClient from '../../api/client'
import AdminDriversPage from './AdminDriversPage'
import type { Driver, PageResponse } from '../../types/domain'

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
    mockedApiClient.get.mockRejectedValueOnce(
      new AxiosError('Sesion expirada', undefined, undefined, undefined, { data: { message: 'Sesion expirada' } } as any)
    )

    render(<AdminDriversPage />)

    expect(await screen.findByText('Sesion expirada')).toBeInTheDocument()
  })

  it('muestra el equipo cuando la carga es exitosa', async () => {
    mockedApiClient.get.mockResolvedValueOnce({ data: onePage([activeUser]) } as any)

    render(<AdminDriversPage />)

    expect(await screen.findByText('conductor1')).toBeInTheDocument()
  })

  it('muestra un error si falla desactivar un usuario, sin romper la pagina', async () => {
    mockedApiClient.get.mockResolvedValue({ data: onePage([activeUser]) } as any)
    mockedApiClient.put.mockRejectedValueOnce(
      new AxiosError(
        'No puedes desactivar o eliminar tu propia cuenta.',
        undefined,
        undefined,
        undefined,
        { data: { message: 'No puedes desactivar o eliminar tu propia cuenta.' } } as any
      )
    )

    render(<AdminDriversPage />)
    await screen.findByText('conductor1')

    await userEvent.click(screen.getByRole('button', { name: 'Desactivar' }))

    expect(await screen.findByText('No puedes desactivar o eliminar tu propia cuenta.')).toBeInTheDocument()
  })
})
