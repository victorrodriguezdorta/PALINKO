<p align="center">
  <img src="frontend/src/assets/images/logo.svg" alt="Palinko logo" width="160" />
</p>

<h1 align="center">Palinko</h1>

<p align="center">
  A real-time multiplayer word-chain party game where one (or more) of your friends might secretly be an AI.
</p>

## About the game

Palinko is a social deduction word game played in real time with a group of friends. Each round, players take
turns extending a shared chain of related words, one word at a time, racing against a shared clock. Hidden among
the human players are one or more **infiltrators** — players secretly guided towards steering the chain off
course. After the word chain phase ends, everyone votes on who they think the infiltrator was, and the reveal
phase shows who was right.

Behind the scenes, every submitted word is checked for spelling and semantic relatedness to the previous word in
the chain (spell-corrected and scored via a local ONNX embedding model, optionally backed by a Groq-powered LLM
check) before it's accepted into the chain — so bluffing your way through isn't as easy as just typing something
plausible-sounding.

Rooms, turns, voting and reveals are all synchronized live across players over WebSockets, with a lobby/room flow
for creating or joining a game with friends.

## Tech stack

**Backend** — `backend/`
- Java 21, Spring Boot 3 (Web, WebSocket/STOMP, Validation, Actuator)
- Hexagonal / clean architecture (`domain` → `application` → `infrastructure`) per feature slice
- ONNX Runtime + DJL HuggingFace tokenizers for local semantic word-relatedness scoring
- Hunspell (via JNA bindings) for per-language spell correction
- Optional Groq API integration for LLM-assisted word-relation checking
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito for tests
- Maven (wrapper included)

**Frontend** — `frontend/`
- Vue 3 (Composition API) + TypeScript
- Vite
- Pinia for state management
- Vue Router
- Tailwind CSS
- `@stomp/stompjs` + `sockjs-client` for WebSocket/STOMP communication with the backend
- vue-i18n (English/Spanish)
- Axios

## Repository layout

```
guessTheAI/
├── backend/    # Spring Boot API + WebSocket game server
└── frontend/   # Vue 3 + Vite single-page app
```

Each project has its own build tooling — there is no top-level build. `cd` into the relevant directory before
running any commands below.

## Prerequisites

- Java 21 (JDK)
- Maven (or use the included `mvnw` / `mvnw.cmd` wrapper — no local Maven install needed)
- Node.js 18+ (20+ recommended)
- npm

No database is required — game state (rooms, rounds, players) lives in memory on the backend.

## Getting started

### 1. Clone the repository

```bash
git clone <this-repository-url>
cd guessTheAI
```

### 2. Backend setup

```bash
cd backend
```

Configuration lives in `src/main/resources/application.properties`. The defaults work out of the box; the only
optional setting is:

- `GROQ_API_KEY` — environment variable for enabling the Groq-backed LLM word-relation checker. If unset, the
  app falls back to the local ONNX-based checker only.

Run the backend:

```bash
# Windows
mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080` and exposes:
- REST/WebSocket endpoints under `/ws` (STOMP over SockJS)
- Swagger UI at `http://localhost:8080/swagger-ui/index.html`

Other useful backend commands (run from `backend/`):

```bash
# Compile only
./mvnw compile

# Full build
./mvnw clean install

# Run the full test suite
./mvnw test

# Run a single test class
./mvnw test -Dtest=GameApplicationServiceTest

# Run a single test method
./mvnw test -Dtest=RoundTest#testMethodName
```

On Windows, replace `./mvnw` with `mvnw.cmd`.

### 3. Frontend setup

```bash
cd frontend
npm install
```

The frontend talks to the backend via two environment variables, already set for local development in
`.env.development`:

```
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
```

If you need to point the frontend at a different backend (e.g. a Cloudflare tunnel for testing with remote
friends), copy `.env.development.local.example` to `.env.development.local` and edit the URLs there — this file
is gitignored and overrides `.env.development`.

Start the dev server:

```bash
npm run dev
```

The app will be available at the URL Vite prints (typically `http://localhost:5173`).

Other useful frontend commands:

```bash
# Type-check + production build
npm run build

# Preview a production build locally
npm run preview

# Lint (auto-fix)
npm run lint

# Format
npm run format
```

### 4. Play

With both the backend (`:8080`) and frontend (`:5173`) running, open the frontend URL in a browser, create a
room, and share the room code with friends so they can join from their own browsers.

## Running both together

There's no single command to start both services — run each in its own terminal:

```bash
# Terminal 1
cd backend && ./mvnw spring-boot:run

# Terminal 2
cd frontend && npm run dev
```

## Testing

- Backend: `cd backend && ./mvnw test` (JUnit 5 + Mockito, no external services required)
- Frontend: type-checking runs as part of `npm run build` (`vue-tsc --build`)
