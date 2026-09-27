import type { ChangeEvent, FormEvent, RefObject } from 'react'
import { Icon } from '../../../components/Workspace'
import type { Invoice } from '../../../types/domain'

export interface DeliveryFeedback {
  type: 'success' | 'warning' | 'error'
  message: string
}

interface InvoiceSearchPanelProps {
  query: string
  onQueryChange: (value: string) => void
  onSearchSubmit: (e: FormEvent<HTMLFormElement>) => void
  searching: boolean
  scanning: boolean
  scanInputRef: RefObject<HTMLInputElement | null>
  onScanSelected: (e: ChangeEvent<HTMLInputElement>) => void
  hasSearched: boolean
  invoices: Invoice[]
  searchError: string
  feedback: DeliveryFeedback | null
  onSelectInvoice: (invoice: Invoice) => void
}

export function InvoiceSearchPanel({
  query,
  onQueryChange,
  onSearchSubmit,
  searching,
  scanning,
  scanInputRef,
  onScanSelected,
  hasSearched,
  invoices,
  searchError,
  feedback,
  onSelectInvoice,
}: InvoiceSearchPanelProps) {
  return (
    <>
      <section className="search-panel">
        <div className="search-panel-heading"><span className="surface-icon"><Icon name="search" size={24} /></span><div><h2>Encuentra un pedido</h2><p>Busca por número de factura o nombre del cliente.</p></div></div>
        <form className="search-form" onSubmit={onSearchSubmit}>
          <input
            type="text"
            aria-label="Número de factura o cliente"
            placeholder="N° de factura o cliente"
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
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
          onChange={onScanSelected}
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
            <button className="invoice-open" onClick={() => onSelectInvoice(invoice)}>
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
  )
}
