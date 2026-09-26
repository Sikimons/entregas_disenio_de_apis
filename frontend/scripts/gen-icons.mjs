import sharp from 'sharp';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const publicDir = path.join(__dirname, '..', 'public');

const targets = [
  { src: 'favicon.svg', out: 'pwa-192x192.png', size: 192 },
  { src: 'favicon.svg', out: 'pwa-512x512.png', size: 512 },
  { src: 'maskable-icon.svg', out: 'maskable-icon-512x512.png', size: 512 },
  { src: 'favicon.svg', out: 'apple-touch-icon.png', size: 180 },
];

for (const { src, out, size } of targets) {
  await sharp(path.join(publicDir, src), { density: 384 })
    .resize(size, size)
    .png()
    .toFile(path.join(publicDir, out));
  console.log(`Generado ${out} (${size}x${size}) a partir de ${src}`);
}
