<template>
  <button
    type="button"
    class="cartoon-btn"
    :class="[`cartoon-btn--${size}`, { 'cartoon-btn--block': block }]"
    :style="cssVars"
    :disabled="disabled || loading"
    @click="onClick"
  >
    <span class="cartoon-btn__label">
      <slot />
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { THEME_COLORS } from '@/assets/theme'
import { useSound } from '@/composables/useSound'

const props = withDefaults(
  defineProps<{
    color?: string
    shadowColor?: string
    textColor?: string
    size?: 'sm' | 'md' | 'lg'
    block?: boolean
    disabled?: boolean
    loading?: boolean
  }>(),
  {
    color: THEME_COLORS.accent500,
    shadowColor: '',
    textColor: THEME_COLORS.white,
    size: 'md',
    block: false,
    disabled: false,
    loading: false,
  },
)

// Auto-darkens the base color for the shadow/pressed layer when no
// explicit shadowColor is given, so callers usually only pass one color.
function darken(hex: string, amount: number): string {
  const match = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  if (!match) return hex
  const [, r, g, b] = match
  const clamp = (channel: number) => Math.max(0, Math.min(255, Math.round(channel)))
  const shade = (value: string) => clamp(parseInt(value, 16) * (1 - amount))
  return `rgb(${shade(r)}, ${shade(g)}, ${shade(b)})`
}

const cssVars = computed(() => ({
  '--cartoon-color': props.color,
  '--cartoon-shadow': props.shadowColor || darken(props.color, 0.35),
  '--cartoon-text': props.textColor,
}))

const { play } = useSound()
function onClick() {
  play('buttonClick')
}
</script>

<style scoped>
.cartoon-btn {
  --cartoon-color: var(--color-accent-500);
  --cartoon-shadow: var(--color-accent-700);
  --cartoon-text: var(--color-white);

  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4em;
  border: 3px solid var(--cartoon-text, var(--color-black));
  border-radius: 1rem;
  background: var(--cartoon-color);
  color: var(--cartoon-text);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 600;
  letter-spacing: 0.02em;
  box-shadow: 0 5px 0 var(--cartoon-shadow);
  transform: translateY(0);
  transition:
    transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.15s ease;
  cursor: pointer;
  user-select: none;
}

.cartoon-btn--sm {
  padding: 0.4rem 0.9rem;
  font-size: 0.85rem;
  border-width: 2px;
  box-shadow: 0 3px 0 var(--cartoon-shadow);
}

.cartoon-btn--md {
  padding: 0.6rem 1.4rem;
  font-size: 1rem;
}

.cartoon-btn--lg {
  padding: 0.8rem 1.8rem;
  font-size: 1.15rem;
  box-shadow: 0 6px 0 var(--cartoon-shadow);
}

.cartoon-btn--block {
  display: flex;
  width: 100%;
}

.cartoon-btn:hover:not(:disabled) {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 8px 0 var(--cartoon-shadow);
}

.cartoon-btn:active:not(:disabled) {
  transform: translateY(2px) scale(0.97);
  box-shadow: 0 2px 0 var(--cartoon-shadow);
}

.cartoon-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: 0 3px 0 var(--cartoon-shadow);
  transform: none;
}

.cartoon-btn__label {
  display: inline-flex;
  align-items: center;
  gap: 0.4em;
}
</style>
