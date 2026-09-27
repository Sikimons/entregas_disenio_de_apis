"""Confirma un lote de facturas ya publicadas con fotos de tamano realista (~200 KB),
para poblar delivery_log con filas pesadas de verdad y poder medir el rendimiento
real del tablero administrativo (GET /api/v1/admin/dashboard/map|metrics).

No usa Pillow (no esta instalado en el entorno): construye bytes que pasan la
validacion de DeliveryPhoto.decodedBytes() (firma PNG real + tamano real), sin ser
una imagen renderizable - alcanza para medir el tamano de columna/consulta real.

Uso:
    python scripts/seed_confirmed_deliveries.py --count 500
"""
import argparse
import base64
import os
import random
from verify_delivery import request, login, fetch_all_invoices

PNG_SIGNATURE = bytes([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A])


def fake_photo_base64(size_bytes=200_000):
    payload = PNG_SIGNATURE + os.urandom(size_bytes - len(PNG_SIGNATURE))
    return base64.b64encode(payload).decode('ascii')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--count', type=int, default=500)
    args = parser.parse_args()

    admin = login(os.environ.get('RUTA_ADMIN', 'admin'), os.environ.get('RUTA_PASSWORD', 'admin_local_demo_only'))

    conductor_username = 'loadtest_conductor'
    status, _ = request('/api/v1/admin/users', {
        'username': conductor_username, 'password': 'loadtest123',
        'fullName': 'Conductor Carga', 'role': 'CONDUCTOR',
    }, admin)
    if status not in (200, 201, 409):
        raise RuntimeError(('no se pudo crear el conductor de carga', status))
    driver = login(conductor_username, 'loadtest123')

    invoices = fetch_all_invoices(admin)
    pending = [i for i in invoices if i['state'] == 'posted' and not i['confirmed'] and i['requiresPin']]
    random.shuffle(pending)
    targets = pending[:args.count]
    print('Facturas publicadas y pendientes disponibles: %d. Confirmando %d.' % (len(pending), len(targets)))

    confirmed = 0
    for invoice in targets:
        photo = fake_photo_base64()
        status, result = request('/api/v1/driver/deliveries/confirm', {
            'invoiceId': invoice['id'],
            'invoiceNumber': invoice['number'],
            'partnerName': invoice['partnerName'],
            'deliveryAddress': invoice['deliveryAddress'],
            'pin': invoice['pin'],
            'latitude': invoice['expectedLatitude'] or -2.171,
            'longitude': invoice['expectedLongitude'] or -79.922,
            'photoBase64': photo,
            'photoContentType': 'image/png',
            'photoFilename': 'evidencia.png',
        }, driver)
        if status == 200:
            confirmed += 1
        else:
            print('  aviso: factura %s no confirmada (HTTP %s): %s' % (invoice['number'], status, result))

    print('Entregas confirmadas con foto real (~200 KB): %d de %d' % (confirmed, len(targets)))
