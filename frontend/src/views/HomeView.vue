<template>
  <div class="mx-auto max-w-md p-6">
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-2xl font-bold">{{ t('common.appTitle') }}</h1>
      <select
        :value="localeStore.preferredLocale"
        class="rounded border border-gray-300 p-1 text-sm"
        :aria-label="t('home.languageLabel')"
        @change="onLanguageChange"
      >
        <option v-for="option in SUPPORTED_LANGUAGES" :key="option.locale" :value="option.locale">
          {{ option.label }}
        </option>
      </select>
    </div>

    <section class="mb-8 rounded-lg border border-gray-300 p-4">
      <h2 class="mb-3 font-semibold">{{ t('home.daily.heading') }}</h2>
      <p class="mb-3 text-xs text-gray-500">{{ t('home.daily.hint') }}</p>
      <DailyCountdown class="mb-3" />
      <button
        type="button"
        class="w-full rounded bg-purple-600 p-2 font-semibold text-white"
        :disabled="loading"
        @click="onPlayDaily"
      >
        {{ t('home.daily.submit') }}
      </button>
    </section>

    <section class="mb-8 rounded-lg border border-gray-300 p-4">
      <h2 class="mb-3 font-semibold">{{ t('home.createRoom.heading') }}</h2>
      <form class="flex flex-col gap-2" @submit.prevent="onCreateRoom">
        <label class="text-sm">
          {{ t('common.yourName') }}
          <input v-model="hostName" class="w-full rounded border border-gray-300 p-2" required maxlength="24" />
        </label>
        <p class="text-xs text-gray-500">{{ t('home.createRoom.hint') }}</p>
        <button type="submit" class="mt-2 rounded bg-blue-600 p-2 font-semibold text-white" :disabled="loading">
          {{ t('home.createRoom.submit') }}
        </button>
      </form>
    </section>

    <section class="rounded-lg border border-gray-300 p-4">
      <h2 class="mb-3 font-semibold">{{ t('home.joinRoom.heading') }}</h2>
      <form class="flex flex-col gap-2" @submit.prevent="onJoinRoom">
        <label class="text-sm">
          {{ t('home.joinRoom.codeLabel') }}
          <input
            v-model="joinCode"
            class="w-full rounded border border-gray-300 p-2 uppercase"
            required
            maxlength="6"
          />
        </label>
        <label class="text-sm">
          {{ t('common.yourName') }}
          <input v-model="joinName" class="w-full rounded border border-gray-300 p-2" required maxlength="24" />
        </label>
        <button type="submit" class="mt-2 rounded bg-green-600 p-2 font-semibold text-white" :disabled="loading">
          {{ t('home.joinRoom.submit') }}
        </button>
      </form>
    </section>

    <p v-if="error" class="mt-4 rounded bg-red-100 p-2 text-sm text-red-700">{{ error }}</p>

    <RouterLink to="/debug/word-relation" class="mt-6 block text-center text-xs text-gray-400 underline">
      {{ t('debug.wordRelation.heading') }}
    </RouterLink>
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
import type { ApiError } from '@/types/game'

const { t } = useI18n()
const router = useRouter()
const gameStore = useGameStore()
const localeStore = useLocaleStore()

const hostName = ref('')

const joinCode = ref('')
const joinName = ref('')

const loading = ref(false)
const error = ref<string | null>(null)

function onLanguageChange(event: Event) {
  localeStore.choose((event.target as HTMLSelectElement).value)
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

async function onCreateRoom() {
  loading.value = true
  error.value = null
  try {
    await gameStore.createRoom(hostName.value, gameLanguageForLocale(localeStore.preferredLocale))
    router.push(`/room/${gameStore.roomCode}`)
  } catch (err) {
    error.value = translateError(err as ApiError)
  } finally {
    loading.value = false
  }
}

async function onJoinRoom() {
  loading.value = true
  error.value = null
  try {
    await gameStore.joinRoom(joinCode.value.trim().toUpperCase(), joinName.value)
    router.push(`/room/${gameStore.roomCode}`)
  } catch (err) {
    error.value = translateError(err as ApiError)
  } finally {
    loading.value = false
  }
}
</script>
