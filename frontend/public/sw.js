// Service worker for push alerts only. On purpose it caches nothing and intercepts no request:
// the app always comes from the server, so an old version never gets stuck on the device.

self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()));

self.addEventListener('push', (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch {
    // An unreadable body still becomes a generic alert, never silence.
  }
  event.waitUntil(
    self.registration.showNotification(typeof data.title === 'string' ? data.title : 'Wall.ia', {
      body: typeof data.body === 'string' ? data.body : '',
      icon: '/logo.png',
      badge: '/favicon.svg',
      tag: typeof data.tag === 'string' ? data.tag : 'wallia',
      renotify: true,
      data: { url: typeof data.url === 'string' ? data.url : '/future' },
    })
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  // Only paths of this site, whatever the message says.
  const target = new URL(event.notification.data?.url || '/future', self.location.origin);
  if (target.origin !== self.location.origin) return;
  event.waitUntil(
    (async () => {
      const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
      const open = windows.find((client) => new URL(client.url).origin === target.origin);
      if (open) {
        await open.focus();
        return open.navigate(target.href).catch(() => {});
      }
      return self.clients.openWindow(target.href);
    })()
  );
});
