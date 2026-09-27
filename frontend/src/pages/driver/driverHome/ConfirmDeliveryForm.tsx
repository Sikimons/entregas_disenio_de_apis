import type { ChangeEvent, FormEvent, RefObject } from 'react'
import { Icon } from '../../../components/Workspace'
import { PinBoxes } from './PinBoxes'
import type { Invoice, InvoiceLine } from '../../../types/domain'
import type { DeliveryFeedback } from './InvoiceSearchPanel'

export type LocationState = 'idle' | 'locating' | 'ready' | 'error'

export interface CapturedPhoto {
  dataUrl: string
  filename: string
}

function formatQuantity(quantity: number | null | undefined): string | null {
  if (quantity == null) return null
  return Number.isInteger(quantity) ? String(quantity) : quantity.toFixed(2)
}

interface ConfirmDeliveryFormProps {
  selectedInvoice: Invoice
  onBack: () => void
  lines: InvoiceLine[]
  linesLoading: boolean
  linesError: string
  checkedIds: Set<number>
  onToggleChecked: (lineId: number) => void
  photoInputRef: RefObject<HTMLInputElement | null>
  onPhotoSelected: (e: ChangeEvent<HTMLInputElement>) => void
  photo: CapturedPhoto | null
  photoError: string
  pin: string
  onPinChange: (value: string) => void
  confirming: boolean
  allChecked: boolean
  canConfirm: boolean
  locationState: LocationState
  feedback: DeliveryFeedback | null
  onSubmit: (e: FormEvent<HTMLFormElement>) => void
  onShowIncidentForm: () => void
}

export function ConfirmDeliveryForm({
  selectedInvoice,
  onBack,
  lines,
  linesLoading,
  linesError,
  checkedIds,
  onToggleChecked,
  photoInputRef,
  onPhotoSelected,
  photo,
  photoError,
  pin,
  onPinChange,
  confirming,
  allChecked,
  canConfirm,
  locationState,
  feedback,
  onSubmit,
  onShowIncidentForm,
}: ConfirmDeliveryFormProps) {
  return (
    <form className="pin-form" onSubmit={onSubmit}>
      <button type="button" className="link-button" onClick={onBack}>← Volver a buscar</button>

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
                  <input type="checkbox" checked={checked} onChange={() => onToggleChecked(line.id)} />
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
        onChange={onPhotoSelected}
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
      <PinBoxes value={pin} onChange={onPinChange} disabled={confirming} />
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
        onClick={onShowIncidentForm}
        disabled={confirming}
      >
        No se pudo entregar — reportar incidencia
      </button>
    </form>
  )
}
