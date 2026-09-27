// Carga sostenida: 0 -> 150 VUs en 3 min, meseta de 150 VUs por 7 min, bajada a 0 en 2 min.
// Contra endpoints de lectura reales (busqueda de facturas, historial, tablero, costo,
// estado del circuit breaker). No incluye POST /api/v1/driver/deliveries/confirm: cada
// factura solo se puede confirmar una vez (idempotencia real del dominio), asi que no
// hay forma de repetirlo miles de veces sin fabricar facturas nuevas por iteracion; se
// deja fuera en vez de fingir que se probo.
import http from 'k6/http'
import { check, sleep } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18091'

export const options = {
  scenarios: {
    sustained: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '3m', target: 150 },
        { duration: '7m', target: 150 },
        { duration: '2m', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
}

export function setup() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: 'admin', password: 'admin123' }),
    { headers: { 'Content-Type': 'application/json' } }
  )
  if (res.status !== 200) {
    throw new Error(`No se pudo autenticar en setup(): HTTP ${res.status} ${res.body}`)
  }
  return { token: res.json('token') }
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` }
  const now = new Date()
  const from = new Date(now.getTime() - 30 * 86400000).toISOString()
  const to = now.toISOString()
  const month = now.toISOString().slice(0, 7)

  const responses = http.batch([
    ['GET', `${BASE_URL}/api/v1/driver/invoices?q=001`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/deliveries?page=0&size=20`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/dashboard/map?from=${from}&to=${to}`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/dashboard/metrics?from=${from}&to=${to}`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/cost?month=${month}`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/resilience/status`, null, { headers }],
  ])

  responses.forEach((res) => check(res, { 'status is 200': (r) => r.status === 200 }))
  sleep(1)
}
