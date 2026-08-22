<template>
  <div class="mx-auto max-w-2xl p-6">
    <AppHeader
      :title="t('common.appTitle')"
      show-back
      :back-label="t('profile.backToHome')"
      :user="authStore.user"
      :login-label="t('auth.header.login')"
      :profile-label="t('auth.header.profile')"
      :logout-label="t('auth.header.logout')"
      @back="router.push('/')"
      @logout-click="onLogout"
    />

    <div class="flex flex-col gap-6">
      <PlainCard>
        <template #title>{{ t('profile.account.heading') }}</template>

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

        <form class="flex flex-col gap-2" @submit.prevent="onSaveProfile">
          <label class="text-sm">
            {{ t('auth.fields.username') }}
            <input
              v-model="username"
              class="w-full rounded border border-gray-300 p-2"
              required
              minlength="5"
              maxlength="15"
            />
          </label>
          <CartoonButton
            type="submit"
            block
            :color="THEME_COLORS.secondary500"
            class="mt-2"
            :disabled="profileLoading || !isProfileDirty"
            :loading="profileLoading"
          >
            {{ profileLoading ? t('profile.account.savingLoading') : t('profile.account.save') }}
          </CartoonButton>
          <p v-if="profileSuccess" class="mt-1 rounded bg-success-100 p-2 text-sm text-success-700">
            {{ t('profile.account.saved') }}
          </p>
          <p v-else-if="profileError" class="mt-1 rounded bg-error-100 p-2 text-sm text-error-700">
            {{ profileError }}
          </p>
        </form>
      </PlainCard>

      <PlainCard>
        <template #title>{{ t('profile.password.heading') }}</template>

        <form class="flex flex-col gap-2" @submit.prevent="onChangePassword">
          <label class="text-sm">
            {{ t('profile.password.current') }}
            <input
              v-model="passwordForm.currentPassword"
              type="password"
              class="w-full rounded border border-gray-300 p-2"
              required
            />
          </label>
          <label class="text-sm">
            {{ t('profile.password.new') }}
            <input
              v-model="passwordForm.newPassword"
              type="password"
              class="w-full rounded border border-gray-300 p-2"
              required
            />
          </label>
          <label class="text-sm">
            {{ t('profile.password.confirm') }}
            <input
              v-model="passwordForm.confirmNewPassword"
              type="password"
              class="w-full rounded border border-gray-300 p-2"
              required
            />
          </label>
          <p class="text-xs text-gray-500">{{ t('auth.signup.passwordHint') }}</p>
          <CartoonButton
            type="submit"
            block
            :color="THEME_COLORS.accent500"
            class="mt-2"
            :disabled="passwordLoading"
            :loading="passwordLoading"
          >
            {{ passwordLoading ? t('profile.password.savingLoading') : t('profile.password.save') }}
          </CartoonButton>

          <p v-if="passwordSuccess" class="mt-1 rounded bg-success-100 p-2 text-sm text-success-700">
            {{ t('profile.password.saved') }}
          </p>
          <template v-else-if="passwordRules.length">
            <p class="mt-1 text-sm text-error-700">{{ t('auth.signup.passwordErrorsHeading') }}</p>
            <ul class="ml-4 list-disc text-sm text-error-700">
              <li v-for="rule in passwordRules" :key="rule">{{ t(`errors.passwordRules.${rule}`) }}</li>
            </ul>
          </template>
          <p v-else-if="passwordError" class="mt-1 rounded bg-error-100 p-2 text-sm text-error-700">
            {{ passwordError }}
          </p>
        </form>
      </PlainCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { randomAvatarSeed } from '@/utils/avatar'
import { translateError } from '@/i18n'
import AppHeader from '@/components/AppHeader.vue'
import CartoonButton from '@/components/CartoonButton.vue'
import PlainCard from '@/components/PlainCard.vue'
import PlayerAvatar from '@/components/PlayerAvatar.vue'
import { THEME_COLORS } from '@/assets/theme'
import type { ApiError } from '@/types/game'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const username = ref(authStore.user?.username ?? '')
const avatarSeed = ref(authStore.user?.avatarSeed ?? '')

// Keeps the form in sync if the store's user changes underneath it (e.g.
// after a successful save re-fetches the profile).
watch(
  () => authStore.user,
  (user) => {
    if (!user) return
    username.value = user.username
    avatarSeed.value = user.avatarSeed
  },
)

const isProfileDirty = computed(
  () => username.value !== authStore.user?.username || avatarSeed.value !== authStore.user?.avatarSeed,
)

const profileLoading = ref(false)
const profileError = ref('')
const profileSuccess = ref(false)

function onShuffleAvatar() {
  avatarSeed.value = randomAvatarSeed()
  profileSuccess.value = false
}

async function onSaveProfile() {
  profileLoading.value = true
  profileError.value = ''
  profileSuccess.value = false
  try {
    const payload: { username?: string; avatarSeed?: string } = {}
    if (username.value !== authStore.user?.username) payload.username = username.value
    if (avatarSeed.value !== authStore.user?.avatarSeed) payload.avatarSeed = avatarSeed.value
    await authStore.updateProfile(payload)
    profileSuccess.value = true
  } catch (err) {
    profileError.value = translateError(err as ApiError)
  } finally {
    profileLoading.value = false
  }
}

const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmNewPassword: '' })
const passwordLoading = ref(false)
const passwordError = ref('')
const passwordRules = ref<string[]>([])
const passwordSuccess = ref(false)

async function onChangePassword() {
  passwordLoading.value = true
  passwordError.value = ''
  passwordRules.value = []
  passwordSuccess.value = false
  try {
    await authStore.changePassword({ ...passwordForm })
    passwordSuccess.value = true
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmNewPassword = ''
  } catch (err) {
    const apiError = err as ApiError
    if (apiError?.code === 'WEAK_PASSWORD') {
      passwordRules.value = (apiError.args?.rules ?? '').split(',').filter(Boolean)
    } else {
      passwordError.value = translateError(apiError)
    }
  } finally {
    passwordLoading.value = false
  }
}

function onLogout() {
  authStore.logout()
  router.push('/')
}
</script>
