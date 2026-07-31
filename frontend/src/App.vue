<template>
  <AnimatedBackground />
  <div class="app-content">
    <RouterView />
  </div>
  <AppFooter />
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AnimatedBackground from '@/components/AnimatedBackground.vue'
import AppFooter from '@/components/AppFooter.vue'

const { locale } = useI18n()

watch(
  locale,
  (newLocale) => {
    document.documentElement.lang = newLocale
  },
  { immediate: true },
)
</script>

<style scoped>
.app-content {
  flex: 1;
}
</style>
