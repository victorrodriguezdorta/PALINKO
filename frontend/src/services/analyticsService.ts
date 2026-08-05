declare global {
  interface Window {
    dataLayer?: unknown[]
    gtag?: (...args: unknown[]) => void
  }
}

function trackEvent(eventName: string, params?: Record<string, unknown>) {
  if (typeof window === 'undefined' || typeof window.gtag !== 'function') return
  window.gtag('event', eventName, params)
}

/** A visitor lands on the app (fired once per app mount, from the router's first navigation). */
export function trackVisit() {
  trackEvent('app_visit')
}

/** The daily-challenge room was created (solo, anonymous room). */
export function trackDailyRoomCreated() {
  trackEvent('daily_room_created')
}

/** A group (party) room was created by a host. */
export function trackGroupRoomCreated() {
  trackEvent('group_room_created')
}

/** A player joined a group room; playerCount is the total players now in the room. */
export function trackGroupRoomPlayerJoined(playerCount: number) {
  trackEvent('group_room_player_joined', { player_count: playerCount })
}
