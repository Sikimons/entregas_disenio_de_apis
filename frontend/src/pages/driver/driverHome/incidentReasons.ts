export const INCIDENT_REASONS = [
  'Cliente ausente',
  'Direccion incorrecta',
  'Producto danado',
  'Cliente rechazo la entrega',
  'Otro',
] as const

export type IncidentReason = (typeof INCIDENT_REASONS)[number]
