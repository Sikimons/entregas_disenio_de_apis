// Ataca especificamente el tablero administrativo (map/metrics) con un dataset real y
// pesado (500 entregas confirmadas con foto de ~200 KB cada una, generado con
// scripts/generate_invoices.py + scripts/seed_confirmed_deliveries.py), escalando VUs
// para encontrar el punto de degradacion real que el analisis estatico del codigo
// identifico (fetch EAGER de `photo`, agregacion de metricas en memoria Java).
import http from 'k6/http'
import { check, sleep } from 'k6'
import { login } from './auth.js'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18091'

export const options = {
  scenarios: {
    dashboard_heavy: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '30s', target: 150 },
        { duration: '30s', target: 300 },
        { duration: '30s', target: 500 },
        { duration: '30s', target: 0 },
      ],
    },
  },
}

export function setup() {
  const headers = login(BASE_URL, http)
  const now = new Date()
  const from = new Date(now.getTime() - 30 * 86400000).toISOString()
  return { headers, from, to: now.toISOString() }
}

export default function (data) {
  const headers = data.headers
  const responses = http.batch([
    ['GET', `${BASE_URL}/api/v1/admin/dashboard/map?from=${data.from}&to=${data.to}`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/dashboard/metrics?from=${data.from}&to=${data.to}`, null, { headers }],
  ])
  responses.forEach((res) => check(res, { 'status is 200': (r) => r.status === 200 }))
  sleep(0.5)
}
