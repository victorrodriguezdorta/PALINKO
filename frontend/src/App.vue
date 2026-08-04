<template>
  <AnimatedBackground />
  <div class="app-content">
    <RouterView />
  </div>
  <AppFooter />
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue'
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

// Static loader lives outside the Vue app (see index.html) so it's visible
// before Vue even finishes parsing/mounting. Remove it once the app has
// actually painted its first frame.
onMounted(() => {
  requestAnimationFrame(() => {
    document.getElementById('app-loader')?.remove()
  })
})
</script>

<style scoped>
.app-content {
  flex: 1;
}
</style>
