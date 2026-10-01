import { useEffect, useMemo, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { MAP_ATTRIBUTION, MAP_TILES_URL } from '../../../config/mapTiles'
import apiClient from '../../../api/client'
import TableSkeleton from '../../../components/TableSkeleton'
import { Stat } from '../../../components/Workspace'
import { useAsyncData } from '../../../hooks/useAsyncData'
import type { DeliveryAttempt, DriverMetric } from '../../../types/domain'

interface MapAndMetrics {
  points: DeliveryAttempt[]
  metrics: DriverMetric[]
}

// Referencias estables para el fallback antes de la primera carga: un array `[]` literal
// inline en cada render cambiaria de identidad en cada render, y el useEffect que redibuja
// los marcadores del mapa depende de `points` (react-hooks/exhaustive-deps).
const EMPTY_POINTS: DeliveryAttempt[] = []
const EMPTY_METRICS: DriverMetric[] = []

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

/** Selector de rango + mapa (Leaflet) + tabla de actividad del equipo. */
export function DashboardMapSection() {
  const [presetKey, setPresetKey] = useState('today')

  const mapContainerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<L.Map | null>(null)
  const markersLayerRef = useRef<L.LayerGroup | null>(null)

  const preset = useMemo(() => RANGE_PRESETS.find((p) => p.key === presetKey)!, [presetKey])

  const { data: mapAndMetrics, loading, error } = useAsyncData<MapAndMetrics>(() => {
    const { from, to } = rangeFor(preset)
    return Promise.all([
      apiClient.get<DeliveryAttempt[]>('/admin/dashboard/map', { params: { from, to } }),
      apiClient.get<DriverMetric[]>('/admin/dashboard/metrics', { params: { from, to } }),
    ]).then(([mapRes, metricsRes]) => ({ points: mapRes.data, metrics: metricsRes.data }))
  }, [preset])
  const points = mapAndMetrics?.points ?? EMPTY_POINTS
  const metrics = mapAndMetrics?.metrics ?? EMPTY_METRICS

  // Inicializa el mapa una sola vez.
  useEffect(() => {
    if (mapRef.current || !mapContainerRef.current) return
    const map = L.map(mapContainerRef.current).setView([-1.8312, -78.1834], 6) // Ecuador
    L.tileLayer(MAP_TILES_URL, {
      attribution: MAP_ATTRIBUTION,
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
    <>
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

      {error && <p role="alert" className="error-text">{error}</p>}
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
    </>
  )
}
