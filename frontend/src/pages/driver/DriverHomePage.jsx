import { useEffect, useRef, useState } from 'react'
import { DriverShell, Icon, PageIntro } from '../../components/Workspace'
import apiClient from '../../api/client'
import { useAuth } from '../../context/AuthContext'

const PIN_LENGTH = 6
const MAX_PHOTO_DIMENSION = 1280
const PHOTO_QUALITY = 0.7

const INCIDENT_REASONS = [
  'Cliente ausente',
  'Direccion incorrecta',
  'Producto danado',
  'Cliente rechazo la entrega',
  'Otro',
]

function getCurrentPosition() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('Este dispositivo no soporta geolocalizacion.'))
      return
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 15000,
      maximumAge: 0,
    })
  })
}

function compressImage(file, maxDimension = MAX_PHOTO_DIMENSION, quality = PHOTO_QUALITY) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('No se pudo leer la imagen.'))
    reader.onload = () => {
      const img = new Image()
      img.onerror = () => reject(new Error('No se pudo procesar la imagen.'))
      img.onload = () => {
        let { width, height } = img
        if (width > height && width > maxDimension) {
          height = Math.round((height * maxDimension) / width)
          width = maxDimension
        } else if (height > maxDimension) {
          width = Math.round((width * maxDimension) / height)
          height = maxDimension
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        canvas.getContext('2d').drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', quality))
      }
      img.src = reader.result
    }
    reader.readAsDataURL(file)
  })
}

function formatQuantity(quantity) {
  if (quantity == null) return null
  return Number.isInteger(quantity) ? String(quantity) : quantity.toFixed(2)
}

const EARTH_RADIUS_METERS = 6371000
const SUSPICIOUS_DISTANCE_METERS = 2000

function distanceMeters(lat1, lon1, lat2, lon2) {
  const toRad = (deg) => (deg * Math.PI) / 180
  const dLat = toRad(lat2 - lat1)
  const dLon = toRad(lon2 - lon1)
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2
  return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

// Formato de numero de factura del SRI (establecimiento-punto de emision-secuencial), ej. "046-101-000005884".
const INVOICE_NUMBER_PATTERN = /(?<!\d)\d{3}-\d{3}-\d{9,10}(?!\d)/
// Longitud maxima de un candidato de respaldo (17 = "NNN-NNN-NNNNNNNNN").
// Evita confundir el numero de factura con el numero de autorizacion o la
// clave de acceso del SRI, que son cadenas de 40+ digitos en el mismo documento.
const MAX_FALLBACK_LENGTH = 18

// Extrae el numero de factura de un texto OCR, priorizando el formato del SRI.
function extractInvoiceCandidate(text) {
  const strictMatch = text.match(INVOICE_NUMBER_PATTERN)
  if (strictMatch) return strictMatch[0]

  // Las lineas de "numero de autorizacion" y "clave de acceso" tambien traen
  // secuencias largas de digitos; se descartan para no confundirlas con la factura.
  const relevantText = text
    .split('\n')
    .filter((line) => !/autorizaci|clave de acceso/i.test(line))
    .join('\n')

  const matches = relevantText.match(/\d[\d-]{5,}\d/g)
  if (!matches || matches.length === 0) return null

  const candidates = matches.filter((m) => m.length <= MAX_FALLBACK_LENGTH)
  if (candidates.length === 0) return null
  return candidates.sort((a, b) => b.length - a.length)[0]
}

function PinBoxes({ value, onChange, disabled }) {
  const inputRefs = useRef([])
  const digits = value.split('')
  while (digits.length < PIN_LENGTH) digits.push('')

  const setDigit = (index, char) => {
    const next = [...digits]
    next[index] = char
    onChange(next.join('').slice(0, PIN_LENGTH))
  }

  const handleChange = (index, e) => {
    const raw = e.target.value.replace(/\D/g, '')
    if (!raw) {
      setDigit(index, '')
      return
    }
    const chars = raw.split('')
    const next = [...digits]
    chars.forEach((c, i) => { if (index + i < PIN_LENGTH) next[index + i] = c })
    onChange(next.join('').slice(0, PIN_LENGTH))
    const nextIndex = Math.min(index + chars.length, PIN_LENGTH - 1)
    inputRefs.current[nextIndex]?.focus()
  }

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus()
    }
  }

  return (
    <div className="pin-boxes">
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(el) => (inputRefs.current[index] = el)}
          value={digit}
          inputMode="numeric"
          aria-label={`Dígito ${index + 1} del PIN`}
          maxLength={1}
          className={digit ? 'filled' : ''}
          disabled={disabled}
          onChange={(e) => handleChange(index, e)}
          onPaste={(e) => {
            const raw = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, PIN_LENGTH)
            if (raw) { e.preventDefault(); onChange(raw); inputRefs.current[Math.min(raw.length, PIN_LENGTH - 1)]?.focus() }
          }}
          onKeyDown={(e) => handleKeyDown(index, e)}
          autoFocus={index === 0}
        />
      ))}
    </div>
  )
}

export default function DriverHomePage() {
  const { user } = useAuth()
  const photoInputRef = useRef(null)
  const scanInputRef = useRef(null)

  const [query, setQuery] = useState('')
  const [invoices, setInvoices] = useState([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState('')
  const [hasSearched, setHasSearched] = useState(false)
  const [scanning, setScanning] = useState(false)

  const [selectedInvoice, setSelectedInvoice] = useState(null)
  const [lines, setLines] = useState([])
  const [linesLoading, setLinesLoading] = useState(false)
  const [linesError, setLinesError] = useState('')
  const [checkedIds, setCheckedIds] = useState(() => new Set())

  const [pin, setPin] = useState('')
  const [photo, setPhoto] = useState(null) // { dataUrl, filename }
  const [photoError, setPhotoError] = useState('')
  const [confirming, setConfirming] = useState(false)
  const [feedback, setFeedback] = useState(null)
  const [locationState, setLocationState] = useState('idle') // idle | locating | ready | error
  const [showStamp, setShowStamp] = useState(false)

  const [showIncidentForm, setShowIncidentForm] = useState(false)
  const [incidentReason, setIncidentReason] = useState(INCIDENT_REASONS[0])
  const [incidentNotes, setIncidentNotes] = useState('')
  const [reportingIncident, setReportingIncident] = useState(false)
  const [incidentError, setIncidentError] = useState('')

  useEffect(() => {
    if (!selectedInvoice) return

    setLocationState('locating')
    getCurrentPosition()
      .then(() => setLocationState('ready'))
      .catch(() => setLocationState('error'))

    setLinesLoading(true)
    setLinesError('')
    apiClient
      .get(`/driver/invoices/${selectedInvoice.id}/lines`)
      .then(({ data }) => setLines(data))
      .catch((err) => setLinesError(err.response?.data?.message || 'No se pudo cargar el detalle de productos.'))
      .finally(() => setLinesLoading(false))
  }, [selectedInvoice])

  const runSearch = async (rawQuery) => {
    const q = rawQuery.trim()
    if (!q) return
    setSearching(true)
    setSearchError('')
    setFeedback(null)
    setHasSearched(true)
    try {
      const { data } = await apiClient.get('/driver/invoices', { params: { q } })
      setInvoices(data)
    } catch (err) {
      if (err.response?.status === 401) {
        setSearchError('Tu sesion expiro. Iniciando sesion de nuevo...')
        return
      }
      setSearchError(err.response?.data?.message || 'Error buscando la factura.')
    } finally {
      setSearching(false)
    }
  }

  const handleSearch = (e) => {
    e.preventDefault()
    runSearch(query)
  }

  const handleScanSelected = async (e) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setSearchError('')
    setScanning(true)
    try {
      const dataUrl = await compressImage(file, 1600, 0.85)
      const { default: Tesseract } = await import('tesseract.js')
      // Servidos localmente (public/tesseract-assets) en vez del CDN por defecto de
      // tesseract.js: algunas redes moviles/corporativas bloquean cdn.jsdelivr.net y
      // devuelven una pagina HTML de error, lo que rompe el worker/wasm en runtime.
      const { data } = await Tesseract.recognize(dataUrl, 'eng', {
        workerPath: '/tesseract-assets/worker.min.js',
        corePath: '/tesseract-assets',
        langPath: '/tesseract-assets',
      })
      const candidate = extractInvoiceCandidate(data.text)
      if (!candidate) {
        setSearchError('No se pudo leer un numero de factura en la foto. Escribelo manualmente.')
        return
      }
      setQuery(candidate)
      await runSearch(candidate)
    } catch (err) {
      setSearchError(err.message || 'No se pudo escanear la factura.')
    } finally {
      setScanning(false)
    }
  }

  const resetSelection = () => {
    setSelectedInvoice(null)
    setLines([])
    setCheckedIds(new Set())
    setPin('')
    setPhoto(null)
    setPhotoError('')
    setFeedback(null)
    setLocationState('idle')
    setShowIncidentForm(false)
    setIncidentReason(INCIDENT_REASONS[0])
    setIncidentNotes('')
    setIncidentError('')
  }

  const selectInvoice = (invoice) => {
    resetSelection()
    setSelectedInvoice(invoice)
  }

  const toggleChecked = (lineId) => {
    setCheckedIds((prev) => {
      const next = new Set(prev)
      if (next.has(lineId)) {
        next.delete(lineId)
      } else {
        next.add(lineId)
      }
      return next
    })
  }

  const handlePhotoSelected = async (e) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setPhotoError('')
    try {
      const dataUrl = await compressImage(file)
      setPhoto({ dataUrl, filename: file.name || 'evidencia.jpg' })
    } catch (err) {
      setPhotoError(err.message || 'No se pudo procesar la foto.')
    }
  }

  const allChecked = lines.length > 0 && lines.every((line) => checkedIds.has(line.id))
  const canConfirm = pin.length === PIN_LENGTH && allChecked && !!photo

  const handleConfirm = async (e) => {
    e.preventDefault()
    if (!selectedInvoice || !canConfirm) return
    setConfirming(true)
    setFeedback(null)
    try {
      const position = await getCurrentPosition()
      const { latitude, longitude } = position.coords

      if (selectedInvoice.expectedLatitude != null && selectedInvoice.expectedLongitude != null) {
        const distance = distanceMeters(latitude, longitude, selectedInvoice.expectedLatitude, selectedInvoice.expectedLongitude)
        if (distance > SUSPICIOUS_DISTANCE_METERS) {
          const km = (distance / 1000).toFixed(1)
          const proceed = window.confirm(
            `Estas a ${km} km de la direccion registrada del cliente. ¿Confirmar la entrega de todas formas?`
          )
          if (!proceed) {
            setConfirming(false)
            return
          }
        }
      }

      const { data } = await apiClient.post('/driver/deliveries/confirm', {
        invoiceId: selectedInvoice.id,
        invoiceNumber: selectedInvoice.number,
        partnerName: selectedInvoice.partnerName,
        deliveryAddress: selectedInvoice.deliveryAddress,
        pin,
        latitude,
        longitude,
        photoBase64: photo.dataUrl,
        photoFilename: photo.filename,
        photoContentType: 'image/jpeg',
      })

      setShowStamp(true)
      setInvoices((prev) => prev.filter((inv) => inv.id !== selectedInvoice.id))
      setTimeout(() => {
        setShowStamp(false)
        resetSelection()
        setFeedback({ type: data.photoUploaded ? 'success' : 'warning', message: data.message })
      }, 1300)
    } catch (err) {
      const message = err.message?.includes('geolocaliz')
        ? err.message
        : err.response?.data?.message || 'No se pudo confirmar la entrega.'
      setFeedback({ type: 'error', message })
    } finally {
      setConfirming(false)
    }
  }

  const handleReportIncident = async (e) => {
    e.preventDefault()
    if (!selectedInvoice || !incidentReason) return
    setReportingIncident(true)
    setIncidentError('')
    try {
      let latitude = null
      let longitude = null
      try {
        const position = await getCurrentPosition()
        latitude = position.coords.latitude
        longitude = position.coords.longitude
      } catch {
        // Se reporta igual sin ubicacion si el GPS no esta disponible.
      }

      await apiClient.post('/driver/deliveries/incident', {
        invoiceId: selectedInvoice.id,
        invoiceNumber: selectedInvoice.number,
        partnerName: selectedInvoice.partnerName,
        deliveryAddress: selectedInvoice.deliveryAddress,
        reason: incidentReason,
        notes: incidentNotes,
        latitude,
        longitude,
      })

      const invoiceNumber = selectedInvoice.number
      resetSelection()
      setFeedback({ type: 'success', message: `Incidencia reportada para ${invoiceNumber}.` })
    } catch (err) {
      setIncidentError(err.response?.data?.message || 'No se pudo reportar la incidencia.')
    } finally {
      setReportingIncident(false)
    }
  }

  return (
    <DriverShell>
      <PageIntro eyebrow="TU JORNADA, EN MARCHA" title={selectedInvoice ? 'Completa la entrega' : `Hola, ${user?.fullName?.split(' ')[0] || 'conductor'}.`} description={selectedInvoice ? 'Revisa el pedido, guarda la evidencia y confirma con el cliente.' : 'Cada pedido tiene un destino. Encuentra tu próxima entrega.'}><span className="date-chip">{new Date().toLocaleDateString('es-EC', { day: 'numeric', month: 'long' })}</span></PageIntro>

      {!selectedInvoice && (
        <>
          <section className="search-panel">
          <div className="search-panel-heading"><span className="surface-icon"><Icon name="search" size={24} /></span><div><h2>Encuentra un pedido</h2><p>Busca por número de factura o nombre del cliente.</p></div></div>
          <form className="search-form" onSubmit={handleSearch}>
            <input
              type="text"
              aria-label="Número de factura o cliente"
              placeholder="N° de factura o cliente"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              autoFocus
            />
            <button type="submit" disabled={searching || scanning}>
              {searching ? '...' : 'Buscar'}
            </button>
          </form>

          <input
            ref={scanInputRef}
            type="file"
            accept="image/*"
            capture="environment"
            hidden
            onChange={handleScanSelected}
          />
          <button
            type="button"
            className="scan-button"
            onClick={() => scanInputRef.current?.click()}
            disabled={scanning || searching}
          >
            <Icon name="camera" />{scanning ? 'Leyendo factura...' : 'Escanear factura con la cámara'}
          </button>
          </section>
          {!hasSearched && <div className="start-guide"><p className="eyebrow">UNA ENTREGA EN TRES PASOS</p><div className="guide-grid"><div><span>01</span><Icon name="box" /><h3>Revisa el pedido</h3><p>Encuentra la factura y verifica los productos.</p></div><div><span>02</span><Icon name="camera" /><h3>Guarda la evidencia</h3><p>Toma una foto al completar la entrega.</p></div><div><span>03</span><Icon name="shield" /><h3>Confirma con PIN</h3><p>Pide los seis dígitos al cliente. Listo.</p></div></div></div>}
          {hasSearched && <div className="section-heading"><h2>Pedidos encontrados</h2><span>{searching ? 'Buscando…' : `${invoices.length} resultados`}</span></div>}

          {searchError && <p className="error-text">{searchError}</p>}
          {feedback?.type === 'success' && <p className="success-text">{feedback.message}</p>}
          {feedback?.type === 'warning' && <p className="warning-text">{feedback.message}</p>}

          {hasSearched && !searching && invoices.length === 0 && !searchError && (
            <div className="empty-state">
              <div className="empty-mark">—</div>
              <p>No hay facturas pendientes de entrega con ese criterio.</p>
            </div>
          )}

          <ul className="invoice-list">
            {invoices.map((invoice) => (
              <li key={invoice.id} className="invoice-item">
                <button className="invoice-open" onClick={() => selectInvoice(invoice)}>
                <span className="invoice-package"><Icon name="box" size={26} /></span>
                <div className="invoice-main">
                  <span className="invoice-code">{invoice.number}</span>
                  <span className="invoice-partner">{invoice.partnerName}</span>
                  {invoice.deliveryAddress && (
                    <span className="invoice-address">{invoice.deliveryAddress}</span>
                  )}
                </div>
                <span className="invoice-arrow" aria-hidden="true">→</span>
                </button>
              </li>
            ))}
          </ul>
        </>
      )}

      {selectedInvoice && !showIncidentForm && (
        <form className="pin-form" onSubmit={handleConfirm}>
          <button type="button" className="link-button" onClick={resetSelection}>← Volver a buscar</button>

          <div className="ticket-header">
            <div className="ticket-code">{selectedInvoice.number}</div>
            <h2 className="ticket-partner">{selectedInvoice.partnerName}</h2>
            {selectedInvoice.deliveryAddress && (
              <div className="ticket-address">
                <Icon name="pin" />
                {selectedInvoice.deliveryAddress}
              </div>
            )}
          </div>

          <p className="pin-section-label">
            Productos {lines.length > 0 && `(${checkedIds.size}/${lines.length})`}
          </p>

          {linesLoading && <p className="hint-text">Cargando productos...</p>}
          {linesError && <p className="error-text">{linesError}</p>}

          {!linesLoading && !linesError && (
            <ul className="checklist">
              {lines.map((line) => {
                const checked = checkedIds.has(line.id)
                const quantity = formatQuantity(line.quantity)
                return (
                  <li
                    key={line.id}
                    className={`checklist-item ${checked ? 'checked' : ''}`}
                  >
                    <label className="checklist-control">
                    <input type="checkbox" checked={checked} onChange={() => toggleChecked(line.id)} />
                    <span className="checklist-label">
                      {quantity && <span className="checklist-qty">{quantity}×</span>}
                      {line.description}
                    </span>
                    </label>
                  </li>
                )
              })}
              {lines.length === 0 && <p className="hint-text">Esta factura no tiene productos para verificar.</p>}
            </ul>
          )}

          <p className="pin-section-label">Foto de evidencia</p>
          <input
            ref={photoInputRef}
            type="file"
            accept="image/*"
            capture="environment"
            hidden
            onChange={handlePhotoSelected}
          />
          {!photo && (
            <button type="button" className="photo-capture-button" onClick={() => photoInputRef.current?.click()}>
              <Icon name="camera" size={24} />Tomar foto de la entrega
            </button>
          )}
          {photo && (
            <div className="photo-preview">
              <img src={photo.dataUrl} alt="Evidencia de entrega" />
              <button type="button" className="link-button" onClick={() => photoInputRef.current?.click()}>
                Tomar otra
              </button>
            </div>
          )}
          {photoError && <p className="error-text">{photoError}</p>}

          <p className="pin-section-label">PIN de entrega</p>
          <PinBoxes value={pin} onChange={setPin} disabled={confirming} />
          {lines.length > 0 && !allChecked && (
            <p className="hint-text">Marca todos los productos antes de confirmar.</p>
          )}
          {allChecked && !photo && (
            <p className="hint-text">Toma la foto de evidencia antes de confirmar.</p>
          )}

          <div className={`location-status ${locationState}`}>
            <span className="location-dot" />
            {locationState === 'locating' && 'Obteniendo tu ubicacion...'}
            {locationState === 'ready' && 'Ubicacion lista'}
            {locationState === 'error' && 'No se pudo obtener tu ubicacion, se reintentara al confirmar'}
            {locationState === 'idle' && 'Ubicacion'}
          </div>

          {feedback?.type === 'error' && <p className="error-text">{feedback.message}</p>}

          <button type="submit" disabled={confirming || !canConfirm}>
            {confirming ? 'Confirmando...' : 'Confirmar entrega'}
          </button>

          <button
            type="button"
            className="incident-toggle-button"
            onClick={() => setShowIncidentForm(true)}
            disabled={confirming}
          >
            No se pudo entregar — reportar incidencia
          </button>
        </form>
      )}

      {selectedInvoice && showIncidentForm && (
        <form className="pin-form" onSubmit={handleReportIncident}>
          <button type="button" className="link-button" onClick={() => setShowIncidentForm(false)}>
            ← Volver a la entrega
          </button>

          <div className="ticket-header">
            <div className="ticket-code">{selectedInvoice.number}</div>
            <h2 className="ticket-partner">{selectedInvoice.partnerName}</h2>
          </div>

          <p className="pin-section-label">Motivo</p>
          <select aria-label="Motivo de la incidencia" value={incidentReason} onChange={(e) => setIncidentReason(e.target.value)}>
            {INCIDENT_REASONS.map((reason) => (
              <option key={reason} value={reason}>{reason}</option>
            ))}
          </select>

          <p className="pin-section-label">Observaciones (opcional)</p>
          <textarea
            className="incident-notes"
            aria-label="Observaciones de la incidencia"
            rows={3}
            value={incidentNotes}
            onChange={(e) => setIncidentNotes(e.target.value)}
            placeholder="Detalle adicional para el administrador..."
          />

          {incidentError && <p className="error-text">{incidentError}</p>}

          <button type="submit" disabled={reportingIncident}>
            {reportingIncident ? 'Enviando...' : 'Enviar reporte'}
          </button>
        </form>
      )}

      {showStamp && (
        <div className="stamp-overlay">
          <div className="stamp-seal">
            <Icon name="check" size={46} />
            <strong>ENTREGADO</strong>
            <span>{selectedInvoice?.number}</span>
          </div>
        </div>
      )}
    </DriverShell>
  )
}
