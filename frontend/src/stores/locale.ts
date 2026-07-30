import { defineStore } from 'pinia'
import { ref } from 'vue'
import { i18n, getPreferredLocale, setPreferredLocale } from '@/i18n'

/**
 * The visitor's own persisted UI language choice — used for the Home
 * screen's own chrome and as the language a room is created with. Distinct
 * from the room's language (see stores/game.ts), which takes over the
 * active locale for everyone inside a room regardless of their own
 * preference here.
 */
export const useLocaleStore = defineStore('locale', () => {
  const preferredLocale = ref(getPreferredLocale())

  function choose(locale: string) {
    preferredLocale.value = locale
    setPreferredLocale(locale)
  }

  function applyPreferred() {
    i18n.global.locale.value = preferredLocale.value
  }

  return { preferredLocale, choose, applyPreferred }
})
