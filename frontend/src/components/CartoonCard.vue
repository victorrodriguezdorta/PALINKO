<template>
  <section class="cartoon-card" :style="cssVars">
    <div class="cartoon-card__blob" aria-hidden="true" />
    <div class="cartoon-card__body">
      <h2 v-if="$slots.title" class="cartoon-card__title">
        <slot name="title" />
      </h2>
      <div class="cartoon-card__content">
        <slot />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { THEME_COLORS } from '@/assets/theme'

const props = withDefaults(
  defineProps<{
    accent?: string
  }>(),
  {
    accent: THEME_COLORS.accent500,
  },
)

const cssVars = computed(() => ({
  '--cartoon-card-accent': props.accent,
}))
</script>

<style scoped>
.cartoon-card {
  --cartoon-card-accent: var(--color-accent-500);

  position: relative;
  isolation: isolate;
  height: 100%;
  padding: 0.35rem;
}

/* Wavy, "blobby" edge: a static oversized asymmetric border-radius shape
   sitting behind the content. On hover it morphs slightly toward a second
   blob shape and the card lifts a touch, giving a light playful reaction. */
.cartoon-card__blob {
  position: absolute;
  inset: 0;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  background: var(--color-white);
  border: 3px solid var(--cartoon-card-accent);
  box-shadow:
    0 6px 0 color-mix(in srgb, var(--cartoon-card-accent) 55%, var(--color-black) 20%),
    0 10px 24px -8px color-mix(in srgb, var(--color-gray-900) 35%, transparent);
  transition:
    border-radius 0.4s ease,
    box-shadow 0.25s ease;
}

.cartoon-card {
  transition: transform 0.25s ease;
}

.cartoon-card:hover {
  transform: translateY(-2px);
}

.cartoon-card:hover .cartoon-card__blob {
  border-radius: 225px 25px 205px 25px / 25px 205px 25px 225px;
  box-shadow:
    0 8px 0 color-mix(in srgb, var(--cartoon-card-accent) 55%, var(--color-black) 20%),
    0 12px 24px -8px color-mix(in srgb, var(--color-gray-900) 35%, transparent);
}

.cartoon-card__body {
  position: relative;
  padding: 1.35rem 1.5rem;
}

.cartoon-card__title {
  margin-bottom: 0.6rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: var(--color-gray-800);
  text-shadow: 2px 2px 0 color-mix(in srgb, var(--cartoon-card-accent) 35%, transparent);
}

.cartoon-card__content {
  font-family: 'Outfit', sans-serif;
}

@media (prefers-reduced-motion: reduce) {
  .cartoon-card,
  .cartoon-card__blob {
    transition: none;
  }
}
</style>
