interface TableSkeletonProps {
  rows?: number
  columns?: number
  widths?: string[]
}

/**
 * Placeholder animado para tablas del admin mientras cargan datos, en vez de
 * un simple texto "Cargando...". `widths` permite variar el ancho de cada
 * columna para que el skeleton se parezca a la forma real de los datos.
 */
export default function TableSkeleton({ rows = 5, columns = 4, widths }: TableSkeletonProps) {
  return (
    <table className="data-table skeleton-table">
      <tbody>
        {Array.from({ length: rows }).map((_, rowIndex) => (
          <tr key={rowIndex}>
            {Array.from({ length: columns }).map((_, colIndex) => (
              <td key={colIndex}>
                <span
                  className="skeleton-bar"
                  style={{ width: widths?.[colIndex] || '70%' }}
                />
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
