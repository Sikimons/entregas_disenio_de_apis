import { describe, expect, it } from 'vitest'
import { computeResizedDimensions } from './imageUtils'

// La foto de evidencia (Fase2 §3.5) se comprime a 1280px/calidad 0.7 antes de subirla;
// esta funcion decide el tamano final. compressImage() en si (canvas/Image reales) no se
// prueba aqui: jsdom no implementa <canvas> de verdad, y probarlo con un mock de
// getContext()/toDataURL() solo verificaria que se llamaron esos mocks, no que la
// compresion funciona -- ver el comentario junto a la funcion.
describe('computeResizedDimensions', () => {
  it('no cambia el tamano si ya esta dentro del limite', () => {
    expect(computeResizedDimensions(800, 600, 1280)).toEqual({ width: 800, height: 600 })
  })

  it('reduce el ancho cuando la imagen es mas ancha que alta y excede el limite', () => {
    // 2560x1440 (16:9) -> el ancho manda, se reescala a 1280 conservando proporcion.
    expect(computeResizedDimensions(2560, 1440, 1280)).toEqual({ width: 1280, height: 720 })
  })

  it('reduce el alto cuando la imagen es mas alta que ancha y excede el limite', () => {
    // 1440x2560 (retrato, tipico de una foto tomada con el celular en vertical).
    expect(computeResizedDimensions(1440, 2560, 1280)).toEqual({ width: 720, height: 1280 })
  })

  it('una imagen cuadrada usa la rama de "height > maxDimension" (ancho == alto no entra en la primera condicion)', () => {
    expect(computeResizedDimensions(2000, 2000, 1280)).toEqual({ width: 1280, height: 1280 })
  })

  it('respeta un maxDimension distinto al default (usado por el OCR: 1600)', () => {
    expect(computeResizedDimensions(3200, 1600, 1600)).toEqual({ width: 1600, height: 800 })
  })

  it('no altera una imagen mas ancha que el limite pero con menos alto que el limite', () => {
    // Caso borde: width > maxDimension pero width <= height (no deberia entrar en la
    // primera rama, que exige width > height Y width > maxDimension).
    expect(computeResizedDimensions(100, 2000, 1280)).toEqual({ width: 64, height: 1280 })
  })
})
