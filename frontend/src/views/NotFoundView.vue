<template>
  <div class="not-found-view mx-auto max-w-md p-6">
    <PlainCard>
      <div class="not-found-view__content">
        <p class="not-found-view__code">404</p>
        <h1 class="not-found-view__title">{{ t('common.notFound.title') }}</h1>
        <p class="not-found-view__message">{{ t('common.notFound.message') }}</p>
        <CartoonButton :color="THEME_COLORS.secondary500" @click="router.push('/')">
          {{ t('common.notFound.backHome') }}
        </CartoonButton>
      </div>
    </PlainCard>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import CartoonButton from '@/components/CartoonButton.vue'
import PlainCard from '@/components/PlainCard.vue'
import { THEME_COLORS } from '@/assets/theme'

const { t } = useI18n()
const router = useRouter()
</script>

<style scoped>
.not-found-view {
  min-height: 60vh;
  display: flex;
  align-items: center;
}

.not-found-view :deep(.plain-card) {
  width: 100%;
}

.not-found-view__content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  text-align: center;
}

.not-found-view__code {
  margin: 0;
  color: var(--color-secondary-500);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 4rem;
  font-weight: 700;
  line-height: 1;
}

.not-found-view__title,
.not-found-view__message {
  margin: 0;
}

.not-found-view__title {
  color: var(--color-gray-800);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.5rem;
}

.not-found-view__message {
  margin-bottom: 0.5rem;
  color: var(--color-gray-600);
}
</style>