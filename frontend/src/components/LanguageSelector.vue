<template>
  <div ref="root" class="lang-select" :class="{ 'lang-select--open': isOpen }">
    <button
      type="button"
      class="lang-select__trigger"
      :aria-label="ariaLabel"
      aria-haspopup="listbox"
      :aria-expanded="isOpen"
      @click="toggle"
      @keydown="onTriggerKeydown"
    >
      <Globe class="lang-select__icon" :size="16" aria-hidden="true" />
      <span class="lang-select__label">{{ selectedLabel }}</span>
      <ChevronDown class="lang-select__chevron" :size="14" aria-hidden="true" />
    </button>

    <Transition name="lang-panel">
      <ul v-if="isOpen" class="lang-select__panel" role="listbox" :aria-label="ariaLabel">
        <li
          v-for="option in options"
          :key="option.locale"
          class="lang-select__option"
          :class="{ 'lang-select__option--active': option.locale === modelValue }"
          role="option"
          :aria-selected="option.locale === modelValue"
          @click="select(option.locale)"
        >
          <Check v-if="option.locale === modelValue" class="lang-select__check" :size="14" aria-hidden="true" />
          <span>{{ option.label }}</span>
        </li>
      </ul>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Globe, ChevronDown, Check } from 'lucide-vue-next'
import type { LanguageOption } from '@/i18n/languages'

const props = defineProps<{
  modelValue: string
  options: LanguageOption[]
  ariaLabel: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const root = ref<HTMLElement | null>(null)
const isOpen = ref(false)

const selectedLabel = computed(
  () => props.options.find((option) => option.locale === props.modelValue)?.label ?? '',
)

function toggle() {
  isOpen.value = !isOpen.value
}

function close() {
  isOpen.value = false
}

function select(locale: string) {
  emit('update:modelValue', locale)
  close()
}

function onTriggerKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') close()
}

function onClickOutside(event: MouseEvent) {
  if (root.value && !root.value.contains(event.target as Node)) close()
}

onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))
</script>

<style scoped>
.lang-select {
  position: relative;
  display: inline-flex;
}

.lang-select__trigger {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  border: 3px solid var(--color-black);
  border-radius: 1rem;
  background: var(--color-white);
  padding: 0.4rem 0.7rem;
  box-shadow: 0 4px 0 var(--color-brand-200);
  color: var(--color-brand-700);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  transition:
    transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.15s ease;
}

.lang-select__trigger:hover {
  transform: translateY(-2px) scale(1.03);
  box-shadow: 0 6px 0 var(--color-brand-200);
}

.lang-select__trigger:active {
  transform: translateY(1px) scale(0.98);
  box-shadow: 0 2px 0 var(--color-brand-200);
}

.lang-select__trigger:focus-visible {
  outline: 2px solid var(--color-brand-500);
  outline-offset: 2px;
}

.lang-select--open .lang-select__trigger {
  box-shadow: 0 2px 0 var(--color-brand-200);
  transform: translateY(2px);
}

.lang-select__icon {
  color: var(--color-brand-500);
  flex-shrink: 0;
}

.lang-select__chevron {
  color: var(--color-brand-500);
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.lang-select--open .lang-select__chevron {
  transform: rotate(180deg);
}

.lang-select__panel {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  z-index: 20;
  min-width: 100%;
  margin: 0;
  padding: 0.4rem;
  list-style: none;
  border: 3px solid var(--color-black);
  border-radius: 1rem;
  background: var(--color-white);
  box-shadow: 0 6px 0 var(--color-brand-200);
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.lang-select__option {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.6rem;
  border-radius: 0.7rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--color-gray-800);
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.lang-select__option:hover {
  background: var(--color-brand-50);
}

.lang-select__option--active {
  color: var(--color-brand-700);
  background: var(--color-brand-100);
}

.lang-select__check {
  color: var(--color-brand-700);
  flex-shrink: 0;
}

.lang-panel-enter-active,
.lang-panel-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.lang-panel-enter-from,
.lang-panel-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.96);
}

@media (prefers-reduced-motion: reduce) {
  .lang-select__trigger,
  .lang-select__chevron,
  .lang-panel-enter-active,
  .lang-panel-leave-active {
    transition: none;
  }
}
</style>
