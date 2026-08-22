<template>
  <div ref="root" class="lang-select" :class="{ 'lang-select--open': isOpen }">
    <button
      type="button"
      class="lang-select__trigger"
      :aria-label="ariaLabel"
      :title="selectedLabel"
      aria-haspopup="listbox"
      :aria-expanded="isOpen"
      @click="toggle"
      @keydown="onTriggerKeydown"
    >
      <span class="lang-select__flag" aria-hidden="true">
        <img v-if="selectedFlagSrc" :src="selectedFlagSrc" alt="" class="lang-select__flag-img" />
      </span>
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
          <span class="lang-select__flag lang-select__flag--option" aria-hidden="true">
            <img :src="flagSrc(option.locale)" alt="" class="lang-select__flag-img" />
          </span>
          <span>{{ option.label }}</span>
          <Check v-if="option.locale === modelValue" class="lang-select__check" :size="14" aria-hidden="true" />
        </li>
      </ul>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Check } from 'lucide-vue-next'
import type { LanguageOption } from '@/i18n/languages'
import esFlag from '@/assets/images/es.svg'
import gbFlag from '@/assets/images/gb.svg'

const FLAG_SRC_BY_LOCALE: Record<string, string> = {
  es: esFlag,
  en: gbFlag,
}

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

const selectedOption = computed(() => props.options.find((option) => option.locale === props.modelValue))
const selectedLabel = computed(() => selectedOption.value?.label ?? '')
const selectedFlagSrc = computed(() => flagSrc(selectedOption.value?.locale))

function flagSrc(locale: string | undefined): string | undefined {
  return locale ? FLAG_SRC_BY_LOCALE[locale] : undefined
}

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
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  flex-shrink: 0;
  border-radius: 9999px;
  border: 2px solid color-mix(in srgb, var(--color-white) 40%, transparent);
  background: transparent;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    background 0.15s ease;
}

.lang-select__trigger:hover {
  background: color-mix(in srgb, var(--color-white) 15%, transparent);
  transform: scale(1.08);
}

.lang-select__trigger:focus-visible {
  outline: 2px solid var(--color-white);
  outline-offset: 2px;
}

.lang-select--open .lang-select__trigger {
  background: color-mix(in srgb, var(--color-white) 15%, transparent);
}

.lang-select__flag {
  display: inline-flex;
  width: 100%;
  height: 100%;
  border-radius: 9999px;
  overflow: hidden;
  line-height: 1;
}

.lang-select__flag--option {
  width: 1.35rem;
  height: 1.35rem;
  flex-shrink: 0;
}

.lang-select__flag-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.lang-select__panel {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  z-index: 20;
  min-width: max-content;
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
  .lang-panel-enter-active,
  .lang-panel-leave-active {
    transition: none;
  }
}
</style>
