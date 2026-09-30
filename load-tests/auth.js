// Helper de login compartido por los 4 escenarios (auditoria tecnica, Tanda 4: encontrado
// ejecutando estos scripts de verdad contra el stack local, no solo leyendolos -- todos
// asumian el esquema Bearer/localStorage que la Tanda 1 reemplazo por cookie HttpOnly, y
// la contrasena de admin de ejemplo "admin123" en vez de la real de deploy/env/local.env).
//
// El JWT ya no viaja en el cuerpo de la respuesta (LoginResponse ya no tiene "token"): hay
// que leerlo de la cookie "access_token" que el login establece via Set-Cookie, y reenviarlo
// como header Cookie en cada peticion (k6 no comparte el cookie jar de setup() con las
// iteraciones de cada VU). Ningun escenario de load-tests/ escribe datos (solo GET, ver los
// README de cada uno), asi que no hace falta el token CSRF -- eso solo protege escrituras.
export function login(baseUrl, http) {
  const username = __ENV.RUTA_ADMIN || 'admin'
  const password = __ENV.RUTA_PASSWORD
  if (!password) {
    throw new Error('Falta RUTA_PASSWORD (contrasena del admin del entorno que se prueba).')
  }
  const res = http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ username, password, remember: true }),
    { headers: { 'Content-Type': 'application/json' } }
  )
  if (res.status !== 200) {
    throw new Error(`No se pudo autenticar en setup(): HTTP ${res.status} ${res.body}`)
  }
  const cookie = res.cookies.access_token && res.cookies.access_token[0]
  if (!cookie) {
    throw new Error('El login no devolvio la cookie access_token (revisar AuthController.setAuthCookie()).')
  }
  return { Cookie: `access_token=${cookie.value}` }
}
