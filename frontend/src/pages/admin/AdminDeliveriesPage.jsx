import { useEffect, useState } from 'react'
import apiClient from '../../api/client'
import TableSkeleton from '../../components/TableSkeleton'
import { PageIntro, Modal } from '../../components/Workspace'

const OUTCOME_LABELS = {
  CONFIRMED: 'Entregado',
  REJECTED: 'Rechazado',
  INCIDENT: 'Incidencia',
}

const OUTCOME_CLASSES = {
  CONFIRMED: 'success',
  REJECTED: 'error',
  INCIDENT: 'warning',
}

const SUSPICIOUS_DISTANCE_METERS = 2000

export default function AdminDeliveriesPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [photoUrl, setPhotoUrl] = useState(null)
  const [photoLoading, setPhotoLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    apiClient
      .get('/admin/deliveries', { params: { page, size: 20 } })
      .then((res) => setData(res.data))
      .finally(() => setLoading(false))
  }, [page])

  const viewPhoto = async (log) => {
    setPhotoLoading(true)
    try {
      const res = await apiClient.get(`/admin/deliveries/${log.id}/photo`, { responseType: 'blob' })
      setPhotoUrl(URL.createObjectURL(res.data))
    } finally {
      setPhotoLoading(false)
    }
  }

  const closePhoto = () => {
    if (photoUrl) URL.revokeObjectURL(photoUrl)
    setPhotoUrl(null)
  }

  return (
    <div>
      <PageIntro eyebrow="TRAZABILIDAD DE PRINCIPIO A FIN" title="Cada entrega, en detalle." description="Revisa los resultados, las ubicaciones y la evidencia de tu equipo." />
      <div className="section-heading"><h2>Registro de actividad</h2><span>Página {page + 1} de {Math.max(data.totalPages, 1)}</span></div>

      {loading ? (
        <TableSkeleton
          rows={8}
          columns={6}
          widths={['60%', '50%', '40%', '75%', '35%', '55%']}
        />
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Factura</th>
                <th>Conductor</th>
                <th>Resultado</th>
                <th>Ubicacion</th>
                <th>Foto</th>
                <th>Fecha</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((log) => (
                <tr key={log.id}>
                  <td className="mono">{log.invoiceNumber}</td>
                  <td>{log.driverName}</td>
                  <td>
                    <span className={`status-pill ${OUTCOME_CLASSES[log.outcome] || 'error'}`}>
                      {OUTCOME_LABELS[log.outcome] || log.outcome}
                    </span>
                    {log.detail && <div className="status-detail">{log.detail}</div>}
                  </td>
                  <td className="mono">
                    {log.latitude != null && log.longitude != null ? (
                      <a
                        href={`https://www.google.com/maps?q=${log.latitude},${log.longitude}`}
                        target="_blank"
                        rel="noreferrer"
                      >
                        {log.latitude.toFixed(5)}, {log.longitude.toFixed(5)}
                      </a>
                    ) : (
                      '-'
                    )}
                    {log.distanceFromExpectedMeters != null && log.distanceFromExpectedMeters > SUSPICIOUS_DISTANCE_METERS && (
                      <div className="status-detail warning-text">
                        ⚠ {(log.distanceFromExpectedMeters / 1000).toFixed(1)} km de la direccion registrada
                      </div>
                    )}
                  </td>
                  <td>
                    {log.hasPhoto ? (
                      <button className="link-button" onClick={() => viewPhoto(log)} disabled={photoLoading}>
                        Ver foto
                      </button>
                    ) : (
                      '-'
                    )}
                  </td>
                  <td>{new Date(log.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pagination">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Anterior
            </button>
            <span>Pagina {page + 1} de {Math.max(data.totalPages, 1)}</span>
            <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>
              Siguiente
            </button>
          </div>
        </>
      )}

      {photoUrl && (
        <Modal label="Evidencia de entrega" onClose={closePhoto}>
            <img src={photoUrl} alt="Evidencia de entrega" />
            <button className="link-button" onClick={closePhoto}>Cerrar</button>
        </Modal>
      )}
    </div>
  )
}
