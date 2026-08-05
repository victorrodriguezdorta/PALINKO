<template>
  <div class="mx-auto max-w-md p-6">
    <AppHeader :title="t('common.appTitle')" show-back center-logo :back-label="t('room.backToHome')" @back="onBack" />

    <CartoonCard :accent="mode === 'join' ? THEME_COLORS.success500 : THEME_COLORS.secondary500">
      <template #title>
        {{ mode === 'join' ? t('playerSetup.joinHeading', { code: roomCode }) : t('playerSetup.createHeading') }}
      </template>

      <div class="mb-4 flex flex-col items-center gap-2">
        <PlayerAvatar
          :seed="avatarSeed"
          size="lg"
          editable
          :shuffle-label="t('home.avatar.shuffle')"
          @shuffle="onShuffleAvatar"
        />
        <p class="text-xs text-gray-500">{{ t('home.avatar.hint') }}</p>
      </div>

      <form class="flex flex-col gap-2" @submit.prevent="onSubmit">
        <label class="text-sm">
          {{ t('common.yourName') }}
          <input
            v-model="name"
            class="w-full rounded border border-gray-300 p-2"
            required
            maxlength="24"
            autofocus
          />
        </label>
        <CartoonButton
          type="submit"
          block
          :color="mode === 'join' ? THEME_COLORS.success500 : THEME_COLORS.secondary500"
          class="mt-2"
          :disabled="loading"
          :loading="loading"
        >
          {{
            loading
              ? mode === 'join'
                ? t('playerSetup.joinSubmitLoading')
                : t('playerSetup.createSubmitLoading')
              : mode === 'join'
                ? t('playerSetup.joinSubmit')
                : t('playerSetup.createSubmit')
          }}
        </CartoonButton>
      </form>

      <p v-if="error" class="mt-3 rounded bg-error-100 p-2 text-sm text-error-700">{{ error }}</p>
    </CartoonCard>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useGameStore } from '@/stores/game'
import { useLocaleStore } from '@/stores/locale'
import { gameLanguageForLocale } from '@/i18n/languages'
import { translateError } from '@/i18n'
import CartoonButton from '@/components/CartoonButton.vue'
import CartoonCard from '@/components/CartoonCard.vue'
import AppHeader from '@/components/AppHeader.vue'
import PlayerAvatar from '@/components/PlayerAvatar.vue'
import { loadOrCreateAvatarSeed, persistAvatarSeed, randomAvatarSeed } from '@/utils/avatar'
import { THEME_COLORS } from '@/assets/theme'
import { useSound } from '@/composables/useSound'
import type { ApiError } from '@/types/game'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const gameStore = useGameStore()
const localeStore = useLocaleStore()
const { play } = useSound()

// 'join' when arriving from the Home "join room" form with a code already
// picked; anything else (including missing/invalid) falls back to create,
// same as the room-link join prompt does for a missing identity.
const mode = route.query.mode === 'join' ? 'join' : 'create'
const roomCode = (route.query.code as string | undefined)?.trim().toUpperCase() ?? ''

const name = ref('')
const avatarSeed = ref(loadOrCreateAvatarSeed())
const loading = ref(false)
const error = ref<string | null>(null)

function onShuffleAvatar() {
  avatarSeed.value = randomAvatarSeed()
  persistAvatarSeed(avatarSeed.value)
}

function onBack() {
  router.push('/')
}

async function onSubmit() {
  loading.value = true
  error.value = null
  try {
    if (mode === 'join') {
      await gameStore.joinRoom(roomCode, name.value, avatarSeed.value)
      play('roomEnter')
    } else {
      await gameStore.createRoom(name.value, avatarSeed.value, gameLanguageForLocale(localeStore.preferredLocale))
    }
    router.push(`/room/${gameStore.roomCode}`)
  } catch (err) {
    error.value = translateError(err as ApiError)
  } finally {
    loading.value = false
  }
}
</script>
