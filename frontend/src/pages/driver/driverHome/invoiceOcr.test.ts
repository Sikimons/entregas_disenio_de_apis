import { describe, expect, it } from 'vitest'
import { extractInvoiceCandidate } from './invoiceOcr'

// scanInvoiceNumber() (Tesseract + compresion + <canvas>) no se prueba aqui, por la misma
// razon que compressImage() en imageUtils.test.ts: depende de APIs de navegador que jsdom
// no implementa de verdad. extractInvoiceCandidate() es la parte determinista y con mas
// logica propia (el patron del SRI, el descarte de "autorizacion"/"clave de acceso", el
// fallback) -- justo lo que puede fallar en silencio con un documento real distinto al
// esperado.
describe('extractInvoiceCandidate', () => {
  it('reconoce el formato estricto del SRI (establecimiento-punto de emision-secuencial)', () => {
    const text = 'FACTURA\nNo. 001-104-000001234\nCliente: Juan Perez'
    expect(extractInvoiceCandidate(text)).toBe('001-104-000001234')
  })

  it('reconoce el formato con secuencial de 10 digitos', () => {
    const text = 'No. 046-101-0000005884'
    expect(extractInvoiceCandidate(text)).toBe('046-101-0000005884')
  })

  it('no confunde el numero de factura con una clave de acceso larga en la misma linea', () => {
    // Las claves de acceso del SRI son cadenas de 40+ digitos; sin el patron estricto,
    // el fallback por longitud podria preferirlas sobre el numero real de la factura.
    const text = 'Clave de acceso: 2609202601179212345600110010010000012341234567891'
    expect(extractInvoiceCandidate(text)).toBeNull()
  })

  it('descarta lineas de autorizacion y clave de acceso, pero usa el fallback en el resto del texto', () => {
    const text = [
      'Numero de autorizacion: 1234567890123456789012345678901234567890',
      'Clave de acceso: 9876543210987654321098765432109876543210',
      'Pedido: 12-345-6789',
    ].join('\n')

    expect(extractInvoiceCandidate(text)).toBe('12-345-6789')
  })

  it('con varios candidatos de fallback, prefiere el mas largo (mas parecido a un numero de factura real)', () => {
    const text = 'Ref: 12-34\nPedido: 123-456-789'
    expect(extractInvoiceCandidate(text)).toBe('123-456-789')
  })

  it('devuelve null cuando no hay ningun candidato reconocible', () => {
    expect(extractInvoiceCandidate('Gracias por su compra')).toBeNull()
  })

  it('devuelve null con texto vacio', () => {
    expect(extractInvoiceCandidate('')).toBeNull()
  })

  it('ignora un candidato de fallback demasiado largo (mas de 18 caracteres)', () => {
    // Evita que un numero de autorizacion corto (pero aun largo) se cuele como factura si
    // no menciona la palabra "autorizacion" en esa linea exacta.
    const text = 'Codigo interno: 1234567890123456789'
    expect(extractInvoiceCandidate(text)).toBeNull()
  })
})
