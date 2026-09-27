import { PageIntro } from '../../components/Workspace'
import { CostSection } from './dashboard/CostSection'
import { DashboardMapSection } from './dashboard/DashboardMapSection'
import { ResilienceSection } from './dashboard/ResilienceSection'

// Dividido en tres secciones independientes (auditoria tecnica, Tanda 3: este archivo
// mezclaba mapa, costo y resiliencia en 391 lineas con estado de las tres cosas entrelazado).
// Cada seccion es duena de su propio estado y su propia carga de datos (useAsyncData); no
// comparten nada entre si, asi que separarlas no requirio inventar props ni contexto nuevo.
export default function AdminDashboardPage() {
  return (
    <div>
      <PageIntro eyebrow="CENTRO DE OPERACIONES" title="Todo en perspectiva." description="Sigue la actividad de tu equipo y cada entrega en el mapa." />
      <DashboardMapSection />
      <CostSection />
      <ResilienceSection />
    </div>
  )
}
