<template>
  <footer class="app-footer">
    <img :src="logoUrl" alt="PALINKO" class="app-footer__logo" />
    <p class="app-footer__text">{{ t('common.footer.text', { year }) }}</p>

    <div class="app-footer__socials">
      <a
        class="app-footer__social-btn"
        href="https://www.linkedin.com/in/victorrodriguezdorta/"
        target="_blank"
        rel="noopener noreferrer"
        :aria-label="t('common.footer.linkedin')"
        :title="t('common.footer.linkedin')"
      >
        <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path
            d="M20.45 20.45h-3.56v-5.57c0-1.33-.02-3.03-1.85-3.03-1.85 0-2.14 1.45-2.14 2.94v5.66H9.34V9h3.41v1.56h.05c.48-.9 1.64-1.85 3.37-1.85 3.6 0 4.27 2.37 4.27 5.45v6.29ZM5.34 7.43a2.07 2.07 0 1 1 0-4.13 2.07 2.07 0 0 1 0 4.13ZM7.12 20.45H3.56V9h3.56v11.45Z"
          />
        </svg>
      </a>

      <a
        class="app-footer__social-btn"
        href="https://github.com/victorrodriguezdorta"
        target="_blank"
        rel="noopener noreferrer"
        :aria-label="t('common.footer.github')"
        :title="t('common.footer.github')"
      >
        <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path
            d="M12 2.2c-5.52 0-10 4.48-10 10 0 4.42 2.87 8.17 6.84 9.5.5.09.68-.22.68-.48 0-.24-.01-1.02-.01-1.85-2.78.6-3.37-1.18-3.37-1.18-.45-1.16-1.11-1.47-1.11-1.47-.9-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.9 1.53 2.35 1.09 2.92.83.09-.65.35-1.09.63-1.34-2.22-.25-4.56-1.11-4.56-4.94 0-1.09.39-1.98 1.03-2.68-.1-.25-.45-1.27.1-2.65 0 0 .84-.27 2.75 1.02a9.5 9.5 0 0 1 5 0c1.9-1.29 2.75-1.02 2.75-1.02.55 1.38.2 2.4.1 2.65.64.7 1.03 1.59 1.03 2.68 0 3.84-2.34 4.68-4.57 4.93.36.31.68.92.68 1.85 0 1.34-.01 2.42-.01 2.75 0 .27.18.58.69.48A10.01 10.01 0 0 0 22 12.2c0-5.52-4.48-10-10-10Z"
          />
        </svg>
      </a>
    </div>

    <p class="app-footer__author">{{ t('common.footer.author') }}</p>
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

.app-footer__socials {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.25rem;
}

.app-footer__social-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border: 3px solid var(--color-white);
  border-radius: 0.75rem;
  background: var(--color-accent-500);
  color: var(--color-white);
  box-shadow: 0 3px 0 var(--color-accent-700);
  transition:
    transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.15s ease;
}

.app-footer__social-btn svg {
  width: 1.15rem;
  height: 1.15rem;
}

.app-footer__social-btn:hover {
  transform: translateY(-3px) scale(1.08);
  box-shadow: 0 6px 0 var(--color-accent-700);
}

.app-footer__social-btn:active {
  transform: translateY(2px) scale(0.97);
  box-shadow: 0 1px 0 var(--color-accent-700);
}

.app-footer__author {
  margin: 0.25rem 0 0;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--color-white);
}
</style>
