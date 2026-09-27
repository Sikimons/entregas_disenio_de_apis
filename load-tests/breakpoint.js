// Busca el breakpoint de los endpoints de lectura ligeros (Fase8): a diferencia de
// sustained.js/spike.js (que se quedan cortos con 150/750 VUs, sin degradarse nunca),
// este escenario sube la tasa de llegada de peticiones sin techo hasta que
// http_req_failed o p(95) cruzan el umbral (abortOnFail), y ese punto de quiebre es el
// breakpoint real del sistema para esta mezcla de trafico, no un numero elegido a mano.
import http from 'k6/http'
import { check } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18091'

export const options = {
  scenarios: {
    breakpoint: {
      executor: 'ramping-arrival-rate',
      startRate: 500,
      timeUnit: '1s',
      preAllocatedVUs: 200,
      maxVUs: 4000,
      stages: [
        { duration: '1m', target: 500 },
        { duration: '1m', target: 1500 },
        { duration: '1m', target: 3000 },
        { duration: '1m', target: 5000 },
        { duration: '1m', target: 8000 },
        { duration: '1m', target: 10000 },
      ],
    },
  },
  thresholds: {
    // abortOnFail: en cuanto se cruza el umbral, k6 corta el escenario y reporta a que
    // tasa de llegada ocurrio -- ese es el breakpoint que "Rendimiento y pruebas de
    // carga" (§9 de EVALUACION_TECNICA.md) senalaba como no identificado.
    http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: true }],
    http_req_duration: [{ threshold: 'p(95)<500', abortOnFail: true }],
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

  // Misma mezcla de endpoints que sustained.js/spike.js, para que el breakpoint
  // encontrado aqui sea comparable con esos dos escenarios.
  const responses = http.batch([
    ['GET', `${BASE_URL}/api/v1/driver/invoices?q=001`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/deliveries?page=0&size=20`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/dashboard/map?from=${from}&to=${to}`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/dashboard/metrics?from=${from}&to=${to}`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/cost?month=${month}`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/resilience/status`, null, { headers }],
  ])

  responses.forEach((res) => check(res, { 'status is 200': (r) => r.status === 200 }))
}
