import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import axios from 'axios'
import { DriverShell, Icon, PageIntro } from '../../components/Workspace'
import apiClient from '../../api/client'
import { useAuth } from '../../context/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { getErrorMessage } from '../../utils/errors'
import { distanceMeters } from '../../utils/geo'
import { getCurrentPosition } from './driverHome/geolocation'
import { compressImage } from './driverHome/imageUtils'
import { scanInvoiceNumber } from './driverHome/invoiceOcr'
import { PIN_LENGTH } from './driverHome/PinBoxes'
import { InvoiceSearchPanel, type DeliveryFeedback } from './driverHome/InvoiceSearchPanel'
import { ConfirmDeliveryForm, type CapturedPhoto, type LocationState } from './driverHome/ConfirmDeliveryForm'
import { IncidentForm } from './driverHome/IncidentForm'
import { INCIDENT_REASONS } from './driverHome/incidentReasons'
import type { Invoice, InvoiceLine } from '../../types/domain'

interface ConfirmDeliveryResponse {
  success: boolean
  message: string
  photoUploaded: boolean
}

const SUSPICIOUS_DISTANCE_METERS = 2000

export default function DriverHomePage() {
  const { user } = useAuth()
  const photoInputRef = useRef<HTMLInputElement>(null)
  const scanInputRef = useRef<HTMLInputElement>(null)

  const [query, setQuery] = useState('')
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState('')
  const [hasSearched, setHasSearched] = useState(false)
  const [scanning, setScanning] = useState(false)

  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null)
  const [checkedIds, setCheckedIds] = useState<Set<number>>(() => new Set())

  const [pin, setPin] = useState('')
  const [photo, setPhoto] = useState<CapturedPhoto | null>(null)
  const [photoError, setPhotoError] = useState('')
  const [confirming, setConfirming] = useState(false)
  const [feedback, setFeedback] = useState<DeliveryFeedback | null>(null)
  const [locationState, setLocationState] = useState<LocationState>('idle')
  const [showStamp, setShowStamp] = useState(false)

  const [showIncidentForm, setShowIncidentForm] = useState(false)
  const [incidentReason, setIncidentReason] = useState<string>(INCIDENT_REASONS[0])
  const [incidentNotes, setIncidentNotes] = useState('')
  const [reportingIncident, setReportingIncident] = useState(false)
  const [incidentError, setIncidentError] = useState('')

  // GPS: un solo estado enum (LocationState), no un triple loading/error/data -- no
  // justifica forzarlo al hook useAsyncData (esa parte del efecto combinado si se separo
  // en la busqueda de lineas, mas abajo).
  useEffect(() => {
    if (!selectedInvoice) return
    // oxlint-disable-next-line react/set-state-in-effect -- ver comentario de arriba
    setLocationState('locating')
    getCurrentPosition()
      .then(() => setLocationState('ready'))
      .catch(() => setLocationState('error'))
  }, [selectedInvoice])

  const { data: lines = [], loading: linesLoading, error: linesError } = useAsyncData<InvoiceLine[]>(
    () => selectedInvoice
      ? apiClient.get<InvoiceLine[]>(`/driver/invoices/${selectedInvoice.id}/lines`).then((res) => res.data)
      : Promise.resolve([]),
    [selectedInvoice]
  )

  const runSearch = async (rawQuery: string) => {
    const q = rawQuery.trim()
    if (!q) return
    setSearching(true)
    setSearchError('')
    setFeedback(null)
    setHasSearched(true)
    try {
      const { data } = await apiClient.get<Invoice[]>('/driver/invoices', { params: { q } })
      setInvoices(data)
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        setSearchError('Tu sesion expiro. Iniciando sesion de nuevo...')
        return
      }
      setSearchError(getErrorMessage(err, 'Error buscando la factura.'))
    } finally {
      setSearching(false)
    }
  }

  const handleSearch = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    runSearch(query)
  }

  const handleScanSelected = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setSearchError('')
    setScanning(true)
    try {
      const candidate = await scanInvoiceNumber(file)
      if (!candidate) {
        setSearchError('No se pudo leer un numero de factura en la foto. Escribelo manualmente.')
        return
      }
      setQuery(candidate)
      await runSearch(candidate)
    } catch (err: unknown) {
      setSearchError(getErrorMessage(err, 'No se pudo escanear la factura.'))
    } finally {
      setScanning(false)
    }
  }

  const resetSelection = () => {
    setSelectedInvoice(null)
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

  const selectInvoice = (invoice: Invoice) => {
    resetSelection()
    setSelectedInvoice(invoice)
  }

  const toggleChecked = (lineId: number) => {
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

  const handlePhotoSelected = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setPhotoError('')
    try {
      const dataUrl = await compressImage(file)
      setPhoto({ dataUrl, filename: file.name || 'evidencia.jpg' })
    } catch (err: unknown) {
      setPhotoError(getErrorMessage(err, 'No se pudo procesar la foto.'))
    }
  }

  const allChecked = lines.length > 0 && lines.every((line) => checkedIds.has(line.id))
  const canConfirm = pin.length === PIN_LENGTH && allChecked && !!photo

  const handleConfirm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!selectedInvoice || !canConfirm || !photo) return
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

      const { data } = await apiClient.post<ConfirmDeliveryResponse>('/driver/deliveries/confirm', {
        invoiceId: selectedInvoice.id,
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
    } catch (err: unknown) {
      // getErrorMessage ya prioriza el mensaje del backend para errores de axios y cae al
      // .message propio para cualquier otro (incluido el GeolocationPositionError que
      // lanza getCurrentPosition), sin necesidad de adivinar por contenido del texto.
      setFeedback({ type: 'error', message: getErrorMessage(err, 'No se pudo confirmar la entrega.') })
    } finally {
      setConfirming(false)
    }
  }

  const handleReportIncident = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!selectedInvoice || !incidentReason) return
    setReportingIncident(true)
    setIncidentError('')
    try {
      let latitude: number | null = null
      let longitude: number | null = null
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
    } catch (err: unknown) {
      setIncidentError(getErrorMessage(err, 'No se pudo reportar la incidencia.'))
    } finally {
      setReportingIncident(false)
    }
  }

  return (
    <DriverShell>
      <PageIntro eyebrow="TU JORNADA, EN MARCHA" title={selectedInvoice ? 'Completa la entrega' : `Hola, ${user?.fullName?.split(' ')[0] || 'conductor'}.`} description={selectedInvoice ? 'Revisa el pedido, guarda la evidencia y confirma con el cliente.' : 'Cada pedido tiene un destino. Encuentra tu próxima entrega.'}><span className="date-chip">{new Date().toLocaleDateString('es-EC', { day: 'numeric', month: 'long' })}</span></PageIntro>

      {!selectedInvoice && (
        <InvoiceSearchPanel
          query={query}
          onQueryChange={setQuery}
          onSearchSubmit={handleSearch}
          searching={searching}
          scanning={scanning}
          scanInputRef={scanInputRef}
          onScanSelected={handleScanSelected}
          hasSearched={hasSearched}
          invoices={invoices}
          searchError={searchError}
          feedback={feedback}
          onSelectInvoice={selectInvoice}
        />
      )}

      {selectedInvoice && !showIncidentForm && (
        <ConfirmDeliveryForm
          selectedInvoice={selectedInvoice}
          onBack={resetSelection}
          lines={lines}
          linesLoading={linesLoading}
          linesError={linesError}
          checkedIds={checkedIds}
          onToggleChecked={toggleChecked}
          photoInputRef={photoInputRef}
          onPhotoSelected={handlePhotoSelected}
          photo={photo}
          photoError={photoError}
          pin={pin}
          onPinChange={setPin}
          confirming={confirming}
          allChecked={allChecked}
          canConfirm={canConfirm}
          locationState={locationState}
          feedback={feedback}
          onSubmit={handleConfirm}
          onShowIncidentForm={() => setShowIncidentForm(true)}
        />
      )}

      {selectedInvoice && showIncidentForm && (
        <IncidentForm
          selectedInvoice={selectedInvoice}
          onBack={() => setShowIncidentForm(false)}
          incidentReason={incidentReason}
          onIncidentReasonChange={setIncidentReason}
          incidentNotes={incidentNotes}
          onIncidentNotesChange={setIncidentNotes}
          incidentError={incidentError}
          reportingIncident={reportingIncident}
          onSubmit={handleReportIncident}
        />
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
