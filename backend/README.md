# PALINKO — Backend

API REST + servidor de juego en tiempo real (WebSocket/STOMP) para [PALINKO](../README.md), escrita en **Java 21 /
Spring Boot 3** con arquitectura hexagonal (`domain` → `application` → `infrastructure`) dividida en tres slices de
funcionalidad independientes.

> 📘 Para la visión general del proyecto (qué es PALINKO, cómo se juega, cómo razona la IA) consulta el
> [README raíz](../README.md). Para el cliente Vue que consume esta API, consulta el
> [README del frontend](../frontend/README.md).

## 📁 Módulos

```
src/main/java/com/kawser/cleanspringbootproject/
├── game/          # Salas, turnos, cadena de palabras, reto diario, evaluación por IA — todo en memoria
├── user/          # Cuentas: signup/login/JWT, verificación por email, rachas — persistido en PostgreSQL
├── leaderboard/   # Ranking diario — persistido en PostgreSQL
├── exception/     # Manejadores de error transversales (fuera de los tres slices anteriores)
└── config/        # Configuración de aplicación no específica de un slice
```

Cada slice sigue el mismo patrón interno:

- `domain/` — modelo puro (sin anotaciones de framework) y excepciones de dominio.
- `application/` — casos de uso (`port/in`), puertos de salida (`port/out`) que la infraestructura implementa, y
  el servicio que los orquesta.
- `infrastructure/adapter/in/rest` — controladores REST.
- `infrastructure/adapter/out/persistence` — entidades JPA y repositorios, cuando el slice persiste datos.
- `infrastructure/config` — wiring de Spring (beans, seguridad, propiedades) específico del slice.

Los slices se comunican entre sí a través de puertos, nunca importando clases internas de otro módulo. Por
ejemplo, `game` no depende de nada bajo `user`: cuando termina el reto diario, llama a su propio puerto de salida
`DailyStreakTracker`, que `user.infrastructure.adapter.out.game.UserDailyStreakTracker` implementa delegando en el
caso de uso real (`RecordDailyCompletionUseCase`). Lo mismo ocurre con `DailyLeaderboardRecorder` hacia el módulo
`leaderboard`.

## 🗄️ Persistencia: qué vive en memoria y qué en PostgreSQL

- **`game`** sigue completamente en memoria (sin base de datos) — salas, turnos, cadena de palabras y votos se
  descartan al terminar la partida. Ver `InMemoryRoomRepository`.
- **`user`** y **`leaderboard`** persisten en **PostgreSQL** vía Spring Data JPA, con **Flyway** como única fuente
  de verdad del esquema (`spring.jpa.hibernate.ddl-auto=validate` — Hibernate nunca crea ni altera tablas).

Migraciones (`src/main/resources/db/migration/`):

| Migración | Contenido |
|---|---|
| `V1__create_users_table.sql` | Tabla `users`: credenciales, rol, avatar, estado de verificación, OTP |
| `V2__add_streak_to_users.sql` | Añade `current_streak` y `last_streak_date` a `users` |
| `V3__create_daily_leaderboard.sql` | Tabla `daily_leaderboard_entries` (una fila por usuario y día) |
| `V4__add_bot_flag_to_users.sql` | Añade `is_bot` a `users`, para excluir cuentas de relleno del ranking |

### Esquema

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(15) NOT NULL,
    password TEXT NOT NULL,          -- hash BCrypt, nunca texto plano
    role VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    avatar_seed VARCHAR(64) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT false,
    otp VARCHAR(6),
    otp_generation_time TIMESTAMP,
    current_streak INT NOT NULL DEFAULT 0,
    last_streak_date DATE,
    is_bot BOOLEAN NOT NULL DEFAULT false
);
-- índices únicos en username y email

CREATE TABLE daily_leaderboard_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    challenge_date DATE NOT NULL,
    word_count INT NOT NULL,
    accuracy_percent INT NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_daily_leaderboard_user_date UNIQUE (user_id, challenge_date)
);
-- índice compuesto (challenge_date, word_count ASC, accuracy_percent DESC) para el ranking
```

Un proveedor gestionado como [Neon](https://neon.tech) funciona bien para desarrollo/despliegue gratuito; su capa
free suspende la base tras 5 minutos de inactividad, por eso existe `DatabaseKeepAliveTask`, una tarea programada
que hace `SELECT 1` cada `app.db.keep-alive.interval-seconds` (240s por defecto) para mantenerla despierta.

## 🔐 Autenticación y cifrado

### Registro (`POST /auth/signup`)

1. Se valida el payload (`SignupRequest`): usuario, email, contraseña con reglas de fortaleza
   (`PasswordConstraintValidator`).
2. La contraseña se **hashea con BCrypt** (`BCryptPasswordHasher`, que delega en el único bean
   `PasswordEncoder` = `BCryptPasswordEncoder` declarado en `SecurityConfig`) — **nunca se guarda en texto
   plano**, ni siquiera temporalmente; el hash resultante (con su salt embebido, formato estándar `$2a$...`) es lo
   único que llega a la columna `password`.
3. Se genera un avatar aleatorio (`RandomAvatarSeedGenerator`) y, si `EMAIL_VERIFICATION_ENABLED=true`, un OTP de
   un solo uso (`SecureRandomOtpGenerator`, basado en `java.security.SecureRandom`) que se envía por email
   (`JavaMailVerificationEmailSender`) y se guarda junto a su timestamp de generación para poder expirarlo.
   Con la verificación desactivada (valor por defecto en local), la cuenta se crea ya `enabled=true` y el usuario
   puede iniciar sesión de inmediato — pensado para no requerir SMTP en desarrollo.

### Login (`POST /auth/login`) y JWT

1. Se busca el usuario por `username` y se compara la contraseña recibida contra el hash guardado con
   `PasswordEncoder.matches(...)` (BCrypt recalcula el hash con el mismo salt embebido y compara en tiempo
   constante — la contraseña en claro nunca se compara directamente contra nada).
2. Si coincide y la cuenta está habilitada, se emite un **JWT firmado con HMAC-SHA256** (`Auth0TokenIssuer`, usando
   `com.auth0:java-jwt`), con:
   - `issuer`: `palinko-auth`
   - `subject`: el username
   - `expiresAt`: ahora + `app.auth.jwt.expiration-seconds` (7200s / 2h por defecto)
3. El token se firma con un secreto simétrico (`app.auth.jwt.secret`, variable de entorno `JWT_SECRET`) que debe
   ser un valor largo y aleatorio en cualquier despliegue real — el valor por defecto
   (`dev-only-insecure-secret-change-me`) solo es seguro en local.

### Cómo se valida cada request (`JwtAuthenticationFilter`)

Spring Security intercepta **todas** las rutas por defecto en cuanto se añade `spring-boot-starter-security`, así
que las rutas públicas se declaran explícitamente en `SecurityConfig.securityFilterChain`:

- Públicas: `/auth/**` (signup, login, verificación), `/password/**` (reset), `/rooms`, `/rooms/daily`,
  `/rooms/*/join`, `/ws/**` (WebSocket), `/leaderboard/**` (el ranking es visible sin cuenta), `/debug/**`,
  Swagger/Actuator, y las preflight `OPTIONS` de CORS.
- Todo lo demás requiere autenticación (`/users/me`, `/users/me/password`, ...).

Un filtro propio (`JwtAuthenticationFilter`, insertado antes de
`UsernamePasswordAuthenticationFilter`) se ejecuta en cada request:

1. Lee la cabecera `Authorization: Bearer <token>`.
2. Verifica la firma y el `issuer` del JWT contra el mismo secreto (`Auth0TokenIssuer.validateToken`) — un token
   caducado, con firma inválida o manipulado no lanza excepción, simplemente no autentica la request (devuelve
   `Optional.empty()`).
3. Si el token es válido, carga el `UserDetails` correspondiente (`SpringUserDetailsAdapter`) y puebla el
   `SecurityContext` para esa request — sesión completamente **stateless**, sin cookies de sesión ni estado
   compartido entre requests (`SessionCreationPolicy.STATELESS`).

La sesión no vive en el servidor: el propio JWT es la prueba de identidad, y el cliente lo reenvía en cada
llamada (ver [README del frontend](../frontend/README.md#autenticación)).

### Resumen de cifrado usado

| Qué se protege | Cómo |
|---|---|
| Contraseña de usuario en BD | Hash BCrypt (salt aleatorio embebido, factor de coste por defecto de Spring Security) — nunca texto plano |
| Sesión de usuario / autorización | JWT firmado con HMAC-SHA256, secreto en `JWT_SECRET`, expira a las 2h |
| Código de verificación (OTP) | Generado con `SecureRandom` (criptográficamente seguro), de un solo uso, expira por timestamp |
| Transporte | HTTPS en producción (a cargo del proveedor de despliegue); CORS restringido por `APP_CORS_ALLOWED_ORIGINS` |

## 🔥 Rachas diarias (streaks)

Modeladas como estado inmutable en el propio dominio `User` (`User.withDailyCompletion` /
`User.currentStreakAsOf`), no como un job aparte:

- Al completar el reto diario, `game` llama al puerto `DailyStreakTracker.recordDailyCompletion(username)`, que
  el módulo `user` traduce en `RecordDailyCompletionUseCase`.
- **Regla de continuidad** (día natural UTC, la misma convención que usa `DailySeed` para repartir el reto del
  día):
  - Completar el reto el mismo día en que ya se había extendido la racha → no-op (no se puede duplicar sumando
    puntos por rejugar el mismo día).
  - Completar el reto exactamente al día siguiente del último completado → la racha se incrementa en 1.
  - Cualquier hueco de más de un día → la racha se reinicia a 1 (no a 0, porque la finalización de hoy ya cuenta).
- **Lectura sin job de medianoche:** `currentStreakAsOf(today)` es una proyección de solo lectura — si el último
  día jugado no fue ni hoy ni ayer, se devuelve 0 aunque la fila en BD todavía tenga el valor antiguo. Así la
  racha "caduca" visualmente en el momento en que se consulta, sin necesidad de un cron a medianoche que recorra
  todas las filas.
- Persistida en `users.current_streak` (entero) y `users.last_streak_date` (fecha del último día completado).

## 🏆 Leaderboard diario

- **Qué mide:** para el reto diario de hoy (UTC), cada usuario tiene como mucho una fila por día en
  `daily_leaderboard_entries`. `JpaDailyLeaderboardRepository.upsertIfBetter` solo sobrescribe el intento
  guardado si el nuevo es **estrictamente mejor**: gana quien resuelve el reto con **menos palabras**; en caso de
  empate en número de palabras, gana **mayor precisión semántica media** (`accuracy_percent`). Un reintento peor
  nunca desplaza a uno anterior mejor.
- **Cómo se calcula el ranking:** `topForDate` ordena por el índice
  `(challenge_date, word_count ASC, accuracy_percent DESC)` y limita a los 10 primeros
  (`LeaderboardApplicationService.TOP_SIZE`); la posición (`rank`) se asigna en memoria según el orden de llegada
  de las filas, no se guarda en BD.
- **Endpoint:** `GET /leaderboard/daily` — público, no requiere autenticación (`LeaderboardController`). Devuelve
  para cada entrada: posición, usuario, avatar, palabras usadas, precisión y racha actual del jugador.
- **Sin caché:** cada llamada consulta directamente PostgreSQL; el volumen (top 10 de un único día) no lo
  justifica.
- `DailyLeaderboardMockTask` (activable por configuración) puede rellenar el ranking con entradas de cuentas
  `is_bot=true`, útiles para no mostrar una tabla vacía en un despliegue recién estrenado.

## 🛠️ Stack técnico

- **Java 21**, **Spring Boot 3** (Web, WebSocket/STOMP, Security, Data JPA, Validation, Mail, Actuator)
- **PostgreSQL** (vía `spring-boot-starter-data-jpa` + driver JDBC oficial) para los módulos `user` y
  `leaderboard`; **Flyway** gestiona el esquema
- **Spring Security** (stateless, sin sesiones) + **`com.auth0:java-jwt`** para firmar/verificar JWT + **BCrypt**
  para contraseñas
- LLM para juicio semántico de palabras: **Groq** (`llama-3.1-8b-instant`) como proveedor primario, con fallback
  automático a **OpenRouter**
- **Hunspell** (vía bindings JNA) para corrección ortográfica local por idioma
- **springdoc-openapi** (Swagger UI)
- **JUnit 5** + **Mockito** para tests
- **Maven** (wrapper incluido)
- **Docker** (`Dockerfile` incluido para despliegue)

## ✅ Requisitos previos

- Java 21 (JDK)
- Maven (o usa el wrapper incluido `mvnw` / `mvnw.cmd` — no hace falta Maven instalado)
- Una base de datos **PostgreSQL** accesible (local vía Docker, o un proveedor gestionado como
  [Neon](https://neon.tech))

## 🚀 Configuración y arranque

1. Copia `.env.example` a `.env` (git-ignorado, cargado automáticamente al arrancar) y rellena:

   ```env
   # Conexión a PostgreSQL
   DB_URL=jdbc:postgresql://localhost:5432/palinko
   DB_USERNAME=postgres
   DB_PASSWORD=change-me

   # JWT
   JWT_SECRET=change-me-to-a-long-random-string
   JWT_EXPIRATION_SECONDS=7200

   # SMTP para verificación de cuenta / reset de contraseña (opcional en local)
   GMAIL_USERNAME=youremail@gmail.com
   GMAIL_APP_PASSWORD=your-16-char-app-password
   EMAIL_VERIFICATION_ENABLED=false

   # IA para el juicio semántico de palabras
   GROQ_API_KEY=your-groq-api-key
   ```

   Sin `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, el arranque falla al validar el esquema contra una base inexistente.
   Flyway crea las tablas automáticamente en el primer arranque contra una base vacía.

   > ⚠️ `application.properties` lee `JWT_EXPIRATION_SECONDS` (no `JWT_EXPIRATION_TIME`, pese a lo que sugiera
   > algún `.env.example` desactualizado) — usa ese nombre o el valor por defecto de 7200s se aplicará igualmente
   > en silencio.

2. Ejecuta el backend:

   ```bash
   # Windows
   mvnw.cmd spring-boot:run

   # macOS/Linux
   ./mvnw spring-boot:run
   ```

   La API arranca en `http://localhost:8080` y expone:
   - Endpoints REST bajo `/auth`, `/users`, `/password`, `/leaderboard`, `/rooms`
   - WebSocket/STOMP bajo `/ws` (SockJS)
   - Swagger UI en `http://localhost:8080/swagger-ui/index.html`

## 🧪 Comandos útiles

```bash
# Solo compilar
./mvnw compile

# Build completo
./mvnw clean install

# Suite de tests completa (no requiere PostgreSQL real ni servicios externos)
./mvnw test

# Un test concreto
./mvnw test -Dtest=GameApplicationServiceTest

# Un método de test concreto
./mvnw test -Dtest=RoundTest#testMethodName
```

En Windows, sustituye `./mvnw` por `mvnw.cmd`.

## 📚 Endpoints principales

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/auth/signup` | pública | Registro de cuenta |
| POST | `/auth/login` | pública | Login, devuelve JWT + perfil |
| POST | `/auth/verify-account` | pública | Verifica cuenta con OTP recibido por email |
| POST | `/auth/resend-verification` | pública | Reenvía el OTP de verificación |
| POST | `/password/request-reset` | pública | Solicita reset de contraseña |
| POST | `/password/reset` | pública | Aplica el reset con OTP |
| GET | `/users/me` | JWT | Perfil del usuario autenticado (incluye racha actual) |
| PATCH | `/users/me` | JWT | Actualiza perfil (username, avatar) |
| PUT | `/users/me/password` | JWT | Cambia contraseña estando autenticado |
| GET | `/leaderboard/daily` | pública | Top 10 del reto diario de hoy |
| POST | `/rooms`, `/rooms/daily`, `/rooms/*/join` | pública | Ciclo de vida de salas de juego |
| `/ws/**` | — | pública | Handshake WebSocket/STOMP para la partida en vivo |

Ver Swagger UI para el contrato completo (parámetros, DTOs, códigos de error).

## 🐳 Docker

Incluye un `Dockerfile` listo para desplegar (por ejemplo en Render u otro PaaS compatible con contenedores). El
JVM heap está limitado para funcionar en instancias de bajos recursos. Recuerda pasar `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD` y `JWT_SECRET` como variables de entorno reales en el proveedor — nunca reutilices los valores por
defecto de desarrollo.
