import { useState, type FormEvent } from 'react'
import apiClient from '../../../api/client'
import TableSkeleton from '../../../components/TableSkeleton'
import { Stat } from '../../../components/Workspace'
import { useAsyncData } from '../../../hooks/useAsyncData'
import { getErrorMessage } from '../../../utils/errors'
import type { OperationalCost } from '../../../types/domain'

function currentMonth(): string {
  return new Date().toISOString().slice(0, 7) // yyyy-MM
}

/** Costo por entrega verificada (showback, Fase1 §4.5) y edicion de horas de soporte. */
export function CostSection() {
  const [costMonth, setCostMonth] = useState(currentMonth)
  const [supportHoursInput, setSupportHoursInput] = useState('')
  const [savingHours, setSavingHours] = useState(false)
  const [manualCostError, setManualCostError] = useState('')

  const {
    data: cost = null,
    loading: costLoading,
    error: costError,
    setData: setCost,
  } = useAsyncData<OperationalCost | null>(() =>
    apiClient.get<OperationalCost>('/admin/cost', { params: { month: costMonth } }).then((res) => {
      setSupportHoursInput(String(res.data.supportHours))
      return res.data
    }), [costMonth])

  const saveSupportHours = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setSavingHours(true)
    setManualCostError('')
    try {
      const { data } = await apiClient.put<OperationalCost>('/admin/cost/support-hours', {
        month: costMonth,
        hours: Number(supportHoursInput),
      })
      setCost(data)
    } catch (err: unknown) {
      setManualCostError(getErrorMessage(err, 'No se pudieron guardar las horas de soporte.'))
    } finally {
      setSavingHours(false)
    }
  }

  return (
    <>
      <div className="section-heading">
        <h2>Costo por entrega verificada</h2>
        <span>Reporte informativo (showback) · no genera cargos contables</span>
      </div>

      <div className="inline-form">
        <label>Mes<input type="month" value={costMonth} onChange={(e) => setCostMonth(e.target.value)} /></label>
      </div>

      {(costError || manualCostError) && <p role="alert" className="error-text">{costError || manualCostError}</p>}

      {costLoading ? (
        <TableSkeleton rows={2} columns={4} />
      ) : cost && (
        <>
          <div className="stats-grid four">
            <Stat label="Entregas confirmadas" value={cost.confirmedDeliveries} icon="check" tone="mint" />
            <Stat label="Evidencia almacenada" value={`${cost.evidenceStorageGb.toFixed(2)} GB`} icon="box" />
            <Stat label="Costo total del mes" value={`USD ${cost.totalCostUsd.toFixed(2)}`} icon="shield" />
            <Stat
              label="Costo por entrega"
              value={cost.costPerDeliveryUsd != null ? `USD ${cost.costPerDeliveryUsd.toFixed(2)}` : '—'}
              icon="pin"
              tone="peach"
            />
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Componente</th>
                <th>Costo mensual (USD)</th>
              </tr>
            </thead>
            <tbody>
              <tr><td>Infraestructura</td><td>{cost.infrastructureCostUsd.toFixed(2)}</td></tr>
              <tr><td>Almacenamiento de evidencia</td><td>{cost.storageCostUsd.toFixed(2)}</td></tr>
              <tr><td>Soporte y mantenimiento</td><td>{cost.supportCostUsd.toFixed(2)}</td></tr>
              <tr><td><strong>Total</strong></td><td><strong>{cost.totalCostUsd.toFixed(2)}</strong></td></tr>
            </tbody>
          </table>

          <form className="inline-form" onSubmit={saveSupportHours}>
            <label>
              Horas de soporte de {costMonth}
              <input
                type="number"
                min="0"
                step="0.5"
                value={supportHoursInput}
                onChange={(e) => setSupportHoursInput(e.target.value)}
              />
            </label>
            <button type="submit" disabled={savingHours}>{savingHours ? 'Guardando…' : 'Guardar horas'}</button>
          </form>

          <p className="hint-text">
            Entregas confirmadas y GB de evidencia son datos reales del sistema. Infraestructura, almacenamiento y soporte
            usan tarifas configuradas por variable de entorno (0 si no se configuran); las horas de soporte se registran
            manualmente porque varían cada mes.
          </p>
        </>
      )}
    </>
  )
}
