<template>
  <div
    class="streak-flame"
    :class="{ 'streak-flame--compact': compact }"
    :style="cssVars"
    role="img"
    :aria-label="t('home.daily.streak.label', { count: days })"
  >
    <span class="streak-flame__badge">
      <span v-if="!compact" class="streak-flame__label">{{ t('home.daily.streak.prefix') }}</span>
      <span class="streak-flame__icon">
        <div v-if="days <= 0" class="streak-flame__empty" />
        <svg v-else class="streak-flame__fire" viewBox="0 0 64 80" aria-hidden="true">
          <path
            class="streak-flame__fire-outer"
            d="M32 2
               C 40 14, 52 20, 52 36
               C 52 56, 47 65, 42 71
               C 39 79, 25 79, 22 71
               C 17 65, 12 56, 12 36
               C 12 20, 24 14, 32 2 Z"
          />
          <path
            class="streak-flame__fire-inner"
            d="M32 22
               C 37 30, 43 35, 43 45
               C 43 56, 40 62, 36 67
               C 34 72, 30 72, 28 67
               C 24 62, 21 56, 21 45
               C 21 35, 27 30, 32 22 Z"
          />
        </svg>
        <span class="streak-flame__count">{{ days }}</span>
      </span>
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { THEME_COLORS } from '@/assets/theme'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    days: number
    color?: string
    // Drops the pill background and "Días de Racha:" label, leaving just
    // the flame + count — for inline use next to an avatar (e.g. the daily
    // leaderboard row) where the full badge would be too heavy.
    compact?: boolean
  }>(),
  {
    color: THEME_COLORS.streak500,
    compact: false,
  },
)

const cssVars = computed(() => ({
  '--streak-flame-color': props.color,
}))
</script>

<style scoped>
.streak-flame {
  --streak-flame-color: var(--color-streak-500);

  position: relative;
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
}

.streak-flame__badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.3rem 0.7rem 0.3rem 0.9rem;
  border-radius: 999px;
  background: var(--color-streak-700);
}

.streak-flame--compact .streak-flame__badge {
  padding: 0;
  background: transparent;
}

.streak-flame--compact .streak-flame__icon {
  width: 1.5rem;
  height: 1.5rem;
}

.streak-flame--compact .streak-flame__count {
  margin-top: 0.3rem;
  font-size: 0.7rem;
  color: var(--color-gray-900);
  text-shadow: none;
}

.streak-flame__label {
  font-family: 'Outfit', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-white);
  text-shadow: none;
  white-space: nowrap;
}

.streak-flame__icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  flex-shrink: 0;
  animation: streak-flame-bob 1.6s ease-in-out infinite;
}

.streak-flame__empty {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--color-gray-400);
}

.streak-flame__fire {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  transform-origin: 50% 90%;
  animation: streak-flame-flicker 1.4s ease-in-out infinite;
}

.streak-flame__fire-outer {
  fill: var(--streak-flame-color);
  stroke: var(--color-gray-800);
  stroke-width: 3;
  stroke-linejoin: round;
}

.streak-flame__fire-inner {
  fill: var(--color-streak-300);
}

.streak-flame__count {
  position: relative;
  z-index: 1;
  margin-top: 0.5rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-gray-900);
  text-shadow: none;
}

@keyframes streak-flame-bob {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-2px);
  }
}

@keyframes streak-flame-flicker {
  0%,
  100% {
    transform: scale(1, 1) rotate(0deg);
  }
  25% {
    transform: scale(0.95, 1.05) rotate(-2deg);
  }
  50% {
    transform: scale(1.05, 0.97) rotate(1deg);
  }
  75% {
    transform: scale(0.97, 1.03) rotate(2deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .streak-flame__icon,
  .streak-flame__fire {
    animation: none;
  }
}
</style>
