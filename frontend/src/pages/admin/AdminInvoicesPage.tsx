import { useEffect, useState, type FormEvent } from 'react'
import apiClient from '../../api/client'
import { Icon, PageIntro } from '../../components/Workspace'
import TableSkeleton from '../../components/TableSkeleton'
import type { AdminInvoice } from '../../types/domain'

interface ProductForm {
  description: string
  quantity: number | string
}

interface InvoiceForm {
  number: string
  partnerName: string
  deliveryAddress: string
  latitude: number | string
  longitude: number | string
  requiresPin: boolean
  products: ProductForm[]
}

const empty = (): InvoiceForm => ({ number: '', partnerName: '', deliveryAddress: '', latitude: '', longitude: '', requiresPin: true, products: [{ description: '', quantity: 1 }] })

export default function AdminInvoicesPage() {
  const [invoices, setInvoices] = useState<AdminInvoice[]>([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState<InvoiceForm>(empty)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const load = async (search = '') => {
    setLoading(true)
    setError('')
    try { setInvoices((await apiClient.get<AdminInvoice[]>('/admin/invoices', { params: { q: search } })).data) }
    catch (err: any) { setError(err.response?.data?.message || 'No se pudieron cargar las facturas.') }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])
  const field = <K extends keyof InvoiceForm>(name: K, value: InvoiceForm[K]) => setForm(prev => ({ ...prev, [name]: value }))
  const create = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await apiClient.post('/admin/invoices', { ...form, latitude: form.latitude === '' ? null : Number(form.latitude), longitude: form.longitude === '' ? null : Number(form.longitude), products: form.products.map(p => ({ ...p, quantity: Number(p.quantity) })) })
      setForm(empty()); setEditing(false); await load(query)
    } catch (err: any) { setError(err.response?.data?.message || 'Revisa los datos de la factura e intenta nuevamente.') }
    finally { setSaving(false) }
  }
  const publish = async (id: number) => {
    setSaving(true); setError('')
    try { await apiClient.post(`/admin/invoices/${id}/publish`); await load(query) }
    catch (err: any) { setError(err.response?.data?.message || 'No se pudo publicar la factura.') }
    finally { setSaving(false) }
  }
  const exportCsv = () => {
    const csv = '﻿factura;pin\r\n' + invoices.map(i => `"${String(i.number).replaceAll('"', '""')}";"${i.pin || ''}"`).join('\r\n')
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
    const link = document.createElement('a'); link.href = url; link.download = 'facturas_y_pines.csv'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
  }
  return <div>
    <PageIntro eyebrow="TUS PEDIDOS, DESDE EL ORIGEN" title="Facturas y pedidos" description="Crea el pedido, publica la factura y comparte su PIN con el cliente."><button className="primary-button" onClick={() => setEditing(!editing)}>{editing ? 'Cerrar formulario' : 'Nueva factura'}</button></PageIntro>
    {error && <p role="alert" className="error-text">{error}</p>}
    {editing && <form className="invoice-create" onSubmit={create}>
      <div className="section-heading"><h2>Nuevo pedido</h2><span>Se guardará como borrador</span></div>
      <div className="invoice-fields">
        <label>Número de factura<input required maxLength={60} value={form.number} onChange={e => field('number', e.target.value)} placeholder="001-104-0000001234" /></label>
        <label>Cliente<input required maxLength={255} value={form.partnerName} onChange={e => field('partnerName', e.target.value)} /></label>
        <label>Dirección<input maxLength={255} value={form.deliveryAddress} onChange={e => field('deliveryAddress', e.target.value)} /></label>
        <label>Latitud (opcional)<input type="number" step="any" min="-90" max="90" value={form.latitude} onChange={e => field('latitude', e.target.value)} /></label>
        <label>Longitud (opcional)<input type="number" step="any" min="-180" max="180" value={form.longitude} onChange={e => field('longitude', e.target.value)} /></label>
        <label className="requires-pin"><input type="checkbox" checked={form.requiresPin} onChange={e => field('requiresPin', e.target.checked)} />Requiere PIN de entrega</label>
      </div>
      <h3>Productos</h3>
      {form.products.map((product, index) => <div className="product-editor" key={index}>
        <label>Producto {index + 1}<input required value={product.description} onChange={e => field('products', form.products.map((p, i) => i === index ? { ...p, description: e.target.value } : p))} /></label>
        <label>Cantidad<input required type="number" min="0.01" step="any" value={product.quantity} onChange={e => field('products', form.products.map((p, i) => i === index ? { ...p, quantity: e.target.value } : p))} /></label>
        <button className="link-button danger" type="button" disabled={form.products.length === 1} onClick={() => field('products', form.products.filter((_, i) => i !== index))}>Quitar</button>
      </div>)}
      <div className="invoice-form-actions"><button className="link-button" type="button" onClick={() => field('products', [...form.products, { description: '', quantity: 1 }])}>+ Agregar producto</button><button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Guardar borrador'}</button></div>
    </form>}
    <div className="invoice-toolbar"><form className="search-form" onSubmit={e => { e.preventDefault(); load(query) }}><input aria-label="Buscar facturas" placeholder="Número de factura o cliente" value={query} onChange={e => setQuery(e.target.value)} /><button type="submit" disabled={loading}><Icon name="search" /> Buscar</button></form><button className="link-button" disabled={loading || !invoices.length} onClick={exportCsv}>Exportar facturas y PIN</button></div>
    {loading ? <TableSkeleton rows={6} columns={5} /> : <table className="data-table"><thead><tr><th>Factura</th><th>Cliente</th><th>Estado</th><th>PIN</th><th>Acción</th></tr></thead><tbody>{invoices.map(i => <tr key={i.id}><td className="mono">{i.number}</td><td>{i.partnerName}<div className="status-detail">{i.deliveryAddress}</div></td><td><span className={`status-pill ${i.confirmed ? 'success' : i.state === 'draft' ? 'warning' : ''}`}>{i.confirmed ? 'Entregada' : i.state === 'posted' ? 'Publicada' : i.state === 'draft' ? 'Borrador' : 'Cancelada'}</span></td><td className="mono">{i.pin || '—'}</td><td>{i.state === 'draft' ? <button className="link-button" disabled={saving} onClick={() => publish(i.id)}>Publicar</button> : <span className="hint-text">{i.requiresPin ? 'Entrega con PIN' : 'Sin PIN'}</span>}</td></tr>)}{!invoices.length && <tr><td colSpan={5}>No hay facturas que coincidan con la búsqueda.</td></tr>}</tbody></table>}
    <p className="hint-text">{invoices.length} facturas · Hasta 1.000 resultados por búsqueda. Los PIN solo están disponibles para administradores.</p>
  </div>
}
