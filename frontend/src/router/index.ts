import { createRouter, createWebHistory } from 'vue-router'
import { i18n } from '@/i18n'

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
  ],
})

router.beforeEach((to) => {
  const appTitle = i18n.global.t('common.appTitle')
  const pageTitle = to.meta.titleKey ? i18n.global.t(to.meta.titleKey as string) : appTitle
  document.title = `${pageTitle} | ${appTitle}`
})

export default router
