<template>
  <div ref="root" class="user-menu" :class="{ 'user-menu--open': isOpen }">
    <button
      type="button"
      class="user-menu__trigger"
      aria-haspopup="menu"
      :aria-expanded="isOpen"
      @click="toggle"
      @keydown="onTriggerKeydown"
    >
      <PlayerAvatar :seed="user.avatarSeed" size="sm" />
      <span class="user-menu__username">{{ user.username }}</span>
      <ChevronDown class="user-menu__chevron" :size="14" aria-hidden="true" />
    </button>

    <Transition name="user-menu-panel">
      <ul v-if="isOpen" class="user-menu__panel" role="menu">
        <li class="user-menu__item" role="menuitem" @click="onProfile">
          <UserCog :size="16" aria-hidden="true" />
          <span>{{ profileLabel }}</span>
        </li>
        <li class="user-menu__item user-menu__item--danger" role="menuitem" @click="onLogout">
          <LogOut :size="16" aria-hidden="true" />
          <span>{{ logoutLabel }}</span>
        </li>
      </ul>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ChevronDown, LogOut, UserCog } from 'lucide-vue-next'
import PlayerAvatar from '@/components/PlayerAvatar.vue'
import type { AuthUser } from '@/types/user'

defineProps<{
  user: AuthUser
  profileLabel: string
  logoutLabel: string
}>()

const emit = defineEmits<{
  'profile-click': []
  'logout-click': []
}>()

const root = ref<HTMLElement | null>(null)
const isOpen = ref(false)

function toggle() {
  isOpen.value = !isOpen.value
}

function close() {
  isOpen.value = false
}

function onProfile() {
  close()
  emit('profile-click')
}

function onLogout() {
  close()
  emit('logout-click')
}

function onTriggerKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') close()
}

function onClickOutside(event: MouseEvent) {
  if (root.value && !root.value.contains(event.target as Node)) close()
}

onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))
</script>

<style scoped>
.user-menu {
  position: relative;
  z-index: 10;
  display: inline-flex;
  flex-shrink: 0;
  margin-top: 1rem;
}

.user-menu__trigger {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-shrink: 0;
  border: 3px solid var(--color-black);
  border-radius: 1rem;
  background: var(--color-white);
  padding: 0.3rem 0.7rem 0.3rem 0.3rem;
  box-shadow: 0 4px 0 var(--color-brand-200);
  color: var(--color-brand-700);
  cursor: pointer;
  transition:
    transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.15s ease;
}

.user-menu__trigger:hover {
  transform: translateY(-2px) scale(1.03);
  box-shadow: 0 6px 0 var(--color-brand-200);
}

.user-menu__trigger:active {
  transform: translateY(1px) scale(0.98);
  box-shadow: 0 2px 0 var(--color-brand-200);
}

.user-menu--open .user-menu__trigger {
  box-shadow: 0 2px 0 var(--color-brand-200);
  transform: translateY(2px);
}

.user-menu__username {
  max-width: 8rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 0.8rem;
}

.user-menu__chevron {
  color: var(--color-brand-500);
  transition: transform 0.2s ease;
}

.user-menu--open .user-menu__chevron {
  transform: rotate(180deg);
}

.user-menu__panel {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  z-index: 20;
  min-width: 11rem;
  margin: 0;
  padding: 0.4rem;
  list-style: none;
  border: 3px solid var(--color-black);
  border-radius: 1rem;
  background: var(--color-white);
  box-shadow: 0 6px 0 var(--color-brand-200);
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.user-menu__item {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.5rem 0.6rem;
  border-radius: 0.7rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--color-gray-800);
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.user-menu__item:hover {
  background: var(--color-brand-50);
}

.user-menu__item--danger {
  color: var(--color-error-700, #b91c1c);
}

.user-menu__item--danger:hover {
  background: var(--color-error-100, #fee2e2);
}

.user-menu-panel-enter-active,
.user-menu-panel-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.user-menu-panel-enter-from,
.user-menu-panel-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.96);
}

@media (prefers-reduced-motion: reduce) {
  .user-menu__trigger,
  .user-menu__chevron,
  .user-menu-panel-enter-active,
  .user-menu-panel-leave-active {
    transition: none;
  }
}
</style>
