# PALINKO — Frontend

SPA en **Vue 3 (Composition API) + TypeScript**, construida con **Vite**, que consume la API y el WebSocket del
[backend](../backend/README.md) de [PALINKO](../README.md).

> 📘 Para la visión general del proyecto (qué es PALINKO, cómo se juega, cómo razona la IA) consulta el
> [README raíz](../README.md). Para la API que consume este cliente (auth, rachas, leaderboard, cifrado)
> consulta el [README del backend](../backend/README.md).

## 🛠️ Stack técnico

- **Vue 3.5** (`<script setup lang="ts">`, Composition API) + **TypeScript**
- **Vite 6** como build tool, `vue-tsc` para el type-checking
- **Pinia 4** para estado global (stores estilo "setup", `defineStore('name', () => {...})`)
- **Vue Router 4**, con guardas de navegación para rutas autenticadas
- **Tailwind CSS 4** + bloques `<style scoped>` con custom properties (`--color-*`) como sistema de diseño;
  componentes propios (`CartoonButton`, `CartoonCard`, `PlainCard`...), sin librería de UI externa
- **`@stomp/stompjs` + `sockjs-client`** para la partida en tiempo real por WebSocket/STOMP
- **Axios** para las llamadas REST
- **vue-i18n** (español/inglés)
- **lucide-vue-next** para iconos
- ESLint 9 (flat config) + Prettier para lint/formato. No hay framework de tests configurado (no Vitest/Jest/Cypress).

## 📁 Estructura (`src/`)

```
src/
├── App.vue                # shell raíz: fondo animado + <RouterView> + footer
├── main.ts                # bootstrap de la app (Pinia, router, i18n, rehidratación de sesión)
├── assets/                 # main.css (entrada de Tailwind), theme.ts (tokens), sonidos, imágenes
├── components/              # ~25 componentes de presentación / feature
├── composables/
│   ├── useAuthForm.ts        # estado y handlers compartidos por login/signup
│   └── useSound.ts            # composable de efectos de sonido
├── i18n/                     # instancia vue-i18n, locales es/en, traducción de errores de API
├── router/index.ts           # tabla de rutas + guardas de auth
├── services/                  # capa de acceso a la API/WebSocket (ver abajo)
├── stores/                    # Pinia: auth.ts, game.ts, leaderboard.ts, locale.ts
├── types/                     # interfaces TS compartidas: user.ts, game.ts, leaderboard.ts
├── utils/                      # avatar.ts, clipboard.ts
└── views/                      # componentes de página (uno por ruta)
```

No hay carpeta `api/` (las llamadas viven en `services/`) ni `layouts/` (`App.vue` es el único layout).

## 🔌 Comunicación con el backend

- **Cliente REST** (`src/services/apiClient.ts`): instancia de Axios con `baseURL: import.meta.env.VITE_API_BASE_URL`.
  Un interceptor de request lee el JWT directamente de `localStorage` y añade la cabecera
  `Authorization: Bearer <token>` — deliberadamente sin importar el store de Pinia aquí, para evitar problemas de
  orden de inicialización ("no active Pinia").
- **Variables de entorno** (`.env.development`):
  ```env
  VITE_API_BASE_URL=http://localhost:8080
  VITE_WS_URL=http://localhost:8080/ws
  ```
  Para apuntar a otro backend (por ejemplo un túnel de Cloudflare para jugar con amigos en remoto), copia
  `.env.development.local.example` a `.env.development.local` (git-ignorado, sobreescribe `.env.development`).
- **Normalización de errores** (`src/services/apiError.ts`): `toApiError()` convierte cualquier fallo de Axios en
  un objeto estable `{ code, message, args }`; `i18n/index.ts` traduce `error.code` a claves `errors.<code>`.
- **Servicios** (`src/services/`):
  - `authService.ts` — signup, login, verify-account, resend-verification, request-password-reset, reset-password,
    getCurrentUser, updateProfile, changePassword.
  - `roomService.ts` — createRoom, createDailyRoom, joinRoom (REST).
  - `leaderboardService.ts` — `GET /leaderboard/daily`.
  - `gameSocket.ts` — clase `GameSocket` que envuelve `@stomp/stompjs` sobre `sockjs-client`
    (`webSocketFactory: () => new SockJS(VITE_WS_URL)`); la identidad (código de sala, id de jugador, token de
    reconexión) viaja en las cabeceras del `CONNECT` de STOMP. Suscrita a `/user/queue/room-updates`,
    `/user/queue/errors` y `/topic/rooms/{code}/typing`; publica en
    `/app/rooms/{code}/start|word|vote|play-again|kick|rewind|update-settings|typing`.
  - `analyticsService.ts` — wrapper fino sobre `window.gtag`.
  - `debugService.ts` — `POST /debug/word-relation`, usado por la vista de depuración.

## 🔐 Autenticación

- **Ruta**: `/login` → `src/views/LoginView.vue` (meta `guestOnly: true`, redirige si ya hay sesión). Una única
  tarjeta con dos pestañas (Login / Signup); la lógica compartida vive en `src/composables/useAuthForm.ts`,
  incluyendo el manejo especial del error `WEAK_PASSWORD` (checklist de reglas de contraseña).
- **Store** (`src/stores/auth.ts`, `useAuthStore`):
  - `token` — se inicializa síncronamente leyendo `localStorage.getItem('palinko:auth-token')`.
  - `user` — perfil (`AuthUser`: `id, username, avatarSeed, role, currentStreak`; deliberadamente sin email).
  - `isAuthenticated = computed(() => !!token && !!user)`.
  - `login()`, `signup()` (no inicia sesión automáticamente), `logout()`, `fetchCurrentUser()` (`GET /users/me`),
    `updateProfile()` (vuelve a persistir el token porque el `subject` del JWT es el username, así que cambiar de
    nombre invalida el token anterior), `changePassword()`.
  - `initialize()` — rehidrata la sesión al arrancar: si hay token guardado, llama a `fetchCurrentUser()`; si
    falla (401, token expirado/inválido) limpia el token silenciosamente. Memoizado — llamadas repetidas devuelven
    la misma promesa en curso o ya resuelta, así que es seguro invocarlo desde varios sitios sin duplicar trabajo.
- **Dónde vive el token**: `localStorage` bajo la clave `palinko:auth-token` (no `sessionStorage`, para que la
  sesión persista entre pestañas y recargas — ver el comentario en `stores/auth.ts`).
- **Persistencia entre recargas**: `src/main.ts` llama a `useAuthStore().initialize()` justo después de montar
  Pinia/router (antes de `app.mount`); además, la guarda `beforeEach` del router (`src/router/index.ts`) hace
  `await authStore.initialize()` para cualquier ruta con `meta.requiresAuth` o `meta.guestOnly`, de modo que
  recargar `/profile` no muestre brevemente "sin sesión" antes de que se resuelva la comprobación del token.
- **Gestión de perfil**: `/profile` → `src/views/ProfileView.vue` (meta `requiresAuth: true`) — editar
  username/avatar y cambiar contraseña, con el mismo tratamiento de `WEAK_PASSWORD`.
- **Cabecera**: `src/components/AppHeader.vue` muestra un botón "Iniciar sesión" o `UserMenu.vue`
  (perfil/logout) según `authStore.user`; se oculta en `LoginView`/`RoomView`/`PlayerSetupView` donde no tiene
  sentido mostrarlo a mitad de partida.

## 🔥 Rachas (streaks) en la UI

- **Fuente de datos**: `AuthUser.currentStreak` (de `/users/me` y de la respuesta de login) y
  `DailyLeaderboardEntry.currentStreak` (de `/leaderboard/daily`).
- **Componente principal**: `src/components/StreakFlame.vue` — insignia animada con un icono de llama y el
  número de días, con prop `compact` (sin fondo ni etiqueta, para uso inline como en las filas del leaderboard) y
  prop `color`. Animaciones de parpadeo/balanceo, desactivadas bajo `prefers-reduced-motion`.
  - Usado en `HomeContent.vue` (esquina superior de la tarjeta del reto diario, solo si
    `authStore.isAuthenticated`, ligado a `authStore.user?.currentStreak`).
  - Usado en modo compacto dentro de `DailyLeaderboardCard.vue`, por fila (`v-if="entry.currentStreak > 0"`).
- **Aviso contextual**: `src/components/StreakHintCard.vue` — banner descartable mostrado desde `RoomView.vue`
  para avisar al jugador sobre su racha durante el reto diario.
- No existe una página de "historial de racha" — solo se muestra el contador actual como insignia.

## 🏆 Leaderboard en la UI

- No hay una ruta `/leaderboard` independiente — el ranking diario se muestra embebido dentro de la vista de
  sala (`RoomView.vue`), solo en partidas de tipo "reto diario".
- **Componente**: `src/components/DailyLeaderboardCard.vue` — renderiza una lista ordenada a partir del prop
  `entries: DailyLeaderboardEntry[]` (`rank, userId, username, avatarSeed, wordCount, accuracyPercent,
  currentStreak`), con iconos de medalla para las 3 primeras posiciones, avatar, racha en formato compacto, y
  resaltado de la fila del propio jugador.
- **Store** (`src/stores/leaderboard.ts`, `useLeaderboardStore`): `entries`, `loading` y `fetchDaily()`, que
  llama a `leaderboardService.getDailyLeaderboard()` (`GET /leaderboard/daily`).
- **Cuándo se pide**: en `RoomView.vue`, una vez cuando una sala diaria pasa a `IN_PROGRESS` y de nuevo cuando
  llega a `FINISHED`.
- **Dónde se muestra**: en directo bajo el tablero mientras la sala diaria está `IN_PROGRESS`, y de nuevo tras la
  revelación cuando está `FINISHED` (junto a `GameResultsCard`). A los usuarios anónimos se les muestra además
  una invitación a iniciar sesión para aparecer en el ranking.

## 🎮 Vista principal del juego

- **Ruta**: `/room/:code` → `src/views/RoomView.vue` — todo el ciclo de vida de la sala:
  `LOBBY → IN_PROGRESS (fases WORD_CHAIN / VOTING) → FINISHED` (o `CLOSED`).
- **Preparación**: `/play` → `src/views/PlayerSetupView.vue` recoge nombre/avatar para jugadores anónimos antes
  de crear/unirse a una sala (se salta para usuarios autenticados).
- **Mecánica**: los jugadores envían palabras por turnos (`gameStore.submitWord`), evaluadas por IA en el
  backend según su relación semántica con la palabra anterior. El tablero principal
  (`src/components/ChainBoardCard.vue`) muestra la cadena de palabras aceptadas (con % de relación y
  justificación de la IA), intentos rechazados, y el input de envío.
- Tras la fase de cadena hay una votación (`VOTING`) y una revelación final mostrada por
  `src/components/GameResultsCard.vue` (cadena completa, precisión media, texto para compartir, y en salas no
  diarias, la puntuación final por jugador).
- **Modo Reto diario**: variante en solitario, siempre `IN_PROGRESS`, apta para anónimos
  (`gameStore.createDailyRoom`), con `DailyChallengeBanner`, `DailyCountdown` (cuenta atrás hasta la próxima
  medianoche UTC) y el `DailyLeaderboardCard` descrito arriba.

## ✅ Requisitos previos

- Node.js 18+ (recomendado 20+)
- npm
- El [backend](../backend/README.md) corriendo en local (o apuntar `VITE_API_BASE_URL`/`VITE_WS_URL` a un
  despliegue remoto)

## 🚀 Puesta en marcha

```bash
npm install
npm run dev
```

La app estará disponible en la URL que imprima Vite (normalmente `http://localhost:5173`).

## 🧪 Comandos útiles

```bash
# Type-check + build de producción
npm run build

# Previsualizar un build de producción
npm run preview

# Lint (auto-fix)
npm run lint

# Formatear
npm run format
```

No hay suite de tests configurada en este proyecto (sin Vitest/Jest/Cypress); la verificación de tipos
(`vue-tsc --build`) corre como parte de `npm run build`.
