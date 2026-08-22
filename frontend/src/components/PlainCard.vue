<template>
  <section class="plain-card">
    <h2 v-if="$slots.title" class="plain-card__title">
      <slot name="title" />
    </h2>
    <div class="plain-card__content">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
// Generic white rounded card with no animation, blob edge, or accent
// border/shadow — for neutral surfaces like login/account forms, kept
// deliberately independent of CartoonCard so it never inherits its
// playful styling or animations.
</script>

<style scoped>
.plain-card {
  border-radius: 1.5rem;
  background: var(--color-white);
  box-shadow: 0 2px 12px -4px color-mix(in srgb, var(--color-gray-900) 20%, transparent);
  padding: 1.35rem 1.5rem;
}

.plain-card__title {
  margin-bottom: 0.6rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: var(--color-gray-800);
}

.plain-card__content {
  font-family: 'Outfit', sans-serif;
}
</style>
