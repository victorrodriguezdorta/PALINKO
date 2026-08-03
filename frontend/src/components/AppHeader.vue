<template>
  <header class="app-header" :style="headerWaveStyle">
    <CartoonButton
      v-if="showBack"
      size="sm"
      :color="THEME_COLORS.secondary500"
      class="app-header__back"
      :aria-label="backLabel"
      @click="$emit('back')"
    >
      <ArrowLeft :size="16" aria-hidden="true" />
      <span class="app-header__back-label">{{ backLabel }}</span>
    </CartoonButton>

    <h1 class="app-header__title" :class="{ 'app-header__title--centered': centerLogo }">
      <img :src="logoUrl" :alt="title" class="app-header__logo" />
    </h1>

    <div class="app-header__right">
      <button
        type="button"
        class="app-header__mute"
        :aria-label="isMuted ? 'Unmute sound' : 'Mute sound'"
        @click="toggleMuted"
      >
        <VolumeX v-if="isMuted" :size="18" aria-hidden="true" />
        <Volume2 v-else :size="18" aria-hidden="true" />
      </button>

      <LanguageSelector
        v-if="showLanguageSelector"
        class="app-header__lang"
        :model-value="modelValue"
        :options="options"
        :ariaLabel="languageAriaLabel"
        @update:model-value="$emit('update:modelValue', $event)"
      />
      <span v-else class="app-header__spacer" aria-hidden="true" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { ArrowLeft, Volume2, VolumeX } from 'lucide-vue-next'
import CartoonButton from '@/components/CartoonButton.vue'
import LanguageSelector from '@/components/LanguageSelector.vue'
import type { LanguageOption } from '@/i18n/languages'
import logoUrl from '@/assets/images/logo.svg'
import { THEME_COLORS } from '@/assets/theme'
import { useSound } from '@/composables/useSound'

const { isMuted, toggleMuted } = useSound()

withDefaults(
  defineProps<{
    title: string
    showLanguageSelector?: boolean
    showBack?: boolean
    backLabel?: string
    modelValue?: string
    options?: LanguageOption[]
    languageAriaLabel?: string
    centerLogo?: boolean
  }>(),
  {
    showLanguageSelector: false,
    showBack: false,
    backLabel: '',
    modelValue: '',
    options: () => [],
    languageAriaLabel: '',
    centerLogo: false,
  },
)

defineEmits<{
  'update:modelValue': [value: string]
  back: []
}>()

const headerWaveStyle = {
  '--app-header-wave': `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 20' preserveAspectRatio='none'%3E%3Cpath d='M0 10 C 25 20, 75 0, 100 10 C 125 20, 175 0, 200 10 L200 0 L0 0 Z' fill='%23${THEME_COLORS.secondary700.slice(1)}'/%3E%3C/svg%3E")`,
}
</script>

<style scoped>
.app-header {
  position: relative;
  left: 50%;
  width: 100vw;
  margin-left: -50vw;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  height: 3.5rem;
  padding: 0 1.25rem;
  margin-top: -1.5rem;
  margin-bottom: 4.5rem;
  background: var(--color-secondary-700);
  overflow: visible;
  z-index: 1;
}

.app-header::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1.1rem;
  height: 1.2rem;
  background-image: var(--app-header-wave);
  background-repeat: repeat-x;
  background-size: 200px 100%;
  animation: header-wave 8s linear infinite;
  pointer-events: none;
}

@keyframes header-wave {
  from {
    background-position-x: 0;
  }
  to {
    background-position-x: -200px;
  }
}

@media (min-width: 640px) {
  .app-header {
    padding-left: max(1.25rem, calc((100vw - 1024px) / 2 + 1.25rem));
    padding-right: max(1.25rem, calc((100vw - 1024px) / 2 + 1.25rem));
  }
}

.app-header__title {
  flex: 1;
  min-width: 0;
  align-self: stretch;
  position: relative;
}

.app-header__logo {
  height: 7.5rem;
  max-width: 100%;
  width: auto;
  object-fit: contain;
  position: absolute;
  left: 0;
  top: 0;
  z-index: 2;
  filter: drop-shadow(2px 2px 0 color-mix(in srgb, var(--color-gray-900) 35%, transparent));
}

.app-header__title--centered {
  position: static;
}

.app-header__title--centered .app-header__logo {
  left: 50%;
  right: auto;
  transform: translateX(-50%);
}

.app-header__back {
  flex-shrink: 0;
}

@media (max-width: 639px) {
  .app-header__back-label {
    display: none;
  }
}

.app-header__right {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-shrink: 0;
}

.app-header__mute {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  flex-shrink: 0;
  border-radius: 9999px;
  border: 2px solid color-mix(in srgb, var(--color-white) 40%, transparent);
  background: transparent;
  color: var(--color-white);
  cursor: pointer;
  transition:
    transform 0.15s ease,
    background 0.15s ease;
}

.app-header__mute:hover {
  background: color-mix(in srgb, var(--color-white) 15%, transparent);
  transform: scale(1.08);
}

.app-header__lang,
.app-header__spacer {
  flex-shrink: 0;
}
</style>
