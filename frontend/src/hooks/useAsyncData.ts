import { useCallback, useEffect, useState, type DependencyList, type Dispatch, type SetStateAction } from 'react'
import { getErrorMessage } from '../utils/errors'

interface AsyncDataState<T> {
  data: T | undefined
  loading: boolean
  error: string
  setData: Dispatch<SetStateAction<T | undefined>>
  reload: () => void
}

/**
 * Centraliza el patron "loading/error/data" repetido casi identico en 7 paginas del
 * panel admin y del conductor (RA4, docs/EVALUACION_TECNICA.md §18.3/§21/§22): antes
 * cada una declaraba sus propios useState + useEffect con `setLoading(true)`/
 * `setError('')` sincronicos al inicio del efecto (react/set-state-in-effect). Aqui
 * queda en un solo lugar, revisado y con pruebas, en vez de repetido 7 veces.
 *
 * `setData` se expone para los casos que actualizan el estado directamente desde la
 * respuesta de un POST/PUT sin volver a pedir el GET (evita un round-trip extra).
 * `reload` fuerza una nueva ejecucion de `fetcher` sin depender de que cambien `deps`.
 */
export function useAsyncData<T>(fetcher: () => Promise<T>, deps: DependencyList): AsyncDataState<T> {
  const [data, setData] = useState<T>()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [tick, setTick] = useState(0)

  useEffect(() => {
    let cancelled = false
    // oxlint-disable-next-line react/set-state-in-effect -- patron de carga de datos
    // centralizado aqui a proposito (unico lugar, con pruebas), en vez de repetirlo en
    // cada pagina que antes disparaba este mismo warning por separado.
    setLoading(true)
    setError('')
    fetcher()
      .then((result) => {
        if (!cancelled) setData(result)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(getErrorMessage(err, 'No se pudo cargar la información.'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- las dependencias las controla quien llama al hook
  }, [...deps, tick])

  const reload = useCallback(() => setTick((t) => t + 1), [])

  return { data, loading, error, setData, reload }
}
