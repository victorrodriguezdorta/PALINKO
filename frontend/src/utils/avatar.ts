const DICEBEAR_STYLE_URL = 'https://api.dicebear.com/10.x/micah/svg'
const STORAGE_KEY = 'human-or-ai:avatar-seed'

export function avatarUrlForSeed(seed: string): string {
  return `${DICEBEAR_STYLE_URL}?seed=${encodeURIComponent(seed)}`
}

export function randomAvatarSeed(): string {
  return Math.random().toString(36).slice(2, 10)
}

// localStorage, not sessionStorage: unlike the room identity (see
// stores/game.ts), a player's chosen avatar is a cross-session profile
// preference that should stay the same the next time they open the app,
// including in a brand new tab.
export function loadOrCreateAvatarSeed(): string {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored) return stored
  const seed = randomAvatarSeed()
  localStorage.setItem(STORAGE_KEY, seed)
  return seed
}

export function persistAvatarSeed(seed: string): void {
  localStorage.setItem(STORAGE_KEY, seed)
}
