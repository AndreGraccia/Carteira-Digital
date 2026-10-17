import HttpService from '../services/HttpService';

export const pushSupported = () =>
  typeof window !== 'undefined' && 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;

/** The VAPID key arrives as base64url; PushManager wants the raw bytes. */
export function keyBytes(base64url) {
  const base64 = (base64url + '='.repeat((4 - (base64url.length % 4)) % 4)).replace(/-/g, '+').replace(/_/g, '/');
  return Uint8Array.from(window.atob(base64), (char) => char.charCodeAt(0));
}

export async function currentSubscription() {
  if (!pushSupported()) return null;
  const registration = await navigator.serviceWorker.getRegistration('/');
  return registration ? registration.pushManager.getSubscription() : null;
}

/** True when the browser subscription was made with a different server key (it can never receive again). */
export function staleKey(subscription, publicKey) {
  const key = subscription?.options?.applicationServerKey;
  if (!key) return false;
  const current = keyBytes(publicKey);
  const used = new Uint8Array(key);
  return used.length !== current.length || used.some((byte, index) => byte !== current[index]);
}

/**
 * On logout: stop this browser from showing the next person the alerts of the one who left.
 * Best effort; logging out must never wait on it or fail because of it.
 */
export async function forgetThisBrowser() {
  try {
    const subscription = await currentSubscription();
    if (!subscription) return;
    await HttpService.postWithAuth('/future/push/unsubscribe', { endpoint: subscription.endpoint }).catch(() => {});
    await subscription.unsubscribe();
  } catch {
    // Push is optional; the logout goes on.
  }
}
