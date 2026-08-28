<template>
  <section class="account-cta" aria-labelledby="account-cta-title">
    <div class="account-cta__content">
      <span class="account-cta__eyebrow">{{ t('home.accountCta.eyebrow') }}</span>
      <h2 id="account-cta-title" class="account-cta__title">{{ t('home.accountCta.heading') }}</h2>
      <p class="account-cta__text">{{ t('home.accountCta.text') }}</p>
      <router-link class="account-cta__button" to="/login?mode=signup">
        {{ t('home.accountCta.cta') }}
      </router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<style scoped>
.account-cta {
  position: relative;
  margin-top: 3.5rem;
  padding: 3.5rem 1.5rem;
  border-radius: 1.5rem;
  border: 2px solid var(--color-gray-200, #eaecf0);
  background: var(--color-white);
  box-shadow: 0 8px 0 color-mix(in srgb, var(--color-black) 12%, transparent);
}

.account-cta__content {
  position: relative;
  max-width: 34rem;
  margin: 0 auto;
  text-align: center;
}

.account-cta__eyebrow {
  color: var(--color-gray-500);
  font-family: 'Outfit', sans-serif;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.account-cta__title {
  margin-top: 0.45rem;
  color: var(--color-black);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 2rem;
  font-weight: 700;
  line-height: 1.1;
}

.account-cta__text {
  max-width: 29rem;
  margin: 0.85rem auto 1.5rem;
  color: var(--color-black);
  font-family: 'Outfit', sans-serif;
  line-height: 1.5;
}

.account-cta__button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.9rem;
  padding: 0.7rem 1.4rem;
  border: 3px solid var(--color-black);
  border-radius: 1rem;
  background: var(--color-accent-500);
  box-shadow: 0 5px 0 var(--color-accent-700);
  color: var(--color-white);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 700;
  text-decoration: none;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.account-cta__button:hover {
  transform: translateY(-2px);
  box-shadow: 0 7px 0 var(--color-accent-700);
}

.account-cta__button:active {
  transform: translateY(3px);
  box-shadow: 0 2px 0 var(--color-accent-700);
}

@media (max-width: 639px) {
  .account-cta {
    padding: 3rem 1rem;
  }

  .account-cta__title {
    font-size: 1.7rem;
  }
}
</style>
