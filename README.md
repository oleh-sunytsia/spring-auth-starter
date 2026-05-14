# Benatti Auth Framework

[![Maven Central](https://img.shields.io/maven-central/v/io.github.benatti-dev/benatti-auth-starter?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.benatti-dev/benatti-auth-starter)
[![npm](https://img.shields.io/npm/v/%40benatti%2Fng-auth-lib?label=npm)](https://www.npmjs.com/package/@benatti/ng-auth-lib)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-17-red)](https://angular.dev)

Full-stack JWT authentication framework for Spring Boot + Angular applications.  
Drop in two libraries, implement one interface — get a complete auth system.

---

## What's inside

This monorepo ships two libraries that work together out of the box:

| Library | Language | Registry |
|---------|----------|----------|
| [`benatti-auth-starter`](benatti-auth-starter/) | Java 17 / Spring Boot 3 | Maven Central |
| [`@benatti/ng-auth-lib`](ng-auth-lib/) | TypeScript / Angular 17 | npm |

The backend library exposes four auth endpoints (`/login`, `/refresh`, `/logout`, `/validate`) and handles everything — JWT signing, token rotation, refresh token storage, Spring Security wiring.  
The frontend library consumes those endpoints through a service, two HTTP interceptors, and two route guards — with zero extra configuration in your app.

---

## Installation

### Backend

```xml
<dependency>
  <groupId>io.github.benatti-dev</groupId>
  <artifactId>benatti-auth-starter</artifactId>
  <version>1.0.1</version>
</dependency>
```

### Frontend

```bash
npm install @benatti/ng-auth-lib
```

---

## 5-minute setup

### Backend — implement one interface

```java
@Configuration
public class AuthConfig {

    @Bean
    public UserDetailsProvider userDetailsProvider(UserRepository repo) {
        return new UserDetailsProvider() {
            @Override
            public AuthUserDetails loadUserByUsername(String username) {
                User u = repo.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException(username));
                return new DefaultAuthUserDetails(
                    u.getId(), u.getUsername(), u.getPassword(), u.getEmail(),
                    mapRoles(u), mapPermissions(u)
                );
            }

            @Override
            public AuthUserDetails loadUserById(String id) {
                User u = repo.findById(id)
                    .orElseThrow(() -> new UserNotFoundException(id));
                return new DefaultAuthUserDetails(
                    u.getId(), u.getUsername(), u.getPassword(), u.getEmail(),
                    mapRoles(u), mapPermissions(u)
                );
            }
        };
    }
}
```

```yaml
# application.yml
auth:
  jwt-secret: "your-secret-key-at-least-32-characters-long"
  access-token-expiration-minutes: 60
  refresh-token-expiration-days: 30
  refresh-token-storage: jpa          # in-memory | jpa | redis
  allowed-origins: http://localhost:4200
```

### Frontend — call `provideAuth` once

```typescript
// main.ts
bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideAuth({
      apiEndpoint:  'http://localhost:8080',
      loginUrl:     '/api/auth/login',
      refreshUrl:   '/api/auth/refresh',
      logoutUrl:    '/api/auth/logout',
      validateUrl:  '/api/auth/validate',
    }),
  ],
});
```

```typescript
// app.routes.ts
export const routes: Routes = [
  { path: 'login',     component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'admin',     component: AdminComponent,     canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
];
```

That's it. The interceptors attach `Authorization: Bearer …` to every request and silently refresh the access token before it expires.

---

## How it works

```
Browser                    Angular App                   Spring Boot
  │                            │                              │
  │──── POST /api/auth/login ──►│                              │
  │                            │──── POST /api/auth/login ───►│
  │                            │◄─── { accessToken,           │
  │                            │       refreshToken }         │
  │                            │ (stored in localStorage)     │
  │                            │                              │
  │──── GET /api/protected ───►│                              │
  │                            │  JwtBearerInterceptor adds   │
  │                            │  Authorization header        │
  │                            │──── GET /api/protected ─────►│
  │                            │◄─── 200 OK ─────────────────│
  │                            │                              │
  │                            │  token about to expire?      │
  │                            │──── POST /api/auth/refresh ─►│
  │                            │◄─── { new accessToken } ────│
```

The `AuthErrorInterceptor` catches any `401` response, refreshes the token in the background, then retries the original request — transparent to the user.

---

## Project structure

```
.
├── benatti-auth-starter/      Java library (Spring Boot starter)
│   └── src/main/java/com/benatti/auth/
│       ├── auth/              AuthService
│       ├── jwt/               JwtProvider (HMAC / RSA)
│       ├── user/              UserDetailsProvider interface
│       ├── storage/           RefreshToken + repository
│       ├── dto/               Request / response DTOs
│       └── exception/         Typed exception hierarchy
│
├── ng-auth-lib/               Angular library (ng-packagr)
│   └── src/lib/
│       ├── services/          AuthService, TokenStorageService, TokenDecoderService
│       ├── interceptors/      JwtBearerInterceptor, AuthErrorInterceptor
│       ├── guards/            authGuard, roleGuard
│       └── providers/         provideAuth()
│
└── demo/                      End-to-end demo application
    ├── README.md
    ├── backend/               Spring Boot app (uses benatti-auth-starter from Maven Central)
    └── frontend/              Angular app    (uses @benatti/ng-auth-lib from npm)
```

---

## API endpoints

All endpoints are auto-registered by the starter — no `@RestController` needed in your code.

| Method | Path | Auth required | Description |
|--------|------|:---:|-------------|
| `POST` | `/api/auth/login` | — | Authenticate and receive tokens |
| `POST` | `/api/auth/refresh` | — | Rotate access + refresh tokens |
| `POST` | `/api/auth/logout` | Bearer | Invalidate current session |
| `GET` | `/api/auth/validate` | Bearer | Validate token and return claims |

---

## Running the demo

```bash
# Terminal 1 — backend (port 8080)
cd demo/backend
mvn spring-boot:run

# Terminal 2 — frontend (port 4200)
cd demo/frontend
npm install && npm start
```

Open [http://localhost:4200](http://localhost:4200).  
Demo accounts: `alice / password` (USER) and `bob / password` (USER + ADMIN).

See [demo/README.md](demo/README.md) for full details.

---

## Documentation

| Doc | Description |
|-----|-------------|
| [SETUP_GUIDE_AND_API_REFERENCE.md](SETUP_GUIDE_AND_API_REFERENCE.md) | Full setup guide and API reference |
| [RELEASE.md](RELEASE.md) | Release process for Maven Central & npm |
| [CHANGELOG.md](CHANGELOG.md) | Version history |

---

## License

MIT — see [LICENSE](LICENSE).

