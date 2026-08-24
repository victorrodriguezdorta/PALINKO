import { i18n } from '@/i18n'
import { trackVisit } from '@/services/analyticsService'
import { useAuthStore } from '@/stores/auth'
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { left: 0, top: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('../views/HomeView.vue'),
      meta: { titleKey: 'common.appTitle' },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginView.vue'),
      meta: { titleKey: 'auth.pageTitle', guestOnly: true },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { titleKey: 'profile.pageTitle', requiresAuth: true },
    },
    {
      path: '/play',
      name: 'PlayerSetup',
      component: () => import('../views/PlayerSetupView.vue'),
      meta: { titleKey: 'playerSetup.pageTitle' },
    },
    {
      path: '/room/:code',
      name: 'Room',
      component: () => import('../views/RoomView.vue'),
      meta: { titleKey: 'room.pageTitle' },
    },
    {
      path: '/debug/word-relation',
      name: 'DebugWordRelation',
      component: () => import('../views/DebugWordRelationView.vue'),
      meta: { titleKey: 'debug.wordRelation.heading' },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('../views/NotFoundView.vue'),
      meta: { titleKey: 'common.notFound.title' },
    },
  ],
})

let hasTrackedFirstVisit = false

router.beforeEach(async (to) => {
  if (to.meta.requiresAuth || to.meta.guestOnly) {
    const authStore = useAuthStore()
    // Awaits main.ts's initial rehydration (or an already-settled result)
    // so a hard reload of /profile or /login checks the real session state
    // instead of momentarily seeing "logged out" before the stored token
    // loads.
    await authStore.initialize()
    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      return '/'
    }
    if (to.meta.guestOnly && authStore.isAuthenticated) {
      return '/'
    }
  }

  const appTitle = i18n.global.t('common.appTitle')
  const pageTitle = to.meta.titleKey ? i18n.global.t(to.meta.titleKey as string) : appTitle
  document.title = `${pageTitle} | ${appTitle}`

  if (!hasTrackedFirstVisit) {
    hasTrackedFirstVisit = true
    trackVisit()
  }
})

export default router
