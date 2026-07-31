<template>
  <div class="cartoon-timer" :style="cssVars" role="timer" :aria-label="t('home.daily.countdown', { value: formatted })">
    <span class="cartoon-timer__segment">{{ parts.hours }}</span>
    <span class="cartoon-timer__colon">:</span>
    <span class="cartoon-timer__segment">{{ parts.minutes }}</span>
    <span class="cartoon-timer__colon">:</span>
    <span class="cartoon-timer__segment">{{ parts.seconds }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { THEME_COLORS } from '@/assets/theme'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    color?: string
  }>(),
  {
    color: THEME_COLORS.accent500,
  },
)

const cssVars = computed(() => ({
  '--cartoon-timer-color': props.color,
}))

// The "server day" is the UTC calendar day (see backend DailySeed), so the
// countdown always targets the next UTC midnight regardless of the
// visitor's own timezone.
function nextUtcMidnight(): number {
  const now = new Date()
  return Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() + 1, 0, 0, 0)
}

const now = ref(Date.now())
let tickInterval: ReturnType<typeof setInterval> | undefined

const parts = computed(() => {
  const remainingMs = Math.max(0, nextUtcMidnight() - now.value)
  const totalSeconds = Math.floor(remainingMs / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const pad = (value: number) => value.toString().padStart(2, '0')
  return { hours: pad(hours), minutes: pad(minutes), seconds: pad(seconds) }
})

const formatted = computed(() => `${parts.value.hours}:${parts.value.minutes}:${parts.value.seconds}`)

onMounted(() => {
  tickInterval = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (tickInterval) clearInterval(tickInterval)
})
</script>

<style scoped>
.cartoon-timer {
  --cartoon-timer-color: var(--color-accent-500);

  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
}

.cartoon-timer__segment {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.5rem;
  padding: 0.15rem 0.25rem;
  border: 2px solid var(--color-gray-800);
  border-radius: 0.4rem;
  background: var(--cartoon-timer-color);
  color: var(--color-white);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 0.75rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  box-shadow: 0 2px 0 color-mix(in srgb, var(--cartoon-timer-color) 55%, var(--color-black) 25%);
}

.cartoon-timer__colon {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-gray-800);
}

@media (prefers-reduced-motion: reduce) {
  .cartoon-timer__segment {
    transition: none;
  }
}
</style>
