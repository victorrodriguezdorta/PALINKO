import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import { useAuthStore } from './stores/auth'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)

// Re-hydrate any stored session before the router's requiresAuth guard runs
// (see router/index.ts), so a reload of /profile doesn't briefly look
// logged-out and bounce to Home before the token is checked.
useAuthStore().initialize()

app.mount('#app')
