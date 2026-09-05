const CACHE_VERSION = 'dranivo-v13-pro';
const STATIC_ASSETS = [
  './',
  './Damino.html',
  './manifest.json',
  './icon-192.png',
  './icon-512.png'
];

/* INSTALL: skip waiting so new SW activates immediately */
self.addEventListener('install', (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_VERSION).then((cache) => {
      return cache.addAll(STATIC_ASSETS).catch(() => {});
    })
  );
});

/* ACTIVATE: delete ALL old caches (any version that isn't current), take control immediately */
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames
          .map((name) => {
            return caches.delete(name);
          })
      );
    }).then(() => {
      return self.clients.claim();
    })
  );
});

/* SKIP_WAITING message handler */
self.addEventListener('message', (event) => {
  if (event.data === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});

/* FETCH: network-first for HTML/JS/CSS, cache-first for images & media (saves bandwidth) */
self.addEventListener('fetch', (event) => {
  const request = event.request;

  /* Only handle GET requests */
  if (request.method !== 'GET') return;

  const url = new URL(request.url);

  /* Cross-origin media (Firebase Storage / Supabase / image CDNs): cache-first to save data */
  const isMedia = request.destination === 'image' ||
    /\.(?:png|jpg|jpeg|gif|webp|mp4|webm|mov|svg|ico|woff2?|ttf|mp3|m4a|aac|wav)$/i.test(url.pathname);

  if (url.origin !== self.location.origin) {
    /* Firebase SDK scripts are version-pinned; cache them to avoid repeat downloads. */
    const isPinnedScript = request.destination === 'script' && url.hostname === 'www.gstatic.com';
    /* Cache cross-origin media and pinned SDK scripts to save bandwidth. */
    if (isMedia || isPinnedScript) {
      event.respondWith(
        caches.match(request).then((cached) => {
          if (cached) return cached;
          return fetch(request).then((response) => {
            if (response && response.status === 200) {
              const responseClone = response.clone();
              caches.open(CACHE_VERSION).then((cache) => {
                cache.put(request, responseClone).catch(() => {});
              });
            }
            return response;
          }).catch(() => cached || Response.error());
        })
      );
    }
    return;
  }

  /* Instant navigation: serve cached app shell first, refresh it in background. */
  if (request.mode === 'navigate' ||
      request.destination == 'document' ||
      request.destination === 'style' ||
      url.pathname.endsWith('.html')) {
    event.respondWith(
      caches.match(request).then((cached) => {
        const fresh = fetch(request).then((response) => {
          if (response && response.status === 200) {
            const responseClone = response.clone();
            caches.open(CACHE_VERSION).then((cache) => cache.put(request, responseClone).catch(() => {})));
          }
          return response;
        }).catch(() => cached || caches.match('./Damino.html'));
        return cached || fresh;
      })
    );
    return;
  }
  /* App JS files are immutable in this single-file build; cache first. */
  if (request.destination === 'script') {
    event.respondWith(caches.match(request).then((cached) => cached || fetch(request).then((response) => {
      if (response && response.status === 200) caches.open(CACHE_VERSION).then((cache) => cache.put(request, response.clone()).catch(() => {}));
      return response;
    })));
    return;
  }

  /* Cache-first for images, videos, audio and other static assets (saves bandwidth) */
  event.respondWith(
    caches.match(request).then((cached) => {
      return cached || fetch(request).then((response) => {
        if (response && response.status === 200) {
          const responseClone = response.clone();
          caches.open(CACHE_VERSION).then((cache) => {
            cache.put(request, responseClone).catch(() => {});
          });
        }
        return response;
      }).catch(() => {
        return cached;
      });
    })
  );
});
