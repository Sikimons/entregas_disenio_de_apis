import { useState } from 'react'
import apiClient from '../../../api/client'
import TableSkeleton from '../../../components/TableSkeleton'
import { Stat } from '../../../components/Workspace'
import { useAsyncData } from '../../../hooks/useAsyncData'
import { getErrorMessage } from '../../../utils/errors'
import type { CircuitBreakerStatus } from '../../../types/domain'

/** Estado del Circuit Breaker + Retry (Fase1 §3/§5.1) y simulacion de fallos del ERP. */
export function ResilienceSection() {
  const [simulating, setSimulating] = useState(false)
  const [manualBreakerError, setManualBreakerError] = useState('')

  const {
    data: breaker = null,
    loading: breakerLoading,
    error: breakerError,
    setData: setBreaker,
    reload: reloadBreaker,
  } = useAsyncData<CircuitBreakerStatus | null>(
    () => apiClient.get<CircuitBreakerStatus>('/admin/resilience/status').then((res) => res.data),
    []
  )

  const simulateFailures = async (count: number) => {
    setSimulating(true)
    setManualBreakerError('')
    try {
      const { data } = await apiClient.post<CircuitBreakerStatus>('/admin/resilience/simulate-failures', { count })
      setBreaker(data)
    } catch (err: unknown) {
      setManualBreakerError(getErrorMessage(err, 'No se pudieron simular los fallos.'))
    } finally {
      setSimulating(false)
    }
  }

  return (
    <>
      <div className="section-heading">
        <h2>Resiliencia (Circuit Breaker + Retry)</h2>
        <span>Protegen la simulación del ERP (Fase1 §3/§5.1)</span>
      </div>

      {(breakerError || manualBreakerError) && <p role="alert" className="error-text">{breakerError || manualBreakerError}</p>}

      {breakerLoading ? (
        <TableSkeleton rows={1} columns={4} />
      ) : breaker && (
        <>
          <div className="stats-grid four">
            <Stat
              label="Estado del breaker"
              value={breaker.state}
              icon="shield"
              tone={breaker.state === 'OPEN' ? 'peach' : breaker.state === 'HALF_OPEN' ? 'peach' : 'mint'}
            />
            <Stat label="Activado" value={breaker.enabled ? 'Sí' : 'No'} icon="check" />
            <Stat label="Fallos simulados pendientes" value={breaker.pendingSimulatedFailures} icon="pin" />
            <Stat label="Tasa de fallo" value={breaker.failureRate >= 0 ? `${breaker.failureRate.toFixed(0)}%` : '—'} icon="box" />
          </div>

          <div className="stats-grid four">
            <Stat label="Retry activado" value={breaker.retryEnabled ? 'Sí' : 'No'} icon="check" />
            <Stat label="Éxitos sin reintentar" value={breaker.retrySuccessfulCallsWithoutRetry} icon="box" />
            <Stat label="Éxitos tras reintentar" value={breaker.retrySuccessfulCallsWithRetry} icon="pin" />
            <Stat label="Fallos incluso reintentando" value={breaker.retryFailedCallsWithRetry} icon="pin" />
          </div>

          <div className="inline-form">
            <button type="button" disabled={simulating} onClick={() => simulateFailures(2)}>
              {simulating ? 'Simulando…' : 'Simular 2 fallos (Retry los absorbe)'}
            </button>
            <button type="button" disabled={simulating} onClick={() => simulateFailures(6)}>
              {simulating ? 'Simulando…' : 'Simular 6 fallos (abre el Circuit Breaker)'}
            </button>
            <button type="button" disabled={breakerLoading} onClick={reloadBreaker}>Actualizar estado</button>
          </div>

          <p className="hint-text">
            Con pocos fallos simulados (2, menos que los reintentos configurados), el Retry los absorbe: la búsqueda de
            facturas del conductor tiene éxito igual, sin que quien llama note nada, y el breaker ni se entera. Con una
            racha más larga (6), el Retry también reintenta pero el breaker sigue contando los fallos y termina abriéndose:
            busca una factura desde la pantalla del conductor varias veces para verlo pasar a <strong>OPEN</strong>; tras
            unos segundos sin llamadas pasa a <strong>HALF_OPEN</strong> y, si las siguientes llamadas tienen éxito, vuelve
            a <strong>CLOSED</strong>.
          </p>
        </>
      )}
    </>
  )
}
