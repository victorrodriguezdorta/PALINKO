<template>
  <Transition name="streak-hint">
    <div v-if="visible" class="streak-hint" role="status">
      <span class="streak-hint__icon" aria-hidden="true">💡</span>
      <p class="streak-hint__text">{{ message }}</p>
      <button type="button" class="streak-hint__close" :aria-label="t('common.close')" @click="$emit('dismiss')">
        <X :size="16" aria-hidden="true" />
      </button>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { X } from 'lucide-vue-next'

defineProps<{
  visible: boolean
  message: string
}>()

defineEmits<{
  dismiss: []
}>()

const { t } = useI18n()
</script>

<style scoped>
.streak-hint {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  width: 100%;
  padding: 0.7rem 0.9rem;
  border-radius: 1rem;
  border: 3px solid color-mix(in srgb, var(--color-warning-500) 80%, var(--color-black) 15%);
  background: var(--color-warning-500);
  box-shadow: 0 4px 0 color-mix(in srgb, var(--color-warning-500) 70%, var(--color-black) 25%);
}

.streak-hint__icon {
  font-size: 1.3rem;
  flex-shrink: 0;
}

.streak-hint__text {
  flex: 1;
  font-family: 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--color-white);
  text-shadow: 0 1px 1px color-mix(in srgb, var(--color-black) 25%, transparent);
}

.streak-hint__close {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.6rem;
  height: 1.6rem;
  border-radius: 9999px;
  border: none;
  background: transparent;
  color: var(--color-white);
  cursor: pointer;
  opacity: 0.85;
  transition: opacity 0.15s ease;
}

.streak-hint__close:hover {
  opacity: 1;
}

.streak-hint-enter-active {
  transition:
    opacity 0.3s ease,
    transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.streak-hint-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.streak-hint-enter-from {
  opacity: 0;
  transform: translateY(0.5rem) scale(0.92);
}

.streak-hint-leave-to {
  opacity: 0;
  transform: translateY(-0.3rem) scale(0.96);
}

@media (prefers-reduced-motion: reduce) {
  .streak-hint-enter-active,
  .streak-hint-leave-active {
    transition: none;
  }
}
</style>
