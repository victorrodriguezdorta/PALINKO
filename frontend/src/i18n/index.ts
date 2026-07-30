import { createI18n } from 'vue-i18n'
import en from './locales/en.json'
import es from './locales/es.json'
import { detectDefaultLocale } from './languages'
import type { ApiError } from '@/types/game'

const PREFERRED_LOCALE_KEY = 'human-or-ai:preferred-language'

export function getPreferredLocale(): string {
  return localStorage.getItem(PREFERRED_LOCALE_KEY) ?? detectDefaultLocale()
}

type MessageSchema = typeof en

// Typed as Record<string, MessageSchema> rather than the literal `{ en, es }`
// so vue-i18n's Locale type stays a plain `string` instead of a union
// hardcoded to today's two locales — adding a language later is just one
// more entry here, not a type change.
const messages: Record<string, MessageSchema> = { en, es }

export const i18n = createI18n({
  legacy: false,
  locale: getPreferredLocale(),
  fallbackLocale: 'en',
  messages,
})

/**
 * Persists the visitor's own language choice (used for the Home screen and
 * as the language a room is created with) and applies it immediately. Once
 * inside a room, the active locale is instead driven by the room's own
 * language (see stores/game.ts) — this is only restored on leaving.
 */
export function setPreferredLocale(locale: string): void {
  localStorage.setItem(PREFERRED_LOCALE_KEY, locale)
  i18n.global.locale.value = locale
}

/**
 * Renders a backend ApiError (STOMP or REST) in the currently active
 * locale via its stable code, falling back to a generic translated message
 * for codes this build of the frontend doesn't recognize (e.g. an older
 * client talking to a newer backend).
 */
export function translateError(error: ApiError | null | undefined): string {
  if (!error) return ''
  const key = `errors.${error.code}`
  return i18n.global.te(key) ? i18n.global.t(key, error.args) : i18n.global.t('errors.GENERIC')
}
