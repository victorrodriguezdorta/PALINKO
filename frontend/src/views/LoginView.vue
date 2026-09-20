<template>
  <div class="mx-auto max-w-md p-6">
    <AppHeader
      :title="t('common.appTitle')"
      show-back
      center-logo
      :show-auth="false"
      :back-label="t('auth.backToHome')"
      @back="router.push('/')"
    />

    <PlainCard class="login-view__card">
      <template #title>
        <div class="login-view__tabs">
          <button
            type="button"
            class="login-view__tab"
            :class="{ 'login-view__tab--active': tab === 'login' }"
            @click="switchTab('login')"
          >
            {{ t('auth.login.tab') }}
          </button>
          <button
            type="button"
            class="login-view__tab"
            :class="{ 'login-view__tab--active': tab === 'signup' }"
            @click="switchTab('signup')"
          >
            {{ t('auth.signup.tab') }}
          </button>
        </div>
      </template>

      <form v-if="tab === 'login'" class="flex flex-col gap-2" @submit.prevent="onLogin">
        <label class="text-sm">
          {{ t('auth.fields.username') }}
          <input v-model="loginForm.username" class="w-full rounded border border-gray-300 p-2" required autofocus />
        </label>
        <label class="text-sm">
          {{ t('auth.fields.password') }}
          <input
            v-model="loginForm.password"
            type="password"
            class="w-full rounded border border-gray-300 p-2"
            required
          />
        </label>
        <CartoonButton
          type="submit"
          block
          :color="THEME_COLORS.secondary500"
          class="mt-2"
          :disabled="loading"
          :loading="loading"
        >
          {{ loading ? t('auth.login.submitLoading') : t('auth.login.submit') }}
        </CartoonButton>
        <p v-if="errorMessage" class="mt-1 rounded bg-error-100 p-2 text-sm text-error-700">{{ errorMessage }}</p>
      </form>

      <form v-else class="flex flex-col gap-2" @submit.prevent="onSignup">
        <label class="text-sm">
          {{ t('auth.fields.username') }}
          <input
            v-model="signupForm.username"
            class="w-full rounded border border-gray-300 p-2"
            required
            minlength="5"
            maxlength="15"
            autofocus
          />
        </label>
        <label class="text-sm">
          {{ t('auth.fields.email') }}
          <input v-model="signupForm.email" type="email" class="w-full rounded border border-gray-300 p-2" required />
        </label>
        <label class="text-sm">
          {{ t('auth.fields.password') }}
          <input
            v-model="signupForm.password"
            type="password"
            class="w-full rounded border border-gray-300 p-2"
            required
          />
        </label>
        <p class="text-xs text-gray-500">{{ t('auth.signup.passwordHint') }}</p>
        <div class="login-view__avatar-picker">
          <PlayerAvatar
            :seed="signupForm.avatarSeed"
            size="lg"
            editable
            :shuffle-label="t('home.avatar.shuffle')"
            @shuffle="shuffleSignupAvatar"
          />
          <p class="text-xs text-gray-500">{{ t('home.avatar.hint') }}</p>
        </div>
        <CartoonButton
          type="submit"
          block
          :color="THEME_COLORS.success500"
          class="mt-2"
          :disabled="loading"
          :loading="loading"
        >
          {{ loading ? t('auth.signup.submitLoading') : t('auth.signup.submit') }}
        </CartoonButton>

        <p v-if="signupSuccess" class="mt-1 rounded bg-success-100 p-2 text-sm text-success-700">
          {{ t('auth.signup.success') }}
        </p>
        <template v-else-if="passwordRules.length">
          <p class="mt-1 text-sm text-error-700">{{ t('auth.signup.passwordErrorsHeading') }}</p>
          <ul class="ml-4 list-disc text-sm text-error-700">
            <li v-for="rule in passwordRules" :key="rule">{{ t(`errors.passwordRules.${rule}`) }}</li>
          </ul>
        </template>
        <p v-else-if="errorMessage" class="mt-1 rounded bg-error-100 p-2 text-sm text-error-700">{{ errorMessage }}</p>
      </form>
    </PlainCard>
  </div>
</template>

<script setup lang="ts">
import { THEME_COLORS } from '@/assets/theme'
import AppHeader from '@/components/AppHeader.vue'
import CartoonButton from '@/components/CartoonButton.vue'
import PlainCard from '@/components/PlainCard.vue'
import PlayerAvatar from '@/components/PlayerAvatar.vue'
import { useAuthForm } from '@/composables/useAuthForm'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const {
  tab,
  loading,
  errorMessage,
  passwordRules,
  signupSuccess,
  loginForm,
  signupForm,
  switchTab,
  onLogin,
  onSignup,
  shuffleSignupAvatar,
} = useAuthForm(() => router.push('/'))

if (route.query.mode === 'signup') switchTab('signup')
</script>

<style scoped>
.login-view__card {
  width: 100%;
}

.login-view__tabs {
  display: flex;
  gap: 0.5rem;
}

.login-view__tab {
  flex: 1;
  padding: 0.4rem 0;
  border: none;
  background: transparent;
  font-weight: 700;
  color: var(--color-gray-500);
  cursor: pointer;
  border-bottom: 3px solid transparent;
}

.login-view__tab--active {
  color: var(--color-gray-900);
  border-bottom-color: var(--color-secondary-500);
}

.login-view__avatar-picker {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  margin: 0.75rem 0 0.25rem;
}
</style>
