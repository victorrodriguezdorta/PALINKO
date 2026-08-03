<template>
  <div class="mx-auto max-w-5xl p-6">
    <AppHeader
      :title="t('common.appTitle')"
      show-language-selector
      :model-value="localeStore.preferredLocale"
      :options="SUPPORTED_LANGUAGES"
      :language-aria-label="t('home.languageLabel')"
      @update:model-value="onLanguageChange"
    />

    <div class="grid grid-cols-1 gap-6 md:grid-cols-3 md:items-start">
    <CartoonCard :accent="THEME_COLORS.accent500">
      <template #title>
        <span class="flex items-center gap-2">
          {{ t('home.daily.heading') }}
          <DailyCountdown :color="THEME_COLORS.accent500" />
        </span>
      </template>
      <p class="mb-3 text-xs text-gray-500">{{ t('home.daily.hint') }}</p>
      <CartoonButton block :color="THEME_COLORS.accent500" :disabled="loading" @click="onPlayDaily">
        {{ t('home.daily.submit') }}
      </CartoonButton>
    </CartoonCard>

    <CartoonCard :accent="THEME_COLORS.secondary500">
      <template #title>{{ t('home.createRoom.heading') }}</template>
      <p class="mb-3 text-xs text-gray-500">{{ t('home.createRoom.hint') }}</p>
      <CartoonButton block :color="THEME_COLORS.secondary500" class="mt-2" @click="onGoToCreateRoom">
        {{ t('home.createRoom.submit') }}
      </CartoonButton>
    </CartoonCard>

    <CartoonCard :accent="THEME_COLORS.success500">
      <template #title>{{ t('home.joinRoom.heading') }}</template>
      <form class="flex flex-col gap-2" @submit.prevent="onGoToJoinRoom">
        <label class="text-sm">
          {{ t('home.joinRoom.codeLabel') }}
          <input
            v-model="joinCode"
            class="w-full rounded border border-gray-300 p-2 uppercase"
            required
            maxlength="6"
          />
        </label>
        <CartoonButton type="submit" block :color="THEME_COLORS.success500" class="mt-2">
          {{ t('home.joinRoom.submit') }}
        </CartoonButton>
      </form>
    </CartoonCard>
    </div>

    <p v-if="error" class="mt-4 rounded bg-error-100 p-2 text-sm text-error-700">{{ error }}</p>

    <HowToPlayCard />

    <div class="mt-6 grid grid-cols-1 gap-6 md:grid-cols-2 md:items-start">
      <ShareCard />
      <FeedbackCard />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useGameStore } from '@/stores/game'
import { useLocaleStore } from '@/stores/locale'
import { SUPPORTED_LANGUAGES, gameLanguageForLocale } from '@/i18n/languages'
import { translateError } from '@/i18n'
import DailyCountdown from '@/components/DailyCountdown.vue'
import CartoonButton from '@/components/CartoonButton.vue'
import CartoonCard from '@/components/CartoonCard.vue'
import AppHeader from '@/components/AppHeader.vue'
import HowToPlayCard from '@/components/HowToPlayCard.vue'
import ShareCard from '@/components/ShareCard.vue'
import FeedbackCard from '@/components/FeedbackCard.vue'
import { THEME_COLORS } from '@/assets/theme'
import type { ApiError } from '@/types/game'

const { t } = useI18n()
const router = useRouter()
const gameStore = useGameStore()
const localeStore = useLocaleStore()

const joinCode = ref('')

const loading = ref(false)
const error = ref<string | null>(null)

function onLanguageChange(locale: string) {
  localeStore.choose(locale)
}

async function onPlayDaily() {
  loading.value = true
  error.value = null
  try {
    await gameStore.createDailyRoom(gameLanguageForLocale(localeStore.preferredLocale))
    router.push(`/room/${gameStore.roomCode}`)
  } catch (err) {
    error.value = translateError(err as ApiError)
  } finally {
    loading.value = false
  }
}

function onGoToCreateRoom() {
  router.push({ path: '/play', query: { mode: 'create' } })
}

function onGoToJoinRoom() {
  const code = joinCode.value.trim().toUpperCase()
  if (!code) return
  router.push({ path: '/play', query: { mode: 'join', code } })
}
</script>
