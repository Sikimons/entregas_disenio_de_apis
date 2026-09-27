/**
 * Tipos de dominio del frontend, reflejando los DTO reales del backend
 * (backend/src/main/java/.../infrastructure/adapter/in/web/dto/*.java). Se agregan
 * al migrar a TypeScript (Fase9): antes viajaban como objetos sin tipar entre
 * `apiClient` y las paginas.
 */

export type Role = 'ADMIN' | 'CONDUCTOR'

/** Ver LoginResponse.java. Lo que se guarda en localStorage/sessionStorage tras el login. */
export interface User {
  username: string
  fullName: string
  role: Role
}

/** Ver DriverResponse.java. */
export interface Driver {
  id: number
  username: string
  fullName: string
  role: Role
  active: boolean
}

/** Ver InvoiceResponse.java (busqueda del conductor, sin PIN). */
export interface Invoice {
  id: number
  number: string
  partnerName: string
  deliveryAddress: string | null
  invoiceDate: string
  state: 'draft' | 'posted' | 'cancel'
  expectedLatitude: number | null
  expectedLongitude: number | null
}

/** Ver AdminInvoiceResponse.java (panel admin, incluye PIN y confirmacion). */
export interface AdminInvoice extends Invoice {
  requiresPin: boolean
  pin: string | null
  confirmed: boolean
  /** Username de quien creo/publico la factura (Tanda 2, auditoria tecnica); null en facturas anteriores a esta columna. */
  createdBy: string | null
  publishedBy: string | null
}

/** Ver InvoiceLineResponse.java. */
export interface InvoiceLine {
  id: number
  description: string
  quantity: number
}

export type DeliveryOutcome = 'CONFIRMED' | 'REJECTED' | 'INCIDENT'

/** Ver DeliveryAttemptResponse.java. */
export interface DeliveryAttempt {
  id: number
  invoiceId: number
  invoiceNumber: string
  partnerName: string | null
  deliveryAddress: string | null
  driverName: string
  outcome: DeliveryOutcome
  latitude: number | null
  longitude: number | null
  detail: string | null
  hasPhoto: boolean
  distanceFromExpectedMeters: number | null
  createdAt: string
}

/** Ver DriverMetricResponse.java. */
export interface DriverMetric {
  driverName: string
  confirmed: number
  rejected: number
  incident: number
  total: number
}

/** Ver OperationalCostResponse.java (showback, Fase1 §4.5). */
export interface OperationalCost {
  month: string
  confirmedDeliveries: number
  evidencePhotoBytes: number
  evidenceStorageGb: number
  infrastructureCostUsd: number
  storageCostUsd: number
  supportHours: number
  supportCostUsd: number
  totalCostUsd: number
  costPerDeliveryUsd: number | null
}

/** Ver CircuitBreakerStatusResponse.java (Circuit Breaker + Retry, Fase8). */
export interface CircuitBreakerStatus {
  state: 'CLOSED' | 'OPEN' | 'HALF_OPEN'
  enabled: boolean
  pendingSimulatedFailures: number
  failureRate: number
  numberOfSuccessfulCalls: number
  numberOfFailedCalls: number
  numberOfNotPermittedCalls: number
  retryEnabled: boolean
  retrySuccessfulCallsWithoutRetry: number
  retrySuccessfulCallsWithRetry: number
  retryFailedCallsWithRetry: number
}

/** Ver PageResponse.java. */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
