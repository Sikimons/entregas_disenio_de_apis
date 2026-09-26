import { useEffect, useMemo, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import apiClient from '../../api/client'
import TableSkeleton from '../../components/TableSkeleton'
import { PageIntro, Stat } from '../../components/Workspace'

const RANGE_PRESETS = [
  { key: 'today', label: 'Hoy', days: 0 },
  { key: '7d', label: 'Ultimos 7 dias', days: 7 },
  { key: '30d', label: 'Ultimos 30 dias', days: 30 },
]

const OUTCOME_COLORS = {
  CONFIRMED: '#3fbf7f',
  INCIDENT: '#f2a93b',
  REJECTED: '#d7392b',
}

const OUTCOME_LABELS = {
  CONFIRMED: 'Entregado',
  INCIDENT: 'Incidencia',
  REJECTED: 'Rechazado',
}

function startOfDay(date) {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  return d
}

function rangeFor(preset) {
  const to = new Date()
  const from = preset.days === 0 ? startOfDay(to) : new Date(to.getTime() - preset.days * 86400000)
  return { from: from.toISOString(), to: to.toISOString() }
}

export default function AdminDashboardPage() {
  const [presetKey, setPresetKey] = useState('today')
  const [points, setPoints] = useState([])
  const [metrics, setMetrics] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const mapContainerRef = useRef(null)
  const mapRef = useRef(null)
  const markersLayerRef = useRef(null)

  const preset = useMemo(() => RANGE_PRESETS.find((p) => p.key === presetKey), [presetKey])

  useEffect(() => {
    const { from, to } = rangeFor(preset)
    setLoading(true)
    setError('')
    Promise.all([
      apiClient.get('/admin/dashboard/map', { params: { from, to } }),
      apiClient.get('/admin/dashboard/metrics', { params: { from, to } }),
    ])
      .then(([mapRes, metricsRes]) => {
        setPoints(mapRes.data)
        setMetrics(metricsRes.data)
      })
      .catch((err) => setError(err.response?.data?.message || 'No se pudo cargar el tablero.'))
      .finally(() => setLoading(false))
  }, [preset])

  // Inicializa el mapa una sola vez.
  useEffect(() => {
    if (mapRef.current || !mapContainerRef.current) return
    const map = L.map(mapContainerRef.current).setView([-1.8312, -78.1834], 6) // Ecuador
    // Tiles servidos same-origin via /map-tiles/ (proxy de nginx a Carto): algunas redes
    // moviles/corporativas bloquean CDNs de terceros directo.
    L.tileLayer('/map-tiles/light_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
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

    const latLngs = []
    points.forEach((point) => {
      const color = OUTCOME_COLORS[point.outcome] || '#888'
      const marker = L.circleMarker([point.latitude, point.longitude], {
        radius: 8,
        color,
        fillColor: color,
        fillOpacity: 0.85,
        weight: 2,
      })
      marker.bindPopup(`
        <strong>${OUTCOME_LABELS[point.outcome] || point.outcome}</strong><br/>
        ${point.invoiceNumber}<br/>
        ${point.driverName}<br/>
        ${point.detail ? point.detail + '<br/>' : ''}
        ${new Date(point.createdAt).toLocaleString()}
      `)
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
    </div>
  )
}
