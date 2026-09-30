"""Verifica la API (local, staging o produccion) sin restablecer ni modificar las facturas
existentes: crea sus propias facturas 'VERIFY/...' y confirma entregas sobre ellas, o sea
que ESCRIBE datos en el entorno destino.

Variables de entorno (load-tests/run.sh verify <env> las llena desde el .env):
  RUTA_API               URL base (por defecto http://localhost:8080)
  RUTA_ADMIN / RUTA_PASSWORD               admin del entorno
  RUTA_DRIVER / RUTA_DRIVER_PASSWORD       conductor (por defecto conductor / conductor123)
  RUTA_INSECURE_TLS=1    no verifica el certificado (ir directo a la EC2 con el Origin
                         Certificate de Cloudflare, que no es publico)
  RUTA_CHECK_FIXTURES=0  omite la comprobacion de los datos del seed demo (59 facturas, 50
                         PIN); usalo en entornos que no se sembraron con demo/invoices.json
"""
import base64
import csv
import http.cookiejar
import json
import os
import re
import ssl
import uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import HTTPCookieProcessor, HTTPSHandler, Request, build_opener, urlopen

BASE = os.environ.get('RUTA_API', 'http://localhost:8080').rstrip('/')
CHECK_FIXTURES = os.environ.get('RUTA_CHECK_FIXTURES', '1') != '0'
# Cloudflare bloquea por defecto el User-Agent "Python-urllib"; uno propio lo identifica.
USER_AGENT = 'RutaVerify/1.0'
if os.environ.get('RUTA_INSECURE_TLS') == '1':
    TLS_CONTEXT = ssl.create_default_context()
    TLS_CONTEXT.check_hostname = False
    TLS_CONTEXT.verify_mode = ssl.CERT_NONE
else:
    TLS_CONTEXT = None
PHOTO = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII='


def _send(opener, path, body, csrf_token):
    headers = {'Content-Type': 'application/json', 'User-Agent': USER_AGENT}
    if csrf_token:
        # SecurityConfig exige este header en toda escritura desde que el JWT paso de
        # header Authorization a cookie HttpOnly (Tanda 1, auditoria tecnica): sin el, un
        # POST/PUT/DELETE autenticado responde 403 aunque la cookie de sesion sea valida.
        headers['X-XSRF-TOKEN'] = csrf_token
    req = Request(BASE + path, data=None if body is None else json.dumps(body).encode(), headers=headers)
    try:
        with opener.open(req, timeout=15) as response:
            raw = response.read()
            return response.status, json.loads(raw) if 'json' in response.headers.get('Content-Type', '') else raw
    except HTTPError as error:
        raw = error.read()
        return error.code, json.loads(raw) if raw else None


class Session:
    """Identidad autenticada: cookie de sesion (HttpOnly, la maneja sola el CookieJar) +
    token CSRF (Tanda 1, auditoria tecnica -- antes 'login' devolvia el JWT como string y
    cada llamada lo mandaba a mano en 'Authorization: Bearer'; ahora ese string ya no
    existe en la respuesta, y las escrituras necesitan reenviar el token CSRF que la
    propia cookie de sesion trae)."""

    def __init__(self):
        self._cookiejar = http.cookiejar.CookieJar()
        self._opener = build_opener(HTTPCookieProcessor(self._cookiejar), HTTPSHandler(context=TLS_CONTEXT))

    def request(self, path, body=None):
        csrf_token = next((c.value for c in self._cookiejar if c.name == 'XSRF-TOKEN'), None)
        return _send(self._opener, path, body, csrf_token)


def request(path, body=None, token=None):
    """'token' es ahora una Session (ver login()), no un string de JWT -- se mantiene el
    nombre del parametro para no tener que tocar cada llamada existente en este archivo y
    en generate_invoices.py/seed_confirmed_deliveries.py, que solo pasan el valor que
    login() les dio."""
    if token is not None:
        return token.request(path, body)
    try:
        req = Request(BASE + path, headers={'Content-Type': 'application/json', 'User-Agent': USER_AGENT})
        with urlopen(req, timeout=15, context=TLS_CONTEXT) as response:
            raw = response.read()
            return response.status, json.loads(raw) if 'json' in response.headers.get('Content-Type', '') else raw
    except HTTPError as error:
        raw = error.read()
        return error.code, json.loads(raw) if raw else None


def login(username, password):
    session = Session()
    status, data = session.request('/api/v1/auth/login', {'username': username, 'password': password, 'remember': True})
    assert status == 200, (status, data)
    return session


def fetch_all_invoices(token, q=''):
    """GET /api/v1/admin/invoices ahora pagina (tamano maximo 100, N2/RA3,
    docs/EVALUACION_TECNICA.md §21/§22): recorre todas las paginas del resultado."""
    page, rows = 0, []
    while True:
        status, body = request('/api/v1/admin/invoices?q=%s&page=%d&size=100' % (q, page), token=token)
        assert status == 200, (status, body)
        rows.extend(body['content'])
        if page + 1 >= body['totalPages']:
            return rows
        page += 1


if __name__ == '__main__':
    admin = login(os.environ.get('RUTA_ADMIN') or 'admin', os.environ.get('RUTA_PASSWORD') or 'local_only_admin_password_change_me')
    driver = login(os.environ.get('RUTA_DRIVER') or 'conductor', os.environ.get('RUTA_DRIVER_PASSWORD') or 'conductor123')
    assert request('/api/v1/admin/invoices', token=driver)[0] == 403
    assert request('/api/v1/driver/invoices?q=001-104-')[0] == 401
    print('OK: autenticacion y permisos por rol')
    # Solo tiene sentido si el entorno se sembro con demo/invoices.json (RUTA_CHECK_FIXTURES=0 lo omite).
    if CHECK_FIXTURES:
        rows = fetch_all_invoices(admin)
        indexed = {row['number']: row for row in rows}
        csv_path = Path(__file__).resolve().parents[1] / 'demo/facturas_y_pines.csv'
        exported = list(csv.DictReader(csv_path.open(encoding='utf-8-sig'), delimiter=';'))
        assert len(exported) == 50
        assert all(indexed[row['factura']]['pin'] == row['pin'] for row in exported)
        fixtures = json.loads((csv_path.parent / 'invoices.json').read_text(encoding='utf-8'))
        for row in fixtures:
            assert indexed[row['number']]['id'] == row['id']
            # /driver/invoices/{id}/lines ya no expone lineas de facturas que no esten
            # publicadas (N7, docs/EVALUACION_TECNICA.md §18): antes un conductor podia leer
            # el contenido de una factura en borrador con solo adivinar su id. Los fixtures de
            # demo incluyen a proposito 2 borradores y 1 cancelada (para el panel admin);
            # para esas, solo se confirma el rechazo, no el conteo de productos.
            status, lines = request('/api/v1/driver/invoices/%s/lines' % row['id'], token=driver)
            if row['state'] == 'posted':
                assert status == 200 and len(lines) == len(row['lines'])
            else:
                assert status == 400
        print('OK: 59 facturas, 314 productos y los 50 PIN originales conservados (borradores y cancelada ya no exponen sus lineas al conductor)')
    else:
        print('OMITIDO: comprobacion de los datos del seed demo (RUTA_CHECK_FIXTURES=0)')

    prefix = 'VERIFY/' + uuid.uuid4().hex[:10]
    def create(suffix, requires_pin=True):
        payload = {'number': prefix + suffix, 'partnerName': 'Verificacion automatica',
                   'deliveryAddress': 'Direccion de prueba', 'latitude': -2.171, 'longitude': -79.922,
                   'requiresPin': requires_pin, 'products': [{'description': 'Arroz de prueba', 'quantity': 1.5}]}
        status, row = request('/api/v1/admin/invoices', payload, admin)
        assert status == 201, (status, row)
        assert request('/api/v1/admin/invoices', payload, admin)[0] == 409
        return row

    def confirm(invoice, pin):
        return request('/api/v1/driver/deliveries/confirm', {
            'invoiceId': invoice['id'], 'invoiceNumber': invoice['number'],
            'partnerName': invoice['partnerName'], 'deliveryAddress': invoice['deliveryAddress'],
            'pin': pin, 'latitude': -2.171, 'longitude': -79.922,
            'photoBase64': PHOTO, 'photoContentType': 'image/png', 'photoFilename': 'verificacion.png',
        }, driver)

    invoice = create('/01')
    assert confirm(invoice, '123456')[0] == 400
    status, invoice = request('/api/v1/admin/invoices/%s/publish' % invoice['id'], {}, admin)
    assert status == 200 and re.fullmatch(r'\d{6}', invoice['pin'])
    assert request('/api/v1/admin/invoices/%s/publish' % invoice['id'], {}, admin)[1]['pin'] == invoice['pin']
    wrong_pin = '000000' if invoice['pin'] != '000000' else '000001'
    assert confirm(invoice, wrong_pin)[0] == 422  # PIN invalido: 422 Unprocessable Entity
    print('OK: borrador, publicacion, PIN estable, duplicados y PIN incorrecto')

    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(lambda _: confirm(invoice, invoice['pin']), range(2)))
    assert sorted(status for status, _ in results) == [200, 409], results  # la perdedora: 409 (ya confirmada)
    assert confirm(invoice, invoice['pin'])[0] == 409
    assert request('/api/v1/driver/invoices?q=' + invoice['number'], token=driver)[1] == []
    history = request('/api/v1/driver/deliveries/history', token=driver)[1]['content']
    succeeded = next(row for row in history if row['invoiceNumber'] == invoice['number'] and row['outcome'] == 'CONFIRMED')
    assert succeeded['hasPhoto'] and succeeded['distanceFromExpectedMeters'] < 1
    assert request('/api/v1/driver/deliveries/%s/photo' % succeeded['id'], token=driver)[1] == base64.b64decode(PHOTO)
    print('OK: concurrencia, foto, GPS, historial y desaparicion de pendientes')

    no_pin = create('/02', False)
    request('/api/v1/admin/invoices/%s/publish' % no_pin['id'], {}, admin)
    assert confirm(no_pin, '123456')[0] == 400
    status, result = request('/api/v1/driver/deliveries/incident', {
        'invoiceId': no_pin['id'], 'invoiceNumber': no_pin['number'], 'partnerName': no_pin['partnerName'],
        'deliveryAddress': no_pin['deliveryAddress'], 'reason': 'Cliente ausente', 'notes': 'Verificacion automatica',
        'latitude': -2.171, 'longitude': -79.922,
    }, driver)
    assert status == 200, result
    print('OK: factura sin PIN y reporte de incidencia')
    print('Todas las comprobaciones pasaron. Casos de prueba: ' + prefix)
