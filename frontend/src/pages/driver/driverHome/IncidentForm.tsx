import type { FormEvent } from 'react'
import type { Invoice } from '../../../types/domain'
import { INCIDENT_REASONS } from './incidentReasons'

interface IncidentFormProps {
  selectedInvoice: Invoice
  onBack: () => void
  incidentReason: string
  onIncidentReasonChange: (value: string) => void
  incidentNotes: string
  onIncidentNotesChange: (value: string) => void
  incidentError: string
  reportingIncident: boolean
  onSubmit: (e: FormEvent<HTMLFormElement>) => void
}

export function IncidentForm({
  selectedInvoice,
  onBack,
  incidentReason,
  onIncidentReasonChange,
  incidentNotes,
  onIncidentNotesChange,
  incidentError,
  reportingIncident,
  onSubmit,
}: IncidentFormProps) {
  return (
    <form className="pin-form" onSubmit={onSubmit}>
      <button type="button" className="link-button" onClick={onBack}>
        ← Volver a la entrega
      </button>

      <div className="ticket-header">
        <div className="ticket-code">{selectedInvoice.number}</div>
        <h2 className="ticket-partner">{selectedInvoice.partnerName}</h2>
      </div>

      <p className="pin-section-label">Motivo</p>
      <select aria-label="Motivo de la incidencia" value={incidentReason} onChange={(e) => onIncidentReasonChange(e.target.value)}>
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
        onChange={(e) => onIncidentNotesChange(e.target.value)}
        placeholder="Detalle adicional para el administrador..."
      />

      {incidentError && <p role="alert" className="error-text">{incidentError}</p>}

      <button type="submit" disabled={reportingIncident}>
        {reportingIncident ? 'Enviando...' : 'Enviar reporte'}
      </button>
    </form>
  )
}
