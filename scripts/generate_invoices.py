"""Genera facturas de supermercado usando la API propia de Ruta."""
import argparse
import csv
import json
import os
import random
from pathlib import Path
from verify_delivery import request, login

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--count', type=int, default=50)
    args = parser.parse_args()
    if not 1 <= args.count <= 1000:
        parser.error('--count debe estar entre 1 y 1000')
    token = login(os.environ.get('RUTA_ADMIN', 'admin'), os.environ.get('RUTA_PASSWORD', 'admin_local_demo_only'))
    folder = Path(__file__).resolve().parents[1] / 'demo'
    catalog = sorted({line['description'] for invoice in json.loads((folder / 'invoices.json').read_text(encoding='utf-8')) for line in invoice['lines']})
    rng = random.SystemRandom()
    rows = []
    # Flush each successful invoice so a later network failure retains its PIN.
    with (folder / 'nuevo_lote_facturas_y_pines.csv').open('w', encoding='utf-8-sig', newline='') as output:
        writer = csv.DictWriter(output, fieldnames=['factura', 'pin'], delimiter=';')
        writer.writeheader()
        for index in range(args.count):
            products = [{'description': p, 'quantity': rng.choice([0.5, 0.75, 1.25, 2.5]) if 'venta por kg' in p else rng.randint(1, 6)} for p in rng.sample(catalog, rng.randint(3, 8))]
            while True:
                number = '001-104-%010d' % rng.randint(1, 9999999999)
                status, invoice = request('/api/v1/admin/invoices', {'number': number, 'partnerName': 'Cliente Supermercado %02d (MOCK)' % (index % 10 + 1),
                    'deliveryAddress': 'Calle de prueba, Guayaquil', 'latitude': -2.171, 'longitude': -79.922, 'requiresPin': True, 'products': products}, token)
                if status != 409:
                    break
            if status != 201:
                raise RuntimeError((status, invoice))
            status, invoice = request('/api/v1/admin/invoices/%s/publish' % invoice['id'], {}, token)
            if status != 200:
                raise RuntimeError((status, invoice))
            writer.writerow({'factura': invoice['number'], 'pin': invoice['pin']})
            output.flush()
    print('Generadas %s facturas. CSV: demo/nuevo_lote_facturas_y_pines.csv' % args.count)
