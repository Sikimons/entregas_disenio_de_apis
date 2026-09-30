// Humo: 2 VUs durante 30 s. No mide rendimiento; valida que el destino responde antes de
// lanzar un escenario grande (DNS/conectividad, TLS, Cloudflare, credenciales, rutas).
// Corre primero esto cuando cambies BASE_URL o vayas a probar contra una EC2 nueva.
import http from 'k6/http'
import { check, sleep } from 'k6'
import { login } from './auth.js'
import { BASE_URL, baseOptions } from './config.js'

export const options = baseOptions({
  scenarios: {
    smoke: { executor: 'constant-vus', vus: 2, duration: '30s' },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000'],
  },
})

export function setup() {
  return { headers: login(BASE_URL, http) }
}

export default function (data) {
  const headers = data.headers
  const responses = http.batch([
    ['GET', `${BASE_URL}/api/v1/admin/resilience/status`, null, { headers }],
    ['GET', `${BASE_URL}/api/v1/admin/deliveries?page=0&size=20`, null, { headers }],
  ])
  responses.forEach((res) => check(res, { 'status is 200': (r) => r.status === 200 }))
  sleep(1)
}
