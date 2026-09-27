import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { registerSW } from 'virtual:pwa-register'
import './theme.css'
import App from './App'

// Cuando se detecta una version nueva desplegada, la PWA recarga sola en cuanto
// el nuevo service worker termina de activarse (sin pedirle confirmacion al conductor).
const updateSW = registerSW({
  immediate: true,
  onNeedRefresh() { updateSW(true) },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
