<template>
  <CartoonCard :accent="THEME_COLORS.secondary500">
    <template #title>{{ t('home.share.heading') }}</template>
    <p class="mb-3 text-xs text-gray-500">{{ t('home.share.hint') }}</p>
    <CartoonButton block :color="THEME_COLORS.secondary500" @click="onCopy">
      {{ copied ? t('home.share.copied') : t('home.share.copyButton') }}
    </CartoonButton>
  </CartoonCard>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import CartoonButton from '@/components/CartoonButton.vue'
import CartoonCard from '@/components/CartoonCard.vue'
import { THEME_COLORS } from '@/assets/theme'
import { copyToClipboard } from '@/utils/clipboard'

const { t } = useI18n()

const copied = ref(false)

async function onCopy() {
  const text = t('home.share.text', { url: window.location.origin })
  const ok = await copyToClipboard(text)
  if (!ok) return
  copied.value = true
  setTimeout(() => {
    copied.value = false
  }, 2000)
}
</script>
