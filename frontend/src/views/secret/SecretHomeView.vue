<template>
  <div class="mx-auto max-w-3xl p-6">
    <AppHeader
      :title="t('secret.home.pageTitle')"
      show-back
      center-logo
      :back-label="t('room.backToHome')"
      @back="onBack"
    />

    <div class="secret-home-badge-wrap">
      <span class="secret-home-badge">
        <Sparkles :size="14" aria-hidden="true" />
        {{ t('home.hiddenMode.badge') }}
      </span>
    </div>

    <h1 class="secret-home-title">{{ t('secret.home.heading') }}</h1>
    <p class="secret-home-subtitle">{{ t('secret.home.subtitle') }}</p>

    <CartoonCard :accent="THEME_COLORS.warning500" class="mt-6">
      <template #title>
        <span class="flex items-center gap-2">
          {{ t('home.daily.heading') }}
          <DailyCountdown :color="THEME_COLORS.warning500" />
        </span>
      </template>
      <p class="mb-3 text-xs text-gray-500">{{ t('secret.home.daily.hint') }}</p>
      <CartoonButton
        block
        :color="THEME_COLORS.warning500"
        :disabled="loading"
        :loading="loading"
        @click="onPlayDaily"
      >
        {{ loading ? t('home.daily.loading') : t('home.daily.submit') }}
      </CartoonButton>
      <p v-if="error" class="mt-3 rounded bg-error-100 p-2 text-sm text-error-700">{{ error }}</p>
    </CartoonCard>

    <section class="secret-home-rules">
      <h2 class="secret-home-rules__title">{{ t('secret.home.rules.heading') }}</h2>
      <ul class="secret-home-rules__list">
        <li>
          <span class="secret-home-rules__icon secret-home-rules__icon--word" aria-hidden="true">
            <PenLine :size="18" />
          </span>
          <span>{{ t('secret.home.rules.step1') }}</span>
        </li>
        <li>
          <span class="secret-home-rules__icon secret-home-rules__icon--check" aria-hidden="true">
            <CheckCircle2 :size="18" />
          </span>
          <span>{{ t('secret.home.rules.step2') }}</span>
        </li>
        <li>
          <span class="secret-home-rules__icon secret-home-rules__icon--hint" aria-hidden="true">
            <Lightbulb :size="18" />
          </span>
          <span>{{ t('secret.home.rules.step3') }}</span>
        </li>
        <li>
          <span class="secret-home-rules__icon secret-home-rules__icon--solve" aria-hidden="true">
            <Trophy :size="18" />
          </span>
          <span>{{ t('secret.home.rules.step4') }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { CheckCircle2, Lightbulb, PenLine, Sparkles, Trophy } from 'lucide-vue-next'
import { useSecretGameStore } from '@/stores/secretGame'
import { useLocaleStore } from '@/stores/locale'
import { gameLanguageForLocale } from '@/i18n/languages'
import { translateError } from '@/i18n'
import DailyCountdown from '@/components/DailyCountdown.vue'
import CartoonButton from '@/components/CartoonButton.vue'
import CartoonCard from '@/components/CartoonCard.vue'
import AppHeader from '@/components/AppHeader.vue'
import { THEME_COLORS } from '@/assets/theme'
import type { SecretApiError } from '@/types/secretGame'

const { t } = useI18n()
const router = useRouter()
const store = useSecretGameStore()
const localeStore = useLocaleStore()

const loading = ref(false)
const error = ref<string | null>(null)

function onBack() {
  router.push('/')
}

async function onPlayDaily() {
  loading.value = true
  error.value = null
  try {
    await store.createDailyRoom(gameLanguageForLocale(localeStore.preferredLocale))
    router.push(`/secret/room/${store.roomCode}`)
  } catch (err) {
    error.value = translateError(err as SecretApiError)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.secret-home-badge-wrap {
  display: flex;
  justify-content: center;
  margin-top: 0.5rem;
}

.secret-home-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.85rem;
  border-radius: 9999px;
  background: var(--color-warning-100, #fef0c7);
  border: 2px solid var(--color-warning-500);
  color: var(--color-warning-700, #b54708);
  font-family: 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 0.7rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.secret-home-title {
  margin-top: 0.75rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: var(--color-white);
  text-shadow: 3px 3px 0 color-mix(in srgb, var(--color-secondary-900, #0b1f4d) 55%, transparent);
  text-align: center;
}

.secret-home-subtitle {
  margin-top: 0.4rem;
  font-family: 'Outfit', sans-serif;
  font-size: 0.95rem;
  color: color-mix(in srgb, var(--color-white) 80%, transparent);
  text-align: center;
  max-width: 34rem;
  margin-left: auto;
  margin-right: auto;
}

.secret-home-rules {
  margin-top: 2rem;
  padding: 1.5rem;
  border-radius: 1.25rem;
  background: var(--color-white);
  border: 2px dashed var(--color-gray-300);
}

.secret-home-rules__title {
  margin-bottom: 1rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-gray-800);
  text-align: center;
}

.secret-home-rules__list {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.secret-home-rules__list li {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-family: 'Outfit', sans-serif;
  font-size: 0.9rem;
  color: var(--color-gray-700);
}

.secret-home-rules__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  flex-shrink: 0;
  border-radius: 9999px;
  color: var(--color-white);
  box-shadow: 0 3px 0 color-mix(in srgb, var(--color-black) 20%, transparent);
}

.secret-home-rules__icon--word {
  background: var(--color-secondary-500);
}

.secret-home-rules__icon--check {
  background: var(--color-success-500);
}

.secret-home-rules__icon--hint {
  background: var(--color-warning-500);
}

.secret-home-rules__icon--solve {
  background: var(--color-accent-500);
}
</style>
