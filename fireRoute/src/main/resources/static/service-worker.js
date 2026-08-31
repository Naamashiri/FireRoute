const STATIC_CACHE = 'fireroute-static-v10';
const DATA_CACHE = 'fireroute-emergency-data-v1';
const RUNTIME_CACHE = 'fireroute-runtime-v1';

const APP_SHELL = [
  '/',
  '/index.html',
  '/app.css',
  '/route-settings.css',
  '/pwa.css',
  '/app.js',
  '/offline-routing.js',
  '/manifest.webmanifest',
  '/icons/fireroute-app-icon.png',
  '/icons/fireroute-icon.svg',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
  '/icons/icon-maskable-512.png',
  '/icons/apple-touch-icon.png'
];

const EMERGENCY_DATA = [
  '/data/map.json',
  '/data/shelters.json'
];

const OPTIONAL_EXTERNAL_ASSETS = [
  'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css',
  'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'
];

self.addEventListener('install', event => {
  event.waitUntil((async () => {
    const staticCache = await caches.open(STATIC_CACHE);
    const dataCache = await caches.open(DATA_CACHE);

    await Promise.all([
      staticCache.addAll(APP_SHELL),
      dataCache.addAll(EMERGENCY_DATA)
    ]);

    // The app remains installable if the CDN is temporarily unavailable.
    await Promise.allSettled(
      OPTIONAL_EXTERNAL_ASSETS.map(asset => staticCache.add(asset))
    );

    await self.skipWaiting();
  })());
});

self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const allowedCaches = new Set([STATIC_CACHE, DATA_CACHE, RUNTIME_CACHE]);
    const existingCaches = await caches.keys();
    await Promise.all(
      existingCaches
        .filter(cacheName => !allowedCaches.has(cacheName))
        .map(cacheName => caches.delete(cacheName))
    );
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', event => {
  const request = event.request;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);

  if (EMERGENCY_DATA.includes(url.pathname)) {
    event.respondWith(cacheFirst(request, DATA_CACHE));
    return;
  }

  if (url.origin === self.location.origin &&
      (APP_SHELL.includes(url.pathname) || request.mode === 'navigate')) {
    event.respondWith(cacheFirst(request, STATIC_CACHE, '/index.html'));
    return;
  }

  // Cache Leaflet and map tiles as they are encountered. Previously viewed
  // tiles remain visible offline; routing itself does not depend on tiles.
  if (url.hostname === 'unpkg.com' || url.hostname.endsWith('tile.openstreetmap.org')) {
    event.respondWith(cacheFirst(request, RUNTIME_CACHE));
  }
});

async function cacheFirst(request, cacheName, fallbackPath) {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(request);
  if (cached) return cached;

  try {
    const response = await fetch(request);
    if (response && (response.ok || response.type === 'opaque')) {
      await cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    if (fallbackPath) {
      const fallback = await cache.match(fallbackPath);
      if (fallback) return fallback;
    }
    throw error;
  }
}
