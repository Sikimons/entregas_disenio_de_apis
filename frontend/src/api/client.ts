import axios, { type AxiosError } from 'axios'

// Migracion de JWT en localStorage/sessionStorage a cookie HttpOnly (auditoria tecnica,
// hallazgo P1): el backend (AuthController) ya no devuelve el token en el cuerpo de
// /auth/login, lo establece via Set-Cookie HttpOnly. Ya no hay interceptor de request
// que adjunte "Authorization: Bearer ..." -- el navegador manda la cookie solo.
//
// "withCredentials" es obligatorio para que el navegador envie esa cookie: en produccion
// (Fase2 §3.6, "Escenario A") la PWA y la API viven en dominios distintos, asi que toda
// peticion es cross-site de verdad. "withXSRFToken" hace que axios lea la cookie
// "XSRF-TOKEN" (no HttpOnly, la escribe SecurityConfig via CookieCsrfTokenRepository) y la
// reenvie como header "X-XSRF-TOKEN" -- el mismo par de nombres que Spring Security espera
// por defecto, cero configuracion adicional de un lado ni del otro.
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Ya no hay "token" que borrar (vive en una cookie HttpOnly que este JS no puede
      // tocar); solo se limpia el perfil cacheado para la UI. Si la cookie sigue siendo
      // invalida, el backend seguira respondiendo 401 hasta que se inicie sesion de nuevo.
      localStorage.removeItem('user')
      sessionStorage.removeItem('user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient
