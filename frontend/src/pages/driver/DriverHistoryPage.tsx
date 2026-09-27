import { useEffect, useRef, useState } from 'react'
import { DriverShell, PageIntro, Modal } from '../../components/Workspace'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import apiClient from '../../api/client'
import type { DeliveryAttempt, PageResponse } from '../../types/domain'

const OUTCOME_LABELS: Record<string, string> = {
  CONFIRMED: 'Entregado',
  REJECTED: 'Rechazado',
  INCIDENT: 'Incidencia',
}

const OUTCOME_CLASSES: Record<string, string> = {
  CONFIRMED: 'success',
  REJECTED: 'error',
  INCIDENT: 'warning',
}

const DETAIL_TABS = [
  { key: 'datos', label: 'Datos' },
  { key: 'foto', label: 'Foto' },
  { key: 'mapa', label: 'Mapa' },
] as const

type DetailTabKey = (typeof DETAIL_TABS)[number]['key']

export default function DriverHistoryPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<DeliveryAttempt>>({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [selected, setSelected] = useState<DeliveryAttempt | null>(null)
  const [activeTab, setActiveTab] = useState<DetailTabKey>('datos')
  const [photoUrl, setPhotoUrl] = useState<string | null>(null)
  const [photoLoading, setPhotoLoading] = useState(false)

  const mapContainerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setLoading(true)
    setError('')
    apiClient
      .get<PageResponse<DeliveryAttempt>>('/driver/deliveries/history', { params: { page, size: 20 } })
      .then((res) => setData(res.data))
      .catch((err) => setError(err.response?.data?.message || 'No se pudo cargar el historial.'))
      .finally(() => setLoading(false))
  }, [page])

  const openDetail = async (item: DeliveryAttempt) => {
    setSelected(item)
    setActiveTab('datos')
    setPhotoUrl(null)
    if (item.hasPhoto) {
      setPhotoLoading(true)
      try {
        const res = await apiClient.get(`/driver/deliveries/${item.id}/photo`, { responseType: 'blob' })
        setPhotoUrl(URL.createObjectURL(res.data))
      } finally {
        setPhotoLoading(false)
      }
    }
  }

  const closeDetail = () => {
    if (photoUrl) URL.revokeObjectURL(photoUrl)
    setPhotoUrl(null)
    setSelected(null)
  }

  // Inicializa el mapa solo cuando la pestana "Mapa" esta visible: el contenedor
  // no existe en el DOM (ni tiene tamano) mientras el usuario esta en otra pestana.
  useEffect(() => {
    if (activeTab !== 'mapa' || !selected || selected.latitude == null || selected.longitude == null || !mapContainerRef.current) return

    const map = L.map(mapContainerRef.current).setView([selected.latitude, selected.longitude], 16)
    // Tiles servidos same-origin via /map-tiles/ (proxy de nginx) por defecto: algunas
    // redes moviles/corporativas del conductor bloquean CDNs de terceros directo. En
    // despliegues sin ese proxy, VITE_MAP_TILES_URL apunta directo al proveedor.
    L.tileLayer(import.meta.env.VITE_MAP_TILES_URL || '/map-tiles/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(map)
    L.circleMarker([selected.latitude, selected.longitude], {
      radius: 9,
      color: '#3fbf7f',
      fillColor: '#3fbf7f',
      fillOpacity: 0.85,
      weight: 2,
    }).addTo(map)

    // El contenedor recien aparece dentro del modal; Leaflet necesita recalcular
    // el tamano una vez que el layout ya se aplico, o el mapa queda en blanco.
    const resizeTimer = setTimeout(() => map.invalidateSize(), 0)

    return () => {
      clearTimeout(resizeTimer)
      map.remove()
    }
  }, [selected, activeTab])

  return (
    <DriverShell>
      <PageIntro eyebrow="CADA PASO QUEDA REGISTRADO" title="Tu recorrido" description="Consulta tus entregas, evidencias e incidencias en un solo lugar." />

      {error && <p className="error-text">{error}</p>}

      {loading ? (
        <p>Cargando...</p>
      ) : (
        <>
          {data.content.length === 0 && (
            <div className="empty-state">
              <div className="empty-mark">—</div>
              <p>Todavia no registraste entregas.</p>
            </div>
          )}

          <ul className="invoice-list">
            {data.content.map((item) => (
              <li key={item.id} className="invoice-item">
                <button className="invoice-open" onClick={() => openDetail(item)}>
                <div className="invoice-main">
                  <span className="invoice-code">{item.invoiceNumber}</span>
                  <span className={`status-pill ${OUTCOME_CLASSES[item.outcome] || 'error'}`}>
                    {OUTCOME_LABELS[item.outcome] || item.outcome}
                  </span>
                  <span className="invoice-partner">{new Date(item.createdAt).toLocaleString()}</span>
                </div>
                <span className="invoice-arrow" aria-hidden="true">→</span>
                </button>
              </li>
            ))}
          </ul>

          {data.totalPages > 1 && (
            <div className="pagination">
              <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Anterior</button>
              <span>Pagina {page + 1} de {Math.max(data.totalPages, 1)}</span>
              <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>Siguiente</button>
            </div>
          )}
        </>
      )}

      {selected && (
        <Modal label="Detalle de entrega" onClose={closeDetail}>
            <h2 className="ticket-code">{selected.invoiceNumber}</h2>

            <div className="range-selector">
              {DETAIL_TABS.map((tab) => (
                <button
                  key={tab.key}
                  type="button"
                  className={`range-button ${activeTab === tab.key ? 'active' : ''}`}
                  onClick={() => setActiveTab(tab.key)}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            {activeTab === 'datos' && (
              <div className="detail-tab-panel">
                <span className={`status-pill ${OUTCOME_CLASSES[selected.outcome] || 'error'}`}>
                  {OUTCOME_LABELS[selected.outcome] || selected.outcome}
                </span>

                <div className="detail-info-list">
                  <div className="detail-info-row">
                    <span className="detail-info-label">Factura</span>
                    <span className="detail-info-value detail-info-mono">{selected.invoiceNumber}</span>
                  </div>
                  {selected.partnerName && (
                    <div className="detail-info-row">
                      <span className="detail-info-label">Cliente</span>
                      <span className="detail-info-value">{selected.partnerName}</span>
                    </div>
                  )}
                  {selected.deliveryAddress && (
                    <div className="detail-info-row">
                      <span className="detail-info-label">Direccion</span>
                      <span className="detail-info-value">{selected.deliveryAddress}</span>
                    </div>
                  )}
                  <div className="detail-info-row">
                    <span className="detail-info-label">Fecha</span>
                    <span className="detail-info-value">{new Date(selected.createdAt).toLocaleDateString()}</span>
                  </div>
                  <div className="detail-info-row">
                    <span className="detail-info-label">Hora</span>
                    <span className="detail-info-value">{new Date(selected.createdAt).toLocaleTimeString()}</span>
                  </div>
                  {selected.distanceFromExpectedMeters != null && (
                    <div className="detail-info-row">
                      <span className="detail-info-label">Distancia de la direccion</span>
                      <span className="detail-info-value">
                        {selected.distanceFromExpectedMeters >= 1000
                          ? `${(selected.distanceFromExpectedMeters / 1000).toFixed(2)} km`
                          : `${Math.round(selected.distanceFromExpectedMeters)} m`}
                      </span>
                    </div>
                  )}
                </div>

                {selected.detail && (
                  <div className="detail-note">
                    <span className="detail-info-label">Motivo</span>
                    <p className="status-detail">{selected.detail}</p>
                  </div>
                )}
              </div>
            )}

            {activeTab === 'foto' && (
              <div className="detail-tab-panel">
                {photoLoading && <p>Cargando foto...</p>}
                {photoUrl && <img src={photoUrl} alt="Evidencia de entrega" />}
                {!photoLoading && !selected.hasPhoto && <p className="hint-text">Sin foto de evidencia.</p>}
              </div>
            )}

            {activeTab === 'mapa' && (
              <div className="detail-tab-panel">
                {selected.latitude != null && selected.longitude != null ? (
                  <div className="map-wrapper">
                    <div ref={mapContainerRef} className="dashboard-map" />
                  </div>
                ) : (
                  <p className="hint-text">Sin ubicacion registrada.</p>
                )}
              </div>
            )}

            <button className="link-button" onClick={closeDetail}>Cerrar</button>
        </Modal>
      )}
    </DriverShell>
  )
}
