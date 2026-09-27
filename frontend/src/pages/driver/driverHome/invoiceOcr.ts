import { compressImage } from './imageUtils'

// Formato de numero de factura del SRI (establecimiento-punto de emision-secuencial), ej. "046-101-000005884".
const INVOICE_NUMBER_PATTERN = /(?<!\d)\d{3}-\d{3}-\d{9,10}(?!\d)/
// Longitud maxima de un candidato de respaldo (17 = "NNN-NNN-NNNNNNNNN").
// Evita confundir el numero de factura con el numero de autorizacion o la
// clave de acceso del SRI, que son cadenas de 40+ digitos en el mismo documento.
const MAX_FALLBACK_LENGTH = 18

// Extrae el numero de factura de un texto OCR, priorizando el formato del SRI.
export function extractInvoiceCandidate(text: string): string | null {
  const strictMatch = text.match(INVOICE_NUMBER_PATTERN)
  if (strictMatch) return strictMatch[0]

  // Las lineas de "numero de autorizacion" y "clave de acceso" tambien traen
  // secuencias largas de digitos; se descartan para no confundirlas con la factura.
  const relevantText = text
    .split('\n')
    .filter((line) => !/autorizaci|clave de acceso/i.test(line))
    .join('\n')

  const matches = relevantText.match(/\d[\d-]{5,}\d/g)
  if (!matches || matches.length === 0) return null

  const candidates = matches.filter((m) => m.length <= MAX_FALLBACK_LENGTH)
  if (candidates.length === 0) return null
  return candidates.sort((a, b) => b.length - a.length)[0]
}

// Recorta y escanea la foto de la factura con Tesseract (cargado en diferido), devolviendo
// el numero de factura detectado o null si no se pudo leer nada util.
export async function scanInvoiceNumber(file: File): Promise<string | null> {
  const dataUrl = await compressImage(file, 1600, 0.85)
  const { default: Tesseract } = await import('tesseract.js')
  // Servidos localmente (public/tesseract-assets) en vez del CDN por defecto de
  // tesseract.js: algunas redes moviles/corporativas bloquean cdn.jsdelivr.net y
  // devuelven una pagina HTML de error, lo que rompe el worker/wasm en runtime.
  const { data } = await Tesseract.recognize(dataUrl, 'eng', {
    workerPath: '/tesseract-assets/worker.min.js',
    corePath: '/tesseract-assets',
    langPath: '/tesseract-assets',
  })
  return extractInvoiceCandidate(data.text)
}
