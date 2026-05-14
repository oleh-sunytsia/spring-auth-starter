# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Released]

---

## [1.0.0] — 2026-05-14

### benatti-auth-starter (Java)

#### Added
- `AuthAutoConfiguration` with `@ConditionalOnMissingBean` for all core beans
- `AuthProperties` (`@ConfigurationProperties(prefix = "auth")`) — JWT secret, expiry, CORS, base-path, public-paths, in-memory store flag
- `HmacJwtProvider` — HMAC-SHA256 JWT generation & validation via JJWT 0.12.3
- `JwtTokenStore` — access token (type=`access`) and refresh token (type=`refresh`) generation
- `DefaultAuthService` — login / refresh / logout / validate orchestration with Spring event publishing
- `AuthController` — 4 endpoints: `POST /login`, `POST /refresh`, `POST /logout`, `GET /validate`
- `SecurityConfig` — STATELESS, CSRF-disabled, CORS from properties, `JwtAuthenticationFilter`
- `JwtAuthenticationFilter` — `OncePerRequestFilter`, accepts only `type=access` tokens
- `InMemoryRefreshTokenStore` — `ConcurrentHashMap`-based (default, dev/test)
- `JpaRefreshTokenStore` + `RefreshTokenEntity` — `@ConditionalOnBean(EntityManagerFactory.class)`
- `GlobalExceptionHandler` — RFC 9457 `ProblemDetail` for all auth exceptions
- Auth events: `LoginSuccessEvent`, `LoginFailureEvent`, `TokenRefreshEvent`, `LogoutEvent`
- `AuthUserDetails` interface + `DefaultAuthUserDetails` builder implementation
- `UserDetailsProvider` interface (`loadUserByUsername`, `loadUserById`)
- Full unit test suite — 41 tests, 100% pass rate

#### Technical
- Spring Boot 3.3.4 BOM
- Lombok 1.18.46 (Java 25 compatible)
- Maven Surefire 3.2.5 with `--add-opens` + `-Dnet.bytebuddy.experimental=true`
- Spring Boot autoconfigure SPI via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

---

### @benatti/ng-auth-lib (Angular)

#### Added
- `provideAuth(config)` — `EnvironmentProviders` factory; wires all services + HTTP interceptors
- `AuthService` — `BehaviorSubject<AuthState>` state; `login()`, `logout()`, `refreshToken()`, `validateToken()`, `hasRole()`, `hasAnyRole()`, `hasPermission()`, `hasAnyPermission()`, `restoreSession()`
- `TokenStorageService` — localStorage access/refresh token persistence
- `TokenDecoderService` — client-side JWT decoding; `decodePayload()`, `isExpired()`, `secondsUntilExpiry()`, `getRoles()`, `getPermissions()`
- `authGuard` — `CanActivateFn`; redirects to `loginRedirectUrl` when unauthenticated
- `roleGuard` — `CanActivateFn`; checks `route.data.roles` against user roles
- `permissionGuard` — `CanActivateFn`; checks `route.data.permissions`
- `jwtBearerInterceptor` — adds `Authorization: Bearer <token>`; respects whitelist/blacklist
- `authErrorInterceptor` — handles 401; queues concurrent requests during single refresh cycle
- `AuthConfig` interface + `ResolvedAuthConfig` with sensible defaults
- `AUTH_CONFIG` injection token
- Full unit test suite — 55 tests, 100% pass rate (Jest 29 + jest-preset-angular 14)

#### Technical
- Angular 17 standalone composition API (`inject()`, functional guards/interceptors)
- `ng-packagr` build for npm distribution
- RxJS 7.8, TypeScript ~5.2, zone.js ~0.14

---

### Demo Applications

#### Added
- `demo-backend-app` — Spring Boot app using `benatti-auth-starter`; H2 in-memory DB; hardcoded users (alice/bob); UserController + AdminController
- `demo-frontend-app` — Angular 17 standalone SPA using `ng-auth-lib`; Login / Dashboard / Profile / Admin pages; dev-server proxy to backend

---

### CI/CD

#### Added
- `.github/workflows/java-ci.yml` — runs tests on push/PR to main
- `.github/workflows/java-publish.yml` — publishes to Maven Central on `v*` tag
- `.github/workflows/angular-ci.yml` — runs tests + build on push/PR to main
- `.github/workflows/angular-publish.yml` — publishes to npm on `ng-v*` tag
