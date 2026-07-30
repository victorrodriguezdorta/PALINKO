import type { GameLanguage } from '@/types/game'

export interface LanguageOption {
  /** vue-i18n locale code */
  locale: string
  /** backend GameLanguage enum value */
  gameLanguage: GameLanguage
  /** label shown in the language selector, in that language's own tongue */
  label: string
}

/**
 * Single source of truth for every language the app supports. Adding a
 * language later means: one more entry here, one more locales/<code>.json
 * file, and the matching GameLanguage constant on the backend — nothing
 * else changes shape.
 */
export const SUPPORTED_LANGUAGES: LanguageOption[] = [
  { locale: 'en', gameLanguage: 'ENGLISH', label: 'English' },
  { locale: 'es', gameLanguage: 'SPANISH', label: 'Español' },
]

const LOCALE_BY_GAME_LANGUAGE: Record<GameLanguage, string> = Object.fromEntries(
  SUPPORTED_LANGUAGES.map((option) => [option.gameLanguage, option.locale]),
) as Record<GameLanguage, string>

const GAME_LANGUAGE_BY_LOCALE: Record<string, GameLanguage> = Object.fromEntries(
  SUPPORTED_LANGUAGES.map((option) => [option.locale, option.gameLanguage]),
)

const DEFAULT_LOCALE = 'es'
const DEFAULT_GAME_LANGUAGE: GameLanguage = 'SPANISH'

export function localeForGameLanguage(language: GameLanguage): string {
  return LOCALE_BY_GAME_LANGUAGE[language] ?? DEFAULT_LOCALE
}

export function gameLanguageForLocale(locale: string): GameLanguage {
  return GAME_LANGUAGE_BY_LOCALE[locale] ?? DEFAULT_GAME_LANGUAGE
}

/**
 * Picks a sensible starting locale from the visitor's browser before they've
 * made any choice of their own — this app's own content only ever existed in
 * Spanish before this feature, so anything unrecognized falls back to that.
 */
export function detectDefaultLocale(): string {
  const browserLocale = navigator.language?.slice(0, 2).toLowerCase()
  const match = SUPPORTED_LANGUAGES.find((option) => option.locale === browserLocale)
  return match?.locale ?? DEFAULT_LOCALE
}
