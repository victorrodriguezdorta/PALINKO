<template>
  <div class="mx-auto max-w-5xl p-6">
    <AppHeader
      :title="t('common.appTitle')"
      show-back
      center-logo
      show-language-selector
      :back-label="t('calendar.backToHome')"
      :model-value="localeStore.preferredLocale"
      :options="SUPPORTED_LANGUAGES"
      :language-aria-label="t('home.languageLabel')"
      :user="authStore.user"
      :login-label="t('auth.header.login')"
      :profile-label="t('auth.header.profile')"
      :logout-label="t('auth.header.logout')"
      @update:model-value="onLanguageChange"
      @back="router.push('/')"
      @login-click="router.push('/login')"
      @profile-click="router.push('/profile')"
      @logout-click="authStore.logout()"
    />

    <main class="calendar-view">
      <div class="calendar-view__heading">
        <h2 class="calendar-view__title">{{ t('calendar.heading') }}</h2>
      </div>

      <div class="calendar-view__months">
        <CartoonCard v-for="month in months" :key="month.key" :accent="month.accent">
          <template #title>{{ formatMonth(month.date) }}</template>

          <div class="calendar" :aria-label="formatMonth(month.date)">
            <div v-for="weekday in weekdays" :key="weekday" class="calendar__weekday">
              {{ weekday }}
            </div>

            <template v-for="(day, index) in month.days" :key="`${month.key}-${index}`">
              <div v-if="!day" class="calendar__empty" aria-hidden="true" />
              <button
                v-else
                type="button"
                class="calendar__day"
                :class="{
                  'calendar__day--today': day.key === todayKey,
                  'calendar__day--completed': isCompletedDay(day),
                  'calendar__day--selected': day.key === loadingDate,
                }"
                :aria-label="
                  t(isCompletedDay(day) ? 'calendar.completedDay' : 'calendar.playDay', {
                    date: formatFullDate(day.date),
                  })
                "
                :aria-busy="day.key === loadingDate"
                :disabled="isFutureDate(day.date) || Boolean(loadingDate)"
                @click="playDay(day)"
              >
                {{ day.number }}
              </button>
            </template>
          </div>
        </CartoonCard>
      </div>
      <section
        v-if="authReady && !authStore.isAuthenticated"
        class="calendar-view__guest-cta"
        aria-labelledby="calendar-guest-cta-title"
      >
        <h3 id="calendar-guest-cta-title" class="calendar-view__guest-cta-title">
          {{ t('calendar.guestCta.heading') }}
        </h3>
        <p class="calendar-view__guest-cta-text">{{ t('calendar.guestCta.text') }}</p>
        <router-link class="calendar-view__guest-cta-link" to="/login?mode=signup">
          {{ t('calendar.guestCta.action') }}
        </router-link>
      </section>
      <p v-if="historyErrorMessage" class="calendar-view__error" role="alert">
        {{ historyErrorMessage }}
      </p>
      <p v-if="errorMessage" class="calendar-view__error" role="alert">{{ errorMessage }}</p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { THEME_COLORS } from '@/assets/theme'
import AppHeader from '@/components/AppHeader.vue'
import CartoonCard from '@/components/CartoonCard.vue'
import { translateError } from '@/i18n'
import { gameLanguageForLocale, SUPPORTED_LANGUAGES } from '@/i18n/languages'
import { getCompletedDailyChallengeDates } from '@/services/dailyHistoryService'
import { useAuthStore } from '@/stores/auth'
import { useGameStore } from '@/stores/game'
import { useLocaleStore } from '@/stores/locale'
import type { ApiError } from '@/types/game'

type CalendarDay = {
  date: Date
  key: string
  number: number
}

type CalendarMonth = {
  accent: string
  date: Date
  days: Array<CalendarDay | null>
  key: string
}

const { t, locale } = useI18n()
const router = useRouter()
const localeStore = useLocaleStore()
const authStore = useAuthStore()
const gameStore = useGameStore()

const today = new Date()
const todayKey = dateKey(today)
const authReady = ref(false)
const loadingDate = ref<string | null>(null)
const errorMessage = ref<string | null>(null)
const historyErrorMessage = ref<string | null>(null)
const completedDates = ref<Set<string>>(new Set())
let historyRequestId = 0

watch(
  () => authStore.isAuthenticated,
  async (isAuthenticated) => {
    const requestId = ++historyRequestId
    completedDates.value = new Set()
    historyErrorMessage.value = null
    if (!isAuthenticated) return

    try {
      const dates = await getCompletedDailyChallengeDates()
      if (requestId === historyRequestId && authStore.isAuthenticated) {
        completedDates.value = new Set(dates)
      }
    } catch (error) {
      if (requestId === historyRequestId && authStore.isAuthenticated) {
        historyErrorMessage.value = translateError(error as ApiError)
      }
    }
  },
  { immediate: true },
)

onMounted(async () => {
  await authStore.initialize()
  authReady.value = true
})

const weekdays = computed(() => [
  t('calendar.weekdays.monday'),
  t('calendar.weekdays.tuesday'),
  t('calendar.weekdays.wednesday'),
  t('calendar.weekdays.thursday'),
  t('calendar.weekdays.friday'),
  t('calendar.weekdays.saturday'),
  t('calendar.weekdays.sunday'),
])

const months = computed<CalendarMonth[]>(() => {
  const currentMonth = new Date(today.getFullYear(), today.getMonth(), 1)
  const previousMonth = new Date(today.getFullYear(), today.getMonth() - 1, 1)

  return [
    createMonth(previousMonth, THEME_COLORS.secondary500),
    createMonth(currentMonth, THEME_COLORS.accent500),
  ]
})

function createMonth(date: Date, accent: string): CalendarMonth {
  const year = date.getFullYear()
  const month = date.getMonth()
  const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const days: Array<CalendarDay | null> = Array.from({ length: firstWeekday }, () => null)

  for (let number = 1; number <= daysInMonth; number += 1) {
    const day = new Date(year, month, number)
    days.push({ date: day, key: dateKey(day), number })
  }

  while (days.length % 7 !== 0) days.push(null)

  return { accent, date, days, key: `${year}-${month}` }
}

function dateKey(date: Date) {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function formatMonth(date: Date) {
  return new Intl.DateTimeFormat(locale.value, { month: 'long', year: 'numeric' }).format(date)
}

function formatFullDate(date: Date) {
  return new Intl.DateTimeFormat(locale.value, { dateStyle: 'full' }).format(date)
}

function isFutureDate(date: Date) {
  return dateKey(date) > todayKey
}

function isCompletedDay(day: CalendarDay) {
  return authStore.isAuthenticated && completedDates.value.has(day.key)
}

async function playDay(day: CalendarDay) {
  if (isFutureDate(day.date) || loadingDate.value) return

  loadingDate.value = day.key
  errorMessage.value = null
  try {
    await gameStore.createDailyRoom(gameLanguageForLocale(localeStore.preferredLocale), day.key)
    await router.push(`/room/${gameStore.roomCode}`)
  } catch (error) {
    errorMessage.value = translateError(error as ApiError)
  } finally {
    loadingDate.value = null
  }
}

function onLanguageChange(nextLocale: string) {
  localeStore.choose(nextLocale)
}
</script>

<style scoped>
:deep(.app-header) {
  margin-bottom: 2rem;
}

.calendar-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.calendar-view__heading {
  text-align: center;
}

.calendar-view__error {
  margin: 0;
  border-radius: 0.5rem;
  background: var(--color-error-100);
  padding: 0.75rem;
  color: var(--color-error-700);
  text-align: center;
}

.calendar-view__title {
  display: inline-block;
  margin: 0;
  border: 3px solid var(--color-white);
  border-radius: 1rem;
  background: var(--color-accent-500);
  box-shadow: 0 4px 0 var(--color-accent-700);
  color: var(--color-white);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: clamp(1.05rem, 3vw, 1.35rem);
  font-weight: 700;
  line-height: 1.1;
  padding: 0.6rem 1.15rem;
}

.calendar-view__months {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.5rem;
}

.calendar-view__guest-cta {
  border: 1px solid var(--color-gray-200);
  border-radius: 1rem;
  background: var(--color-white);
  box-shadow: 0 4px 12px -4px color-mix(in srgb, var(--color-gray-900) 20%, transparent);
  padding: 1.5rem;
  text-align: center;
}

.calendar-view__guest-cta-title {
  margin: 0;
  color: var(--color-gray-900);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.35rem;
  font-weight: 700;
}

.calendar-view__guest-cta-text {
  max-width: 34rem;
  margin: 0.5rem auto 1rem;
  color: var(--color-gray-700);
  line-height: 1.5;
}

.calendar-view__guest-cta-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.75rem;
  border: 2px solid var(--color-success-700);
  border-radius: 0.75rem;
  background: var(--color-success-500);
  box-shadow: 0 4px 0 var(--color-success-700);
  color: var(--color-white);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 700;
  padding: 0.55rem 1.1rem;
  text-decoration: none;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.calendar-view__guest-cta-link:hover,
.calendar-view__guest-cta-link:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 6px 0 var(--color-success-700);
  outline: none;
}

.calendar {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.35rem;
}

.calendar__weekday {
  padding-bottom: 0.2rem;
  color: var(--color-gray-500);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 0.72rem;
  font-weight: 700;
  text-align: center;
  text-transform: uppercase;
}

.calendar__empty,
.calendar__day {
  aspect-ratio: 1;
}

.calendar__day {
  border: 2px solid var(--color-brand-200);
  border-radius: 0.75rem;
  background: var(--color-white);
  box-shadow: 0 3px 0 var(--color-brand-200);
  color: var(--color-gray-800);
  cursor: pointer;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: clamp(0.8rem, 2.2vw, 1rem);
  font-weight: 600;
  transition: transform 0.15s ease, background 0.15s ease, box-shadow 0.15s ease;
}

.calendar__day:hover:not(:disabled),
.calendar__day:focus-visible {
  transform: translateY(-2px) rotate(-2deg);
  background: var(--color-secondary-50);
  box-shadow: 0 5px 0 var(--color-brand-200);
  outline: none;
}

.calendar__day:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.calendar__day--today {
  border-color: var(--color-accent-500);
  color: var(--color-accent-700);
}

.calendar__day--completed {
  border-color: var(--color-success-500);
  background: var(--color-success-50);
  box-shadow: 0 3px 0 var(--color-success-500);
  color: var(--color-success-700);
}

.calendar__day--completed:hover:not(:disabled),
.calendar__day--completed:focus-visible {
  background: var(--color-success-100);
  box-shadow: 0 5px 0 var(--color-success-500);
}

.calendar__day--selected {
  border-color: var(--color-accent-700);
  background: var(--color-accent-500);
  box-shadow: 0 3px 0 var(--color-accent-700);
  color: var(--color-white);
}

.calendar__day--selected:hover:not(:disabled),
.calendar__day--selected:focus-visible {
  background: var(--color-accent-500);
  box-shadow: 0 5px 0 var(--color-accent-700);
}

@media (max-width: 639px) {
  .calendar-view__months {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .calendar__day,
  .calendar-view__guest-cta-link {
    transition: none;
  }
}
</style>
