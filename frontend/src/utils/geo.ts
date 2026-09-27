const EARTH_RADIUS_METERS = 6371000

// Formula de Haversine. Duplica intencionalmente GeoLocation.distanceMetersTo del backend
// (backend/src/main/java/.../domain/model/GeoLocation.java): el frontend necesita avisar
// "estas lejos de la direccion registrada" antes de enviar la confirmacion, sin esperar
// una respuesta del servidor. El backend vuelve a calcular la distancia real al confirmar.
export function distanceMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180
  const dLat = toRad(lat2 - lat1)
  const dLon = toRad(lon2 - lon1)
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2
  return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}
