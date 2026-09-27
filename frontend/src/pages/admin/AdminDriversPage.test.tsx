import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiClient from '../../api/client'
import AdminDriversPage from './AdminDriversPage'
import type { Driver } from '../../types/domain'

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

describe('AdminDriversPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra un mensaje de error si falla la carga del equipo', async () => {
    mockedApiClient.get.mockRejectedValueOnce({ response: { data: { message: 'Sesion expirada' } } })

    render(<AdminDriversPage />)

    expect(await screen.findByText('Sesion expirada')).toBeInTheDocument()
  })

  it('muestra el equipo cuando la carga es exitosa', async () => {
    mockedApiClient.get.mockResolvedValueOnce({ data: [activeUser] } as any)

    render(<AdminDriversPage />)

    expect(await screen.findByText('conductor1')).toBeInTheDocument()
  })

  it('muestra un error si falla desactivar un usuario, sin romper la pagina', async () => {
    mockedApiClient.get.mockResolvedValue({ data: [activeUser] } as any)
    mockedApiClient.put.mockRejectedValueOnce({ response: { data: { message: 'No puedes desactivar o eliminar tu propia cuenta.' } } })

    render(<AdminDriversPage />)
    await screen.findByText('conductor1')

    await userEvent.click(screen.getByRole('button', { name: 'Desactivar' }))

    expect(await screen.findByText('No puedes desactivar o eliminar tu propia cuenta.')).toBeInTheDocument()
  })
})
