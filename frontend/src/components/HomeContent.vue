<template>
  <div class="grid grid-cols-1 gap-6 md:grid-cols-3 md:items-start">
    <CartoonCard :accent="THEME_COLORS.accent500">
      <StreakFlame
        v-if="authStore.isAuthenticated"
        class="streak-flame--corner"
        :days="authStore.user?.currentStreak ?? 0"
      />
      <template #title>
        <span class="flex items-center gap-2">
          {{ t('home.daily.heading') }}
          <DailyCountdown :color="THEME_COLORS.accent500" />
        </span>
      </template>
      <p class="mb-3 text-xs text-gray-500">{{ t('home.daily.hint') }}</p>
      <CartoonButton
        block
        :color="THEME_COLORS.accent500"
        :disabled="loading"
        :loading="loading"
        @click="onPlayDaily"
      >
        {{ loading ? t('home.daily.loading') : t('home.daily.submit') }}
      </CartoonButton>
    </CartoonCard>

    <CartoonCard :accent="THEME_COLORS.secondary500">
      <template #title>{{ t('home.createRoom.heading') }}</template>
      <p class="mb-3 text-xs text-gray-500">{{ t('home.createRoom.hint') }}</p>
      <CartoonButton
        block
        :color="THEME_COLORS.secondary500"
        class="mt-2"
        :disabled="creatingRoom"
        :loading="creatingRoom"
        @click="onGoToCreateRoom"
      >
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
        <CartoonButton type="submit" block :color="THEME_COLORS.success500" class="mt-2" :disabled="joiningRoom" :loading="joiningRoom">
          {{ t('home.joinRoom.submit') }}
        </CartoonButton>
      </form>
    </CartoonCard>
  </div>

  <p v-if="error" class="mt-4 rounded bg-error-100 p-2 text-sm text-error-700">{{ error }}</p>

  <HowToPlayCard />

  <AccountCta />

  <div class="mt-6 grid grid-cols-1 gap-6 md:grid-cols-2 md:items-start">
    <ShareCard />
    <FeedbackCard />
  </div>
</template>

<script setup lang="ts">
import { THEME_COLORS } from '@/assets/theme'
import AccountCta from '@/components/AccountCta.vue'
import CartoonButton from '@/components/CartoonButton.vue'
import CartoonCard from '@/components/CartoonCard.vue'
import DailyCountdown from '@/components/DailyCountdown.vue'
import FeedbackCard from '@/components/FeedbackCard.vue'
import HowToPlayCard from '@/components/HowToPlayCard.vue'
import ShareCard from '@/components/ShareCard.vue'
import StreakFlame from '@/components/StreakFlame.vue'
import { translateError } from '@/i18n'
import { gameLanguageForLocale } from '@/i18n/languages'
import { useAuthStore } from '@/stores/auth'
import { useGameStore } from '@/stores/game'
import { useLocaleStore } from '@/stores/locale'
import type { ApiError } from '@/types/game'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

const { t } = useI18n()
const router = useRouter()
const gameStore = useGameStore()
const localeStore = useLocaleStore()
const authStore = useAuthStore()

const joinCode = ref('')

const loading = ref(false)
const creatingRoom = ref(false)
const joiningRoom = ref(false)
const error = ref<string | null>(null)

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

// Signed-in users already have a name/avatar on their account, so room
// creation/join happens immediately instead of routing through the
// PlayerSetupView name/avatar form.
async function onGoToCreateRoom() {
  if (!authStore.isAuthenticated) {
    router.push({ path: '/play', query: { mode: 'create' } })
    return
  }
  creatingRoom.value = true
  error.value = null
  try {
    await gameStore.createRoom(
      authStore.user!.username,
      authStore.user!.avatarSeed,
      gameLanguageForLocale(localeStore.preferredLocale),
    )
    router.push(`/room/${gameStore.roomCode}`)
  } catch (err) {
    error.value = translateError(err as ApiError)
  } finally {
    creatingRoom.value = false
  }
}

async function onGoToJoinRoom() {
  const code = joinCode.value.trim().toUpperCase()
  if (!code) return

  if (!authStore.isAuthenticated) {
    router.push({ path: '/play', query: { mode: 'join', code } })
    return
  }
  joiningRoom.value = true
  error.value = null
  try {
    await gameStore.joinRoom(code, authStore.user!.username, authStore.user!.avatarSeed)
    router.push(`/room/${gameStore.roomCode}`)
  } catch (err) {
    error.value = translateError(err as ApiError)
  } finally {
    joiningRoom.value = false
  }
}
</script>

<style scoped>
.cartoon-card :deep(.streak-flame--corner) {
  position: absolute;
  top: 0;
  right: 1.25rem;
  transform: translateY(-50%);
  z-index: 2;
}
</style>
