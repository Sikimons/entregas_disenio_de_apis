const MAX_PHOTO_DIMENSION = 1280
const PHOTO_QUALITY = 0.7

export interface Dimensions {
  width: number
  height: number
}

/**
 * Calcula el tamano final manteniendo la proporcion, sin superar maxDimension en ningun
 * lado (RA4, docs/EVALUACION_TECNICA.md §18.3/§21/§22: extraida de compressImage() para
 * poder probarla sin depender de <canvas>/Image, que jsdom no implementa de verdad).
 */
export function computeResizedDimensions(width: number, height: number, maxDimension: number): Dimensions {
  if (width > height && width > maxDimension) {
    return { width: maxDimension, height: Math.round((height * maxDimension) / width) }
  }
  if (height > maxDimension) {
    return { width: Math.round((width * maxDimension) / height), height: maxDimension }
  }
  return { width, height }
}

export function compressImage(file: File, maxDimension = MAX_PHOTO_DIMENSION, quality = PHOTO_QUALITY): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('No se pudo leer la imagen.'))
    reader.onload = () => {
      const img = new Image()
      img.onerror = () => reject(new Error('No se pudo procesar la imagen.'))
      img.onload = () => {
        const { width, height } = computeResizedDimensions(img.width, img.height, maxDimension)
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        canvas.getContext('2d')!.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', quality))
      }
      img.src = reader.result as string
    }
    reader.readAsDataURL(file)
  })
}
