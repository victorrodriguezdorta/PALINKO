<template>
  <div class="mx-auto max-w-5xl p-6">
    <AppHeader
      :title="t('common.appTitle')"
      show-language-selector
      :model-value="localeStore.preferredLocale"
      :options="SUPPORTED_LANGUAGES"
      :language-aria-label="t('home.languageLabel')"
      :user="authStore.user"
      :login-label="t('auth.header.login')"
      :profile-label="t('auth.header.profile')"
      :logout-label="t('auth.header.logout')"
      @update:model-value="onLanguageChange"
      @login-click="router.push('/login')"
      @profile-click="router.push('/profile')"
      @logout-click="authStore.logout()"
    />

    <HomeContent />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useLocaleStore } from '@/stores/locale'
import { useAuthStore } from '@/stores/auth'
import { SUPPORTED_LANGUAGES } from '@/i18n/languages'
import AppHeader from '@/components/AppHeader.vue'
import HomeContent from '@/components/HomeContent.vue'

const { t } = useI18n()
const router = useRouter()
const localeStore = useLocaleStore()
const authStore = useAuthStore()

function onLanguageChange(locale: string) {
  localeStore.choose(locale)
}
</script>
