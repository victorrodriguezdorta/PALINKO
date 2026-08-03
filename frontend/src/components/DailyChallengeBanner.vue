<template>
  <div class="daily-banner" :style="cssVars" role="status">
    <span class="daily-banner__text">{{ t('room.dailyBanner') }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
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
  '--daily-banner-color': props.color,
}))
</script>

<style scoped>
.daily-banner {
  --daily-banner-color: var(--color-accent-500);

  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.45rem 1.5rem;
  border: 3px solid var(--color-gray-800);
  border-radius: 0.6rem;
  background: var(--daily-banner-color);
  box-shadow:
    0 5px 0 color-mix(in srgb, var(--daily-banner-color) 55%, var(--color-black) 25%),
    0 8px 18px -6px color-mix(in srgb, var(--color-gray-900) 40%, transparent);
  width: fit-content;
}

.daily-banner__text {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.1rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: var(--color-white);
  text-shadow: 2px 2px 0 color-mix(in srgb, var(--color-black) 25%, transparent);
  white-space: nowrap;
}

@media (max-width: 480px) {
  .daily-banner__text {
    font-size: 0.9rem;
  }
}
</style>
