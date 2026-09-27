import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import apiClient from '../../api/client'
import TableSkeleton from '../../components/TableSkeleton'
import { PageIntro, Stat } from '../../components/Workspace'
import type { CircuitBreakerStatus, DeliveryAttempt, DriverMetric, OperationalCost } from '../../types/domain'

interface RangePreset {
  key: string
  label: string
  days: number
}

const RANGE_PRESETS: RangePreset[] = [
  { key: 'today', label: 'Hoy', days: 0 },
  { key: '7d', label: 'Ultimos 7 dias', days: 7 },
  { key: '30d', label: 'Ultimos 30 dias', days: 30 },
]

const OUTCOME_COLORS: Record<string, string> = {
  CONFIRMED: '#3fbf7f',
  INCIDENT: '#f2a93b',
  REJECTED: '#d7392b',
}

const OUTCOME_LABELS: Record<string, string> = {
  CONFIRMED: 'Entregado',
  INCIDENT: 'Incidencia',
  REJECTED: 'Rechazado',
}

function startOfDay(date: Date): Date {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  return d
}

function rangeFor(preset: RangePreset): { from: string; to: string } {
  const to = new Date()
  const from = preset.days === 0 ? startOfDay(to) : new Date(to.getTime() - preset.days * 86400000)
  return { from: from.toISOString(), to: to.toISOString() }
}

function currentMonth(): string {
  return new Date().toISOString().slice(0, 7) // yyyy-MM
}

// Construye el contenido del popup con nodos DOM y textContent (nunca innerHTML/template
// string): point.detail viene de texto libre que escribe el conductor (reporte de
// incidencia) y no debe poder inyectar HTML/JS en la sesion del administrador.
function buildPopupContent(point: DeliveryAttempt): HTMLElement {
  const container = document.createElement('div')
  const title = document.createElement('strong')
  title.textContent = OUTCOME_LABELS[point.outcome] || point.outcome
  container.appendChild(title)
  container.appendChild(document.createElement('br'))
  container.appendChild(document.createTextNode(point.invoiceNumber))
  container.appendChild(document.createElement('br'))
  container.appendChild(document.createTextNode(point.driverName))
  container.appendChild(document.createElement('br'))
  if (point.detail) {
    container.appendChild(document.createTextNode(point.detail))
    container.appendChild(document.createElement('br'))
  }
  container.appendChild(document.createTextNode(new Date(point.createdAt).toLocaleString()))
  return container
}

export default function AdminDashboardPage() {
  const [presetKey, setPresetKey] = useState('today')
  const [points, setPoints] = useState<DeliveryAttempt[]>([])
  const [metrics, setMetrics] = useState<DriverMetric[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [costMonth, setCostMonth] = useState(currentMonth)
  const [cost, setCost] = useState<OperationalCost | null>(null)
  const [costLoading, setCostLoading] = useState(true)
  const [costError, setCostError] = useState('')
  const [supportHoursInput, setSupportHoursInput] = useState('')
  const [savingHours, setSavingHours] = useState(false)

  const [breaker, setBreaker] = useState<CircuitBreakerStatus | null>(null)
  const [breakerError, setBreakerError] = useState('')
  const [breakerLoading, setBreakerLoading] = useState(true)
  const [simulating, setSimulating] = useState(false)

  const mapContainerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<L.Map | null>(null)
  const markersLayerRef = useRef<L.LayerGroup | null>(null)

  const preset = useMemo(() => RANGE_PRESETS.find((p) => p.key === presetKey)!, [presetKey])

  useEffect(() => {
    const { from, to } = rangeFor(preset)
    setLoading(true)
    setError('')
    Promise.all([
      apiClient.get<DeliveryAttempt[]>('/admin/dashboard/map', { params: { from, to } }),
      apiClient.get<DriverMetric[]>('/admin/dashboard/metrics', { params: { from, to } }),
    ])
      .then(([mapRes, metricsRes]) => {
        setPoints(mapRes.data)
        setMetrics(metricsRes.data)
      })
      .catch((err) => setError(err.response?.data?.message || 'No se pudo cargar el tablero.'))
      .finally(() => setLoading(false))
  }, [preset])

  const loadCost = () => {
    setCostLoading(true)
    setCostError('')
    apiClient.get<OperationalCost>('/admin/cost', { params: { month: costMonth } })
      .then((res) => {
        setCost(res.data)
        setSupportHoursInput(String(res.data.supportHours))
      })
      .catch((err) => setCostError(err.response?.data?.message || 'No se pudo cargar el costo por entrega.'))
      .finally(() => setCostLoading(false))
  }

  useEffect(loadCost, [costMonth])

  const loadBreakerStatus = () => {
    setBreakerLoading(true)
    setBreakerError('')
    apiClient.get<CircuitBreakerStatus>('/admin/resilience/status')
      .then((res) => setBreaker(res.data))
      .catch((err) => setBreakerError(err.response?.data?.message || 'No se pudo consultar el circuit breaker.'))
      .finally(() => setBreakerLoading(false))
  }

  useEffect(loadBreakerStatus, [])

  const simulateFailures = async (count: number) => {
    setSimulating(true)
    setBreakerError('')
    try {
      const { data } = await apiClient.post<CircuitBreakerStatus>('/admin/resilience/simulate-failures', { count })
      setBreaker(data)
    } catch (err: any) {
      setBreakerError(err.response?.data?.message || 'No se pudieron simular los fallos.')
    } finally {
      setSimulating(false)
    }
  }

  const saveSupportHours = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setSavingHours(true)
    setCostError('')
    try {
      const { data } = await apiClient.put<OperationalCost>('/admin/cost/support-hours', {
        month: costMonth,
        hours: Number(supportHoursInput),
      })
      setCost(data)
    } catch (err: any) {
      setCostError(err.response?.data?.message || 'No se pudieron guardar las horas de soporte.')
    } finally {
      setSavingHours(false)
    }
  }

  // Inicializa el mapa una sola vez.
  useEffect(() => {
    if (mapRef.current || !mapContainerRef.current) return
    const map = L.map(mapContainerRef.current).setView([-1.8312, -78.1834], 6) // Ecuador
    // Tiles servidos same-origin via /map-tiles/ (proxy de nginx) por defecto: algunas
    // redes moviles/corporativas bloquean CDNs de terceros directo. En despliegues sin
    // ese proxy (p. ej. Cloudflare Pages), VITE_MAP_TILES_URL apunta directo al proveedor.
    L.tileLayer(import.meta.env.VITE_MAP_TILES_URL || '/map-tiles/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(map)
    markersLayerRef.current = L.layerGroup().addTo(map)
    mapRef.current = map

    return () => {
      map.remove()
      mapRef.current = null
    }
  }, [])

  // Redibuja los marcadores cuando cambian los puntos.
  useEffect(() => {
    const map = mapRef.current
    const layer = markersLayerRef.current
    if (!map || !layer) return

    layer.clearLayers()

    const latLngs: [number, number][] = []
    points.forEach((point) => {
      if (point.latitude == null || point.longitude == null) return
      const color = OUTCOME_COLORS[point.outcome] || '#888'
      const marker = L.circleMarker([point.latitude, point.longitude], {
        radius: 8,
        color,
        fillColor: color,
        fillOpacity: 0.85,
        weight: 2,
      })
      marker.bindPopup(buildPopupContent(point))
      marker.addTo(layer)
      latLngs.push([point.latitude, point.longitude])
    })

    if (latLngs.length > 0) {
      map.fitBounds(latLngs, { padding: [40, 40], maxZoom: 15 })
    }
  }, [points])

  return (
    <div>
      <PageIntro eyebrow="CENTRO DE OPERACIONES" title="Todo en perspectiva." description="Sigue la actividad de tu equipo y cada entrega en el mapa." />

      <div className="range-selector">
        {RANGE_PRESETS.map((p) => (
          <button
            key={p.key}
            className={`range-button ${p.key === presetKey ? 'active' : ''}`}
            onClick={() => setPresetKey(p.key)}
          >
            {p.label}
          </button>
        ))}
      </div>

      {error && <p className="error-text">{error}</p>}
      <div className="stats-grid four"><Stat label="Entregas confirmadas" value={loading ? '—' : metrics.reduce((n, m) => n + m.confirmed, 0)} icon="check" tone="mint" /><Stat label="Incidencias" value={loading ? '—' : metrics.reduce((n, m) => n + m.incident, 0)} icon="pin" tone="peach" /><Stat label="Intentos rechazados" value={loading ? '—' : metrics.reduce((n, m) => n + m.rejected, 0)} icon="shield" /><Stat label="Conductores con actividad" value={loading ? '—' : metrics.length} icon="users" /></div>
      <div className="section-heading"><h2>El pulso de tus entregas</h2><span>Ubicaciones registradas · {preset.label}</span></div>

      <div className="map-wrapper">
        <div ref={mapContainerRef} className="dashboard-map" />
        {loading && (
          <div className="map-skeleton-overlay">
            <span className="skeleton-bar" style={{ width: '160px', height: '1.1rem' }} />
          </div>
        )}
      </div>

      <div className="map-legend"><span className="success">Entregado</span><span className="warning">Incidencia</span><span className="error">Rechazado</span></div>

      <h2 className="section-title">Actividad del equipo</h2>

      {loading ? (
        <TableSkeleton rows={3} columns={5} widths={['65%', '30%', '30%', '30%', '25%']} />
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Conductor</th>
              <th>Entregado</th>
              <th>Incidencias</th>
              <th>Rechazados</th>
              <th>Total</th>
            </tr>
          </thead>
          <tbody>
            {metrics.map((m) => (
              <tr key={m.driverName}>
                <td>{m.driverName}</td>
                <td className="success-text">{m.confirmed}</td>
                <td className="warning-text">{m.incident}</td>
                <td className="error-text">{m.rejected}</td>
                <td><strong>{m.total}</strong></td>
              </tr>
            ))}
            {metrics.length === 0 && (
              <tr>
                <td colSpan={5}>Sin actividad en este rango.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      <div className="section-heading">
        <h2>Costo por entrega verificada</h2>
        <span>Reporte informativo (showback) · no genera cargos contables</span>
      </div>

      <div className="inline-form">
        <label>Mes<input type="month" value={costMonth} onChange={(e) => setCostMonth(e.target.value)} /></label>
      </div>

      {costError && <p className="error-text">{costError}</p>}

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

      <div className="section-heading">
        <h2>Resiliencia (Circuit Breaker + Retry)</h2>
        <span>Protegen la simulación del ERP (Fase1 §3/§5.1)</span>
      </div>

      {breakerError && <p className="error-text">{breakerError}</p>}

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
            <button type="button" disabled={breakerLoading} onClick={loadBreakerStatus}>Actualizar estado</button>
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
    </div>
  )
}
