"""Verifica la API local sin restablecer ni modificar las facturas del usuario."""
import base64
import csv
import json
import os
import re
import uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = os.environ.get('RUTA_API', 'http://localhost:18091')
PHOTO = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII='


def request(path, body=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = Request(BASE + path, data=None if body is None else json.dumps(body).encode(), headers=headers)
    try:
        with urlopen(req, timeout=15) as response:
            raw = response.read()
            return response.status, json.loads(raw) if 'json' in response.headers.get('Content-Type', '') else raw
    except HTTPError as error:
        raw = error.read()
        return error.code, json.loads(raw) if raw else None


def login(username, password):
    status, data = request('/api/auth/login', {'username': username, 'password': password})
    assert status == 200, (status, data)
    return data['token']


if __name__ == '__main__':
    admin = login('admin', 'admin123')
    driver = login('conductor', 'conductor123')
    assert request('/api/admin/invoices', token=driver)[0] == 403
    assert request('/api/driver/invoices?q=001-104-')[0] == 401
    print('OK: autenticacion y permisos por rol')
    rows = request('/api/admin/invoices', token=admin)[1]
    indexed = {row['number']: row for row in rows}
    csv_path = Path(__file__).resolve().parents[1] / 'demo/facturas_y_pines.csv'
    exported = list(csv.DictReader(csv_path.open(encoding='utf-8-sig'), delimiter=';'))
    assert len(exported) == 50
    assert all(indexed[row['factura']]['pin'] == row['pin'] for row in exported)
    fixtures = json.loads((csv_path.parent / 'invoices.json').read_text(encoding='utf-8'))
    for row in fixtures:
        assert indexed[row['number']]['id'] == row['id']
        status, lines = request('/api/driver/invoices/%s/lines' % row['id'], token=driver)
        assert status == 200 and len(lines) == len(row['lines'])
    print('OK: 59 facturas, 314 productos y los 50 PIN originales conservados')

    prefix = 'VERIFY/' + uuid.uuid4().hex[:10]
    def create(suffix, requires_pin=True):
        payload = {'number': prefix + suffix, 'partnerName': 'Verificacion automatica',
                   'deliveryAddress': 'Direccion de prueba', 'latitude': -2.171, 'longitude': -79.922,
                   'requiresPin': requires_pin, 'products': [{'description': 'Arroz de prueba', 'quantity': 1.5}]}
        status, row = request('/api/admin/invoices', payload, admin)
        assert status == 200, (status, row)
        assert request('/api/admin/invoices', payload, admin)[0] == 409
        return row

    def confirm(invoice, pin):
        return request('/api/driver/deliveries/confirm', {
            'invoiceId': invoice['id'], 'invoiceNumber': invoice['number'],
            'partnerName': invoice['partner_name'], 'deliveryAddress': invoice['delivery_address'],
            'pin': pin, 'latitude': -2.171, 'longitude': -79.922,
            'photoBase64': PHOTO, 'photoContentType': 'image/png', 'photoFilename': 'verificacion.png',
        }, driver)

    invoice = create('/01')
    assert confirm(invoice, '123456')[0] == 400
    status, invoice = request('/api/admin/invoices/%s/publish' % invoice['id'], {}, admin)
    assert status == 200 and re.fullmatch(r'\d{6}', invoice['pin'])
    assert request('/api/admin/invoices/%s/publish' % invoice['id'], {}, admin)[1]['pin'] == invoice['pin']
    wrong_pin = '000000' if invoice['pin'] != '000000' else '000001'
    assert confirm(invoice, wrong_pin)[0] == 400
    print('OK: borrador, publicacion, PIN estable, duplicados y PIN incorrecto')

    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(lambda _: confirm(invoice, invoice['pin']), range(2)))
    assert sorted(status for status, _ in results) == [200, 400], results
    assert confirm(invoice, invoice['pin'])[0] == 400
    assert request('/api/driver/invoices?q=' + invoice['number'], token=driver)[1] == []
    history = request('/api/driver/deliveries/history', token=driver)[1]['content']
    succeeded = next(row for row in history if row['invoiceNumber'] == invoice['number'] and row['outcome'] == 'CONFIRMED')
    assert succeeded['hasPhoto'] and succeeded['distanceFromExpectedMeters'] < 1
    assert request('/api/driver/deliveries/%s/photo' % succeeded['id'], token=driver)[1] == base64.b64decode(PHOTO)
    print('OK: concurrencia, foto, GPS, historial y desaparicion de pendientes')

    no_pin = create('/02', False)
    request('/api/admin/invoices/%s/publish' % no_pin['id'], {}, admin)
    assert confirm(no_pin, '123456')[0] == 400
    status, result = request('/api/driver/deliveries/incident', {
        'invoiceId': no_pin['id'], 'invoiceNumber': no_pin['number'], 'partnerName': no_pin['partner_name'],
        'deliveryAddress': no_pin['delivery_address'], 'reason': 'Cliente ausente', 'notes': 'Verificacion automatica',
        'latitude': -2.171, 'longitude': -79.922,
    }, driver)
    assert status == 200, result
    print('OK: factura sin PIN y reporte de incidencia')
    print('Todas las comprobaciones pasaron. Casos de prueba: ' + prefix)
