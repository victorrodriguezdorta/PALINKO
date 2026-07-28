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
      meta: { title: 'Human or AI' },
    },
    {
      path: '/room/:code',
      name: 'Room',
      component: () => import('../views/RoomView.vue'),
      meta: { title: 'Sala' },
    },
  ],
})

router.beforeEach((to) => {
  document.title = `${to.meta.title} | Human or AI`
})

export default router
