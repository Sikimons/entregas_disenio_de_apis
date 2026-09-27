import axios from 'axios'

/**
 * Extrae un mensaje legible de un error atrapado como `unknown` (RA4,
 * docs/EVALUACION_TECNICA.md §18.3/§21/§22): antes cada catch usaba `any` sin tipar,
 * anulando la proteccion de "strict" de TypeScript. Cubre los tres tipos de error reales
 * del proyecto: axios (con {message} del backend), Error de JS (OCR, compresion de
 * imagen) y GeolocationPositionError (tiene `.message` pero no es `instanceof Error`).
 */
export function getErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined
    return data?.message || fallback
  }
  if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
    return (err as { message: string }).message || fallback
  }
  return fallback
}
