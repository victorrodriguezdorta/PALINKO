<template>
  <footer class="app-footer">
    <img :src="logoUrl" alt="PALINKO" class="app-footer__logo" />
    <p class="app-footer__text">{{ t('common.footer.text', { year }) }}</p>
  </footer>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import logoUrl from '@/assets/images/logosimple.svg'

const { t } = useI18n()
const year = new Date().getFullYear()
</script>

<style scoped>
.app-footer {
  position: relative;
  left: 50%;
  width: 100vw;
  margin-left: -50vw;
  box-sizing: border-box;
  margin-top: 3rem;
  padding: 1.25rem;
  background: var(--color-secondary-700);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.app-footer__logo {
  height: 2rem;
  width: auto;
  object-fit: contain;
}

.app-footer__text {
  margin: 0;
  font-size: 0.75rem;
  color: color-mix(in srgb, var(--color-white) 80%, transparent);
}
</style>
