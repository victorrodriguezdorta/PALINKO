<template>
  <CartoonCard :accent="THEME_COLORS.success500">
    <template #title>{{ t('home.feedback.heading') }}</template>
    <p class="mb-3 text-xs text-gray-500">{{ t('home.feedback.hint') }}</p>
    <div class="flex flex-col gap-2">
      <a href="https://www.linkedin.com/in/victorrodriguezdorta/" target="_blank" rel="noopener noreferrer">
        <CartoonButton block :color="THEME_COLORS.success500">
          {{ t('home.feedback.linkedin') }}
        </CartoonButton>
      </a>
      <a href="https://github.com/victorrodriguezdorta/PALINKO" target="_blank" rel="noopener noreferrer">
        <CartoonButton block :color="THEME_COLORS.accent500">
          {{ t('home.feedback.github') }}
        </CartoonButton>
      </a>
    </div>
  </CartoonCard>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import CartoonButton from '@/components/CartoonButton.vue'
import CartoonCard from '@/components/CartoonCard.vue'
import { THEME_COLORS } from '@/assets/theme'

const { t } = useI18n()
</script>
