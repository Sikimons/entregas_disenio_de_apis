import { describe, expect, it } from 'vitest'
import { distanceMeters } from './geo'

// DriverHomePage.tsx usa esto para advertir "estas a X km de la direccion registrada"
// antes de dejar avanzar la confirmacion (Fase2 §3.5, umbral SUSPICIOUS_DISTANCE_METERS).
// Duplica a proposito GeoLocation.distanceMetersTo del backend -- el backend vuelve a
// calcular la distancia real al confirmar, esto es solo una advertencia de UX temprana.
describe('distanceMeters', () => {
  it('la distancia de un punto a si mismo es cero', () => {
    expect(distanceMeters(-2.170998, -79.922359, -2.170998, -79.922359)).toBeCloseTo(0, 6)
  })

  it('calcula correctamente una distancia conocida (~1.11 km por grado de latitud en el ecuador)', () => {
    const distance = distanceMeters(0, 0, 1, 0)
    expect(distance).toBeCloseTo(111_195, -2) // tolerancia de ~100m sobre el resultado exacto de Haversine
  })

  it('es simetrica (A a B == B a A)', () => {
    const aToB = distanceMeters(-2.17, -79.92, -2.18, -79.93)
    const bToA = distanceMeters(-2.18, -79.93, -2.17, -79.92)
    expect(aToB).toBeCloseTo(bToA, 9)
  })

  it('coincide con la formula de Haversine del backend para el ejemplo del README (Guayaquil)', () => {
    // Mismas coordenadas que usa README.md como ejemplo de creacion de factura.
    const nearby = distanceMeters(-2.170998, -79.922359, -2.171998, -79.922359)
    // ~1/1000 de grado de latitud ~= 111 metros.
    expect(nearby).toBeGreaterThan(100)
    expect(nearby).toBeLessThan(120)
  })
})
