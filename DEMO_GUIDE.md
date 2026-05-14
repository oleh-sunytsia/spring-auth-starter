# Phase 4 — Demo Project Integration

This directory contains two demo applications that show end-to-end usage of the
Benatti Auth Framework.

```
demo-backend-app/   — Spring Boot REST API (uses benatti-auth-starter)
demo-frontend-app/  — Angular 17 SPA       (uses ng-auth-lib directly)
```

---

## Quick Start

### Prerequisites

| Tool          | Required version |
|---------------|-----------------|
| Java (JDK)    | 17 +            |
| Maven         | 3.9 +           |
| Node.js       | 18 +            |
| npm           | 9 +             |
| Angular CLI   | 17 +            |

---

### Step 1 — Install the Java starter library locally

```bash
cd benatti-auth-starter
mvn install -DskipTests
cd ..
```

> This places `com.benatti:benatti-auth-starter:1.0.0` in your local
> Maven repository so that `demo-backend-app` can resolve it.

---

### Step 2 — Start the backend (port 8080)

```bash
cd demo-backend-app
mvn spring-boot:run
```

The following auth endpoints are auto-registered:

| Method | URL                   | Description          |
|--------|-----------------------|----------------------|
| POST   | /api/auth/login       | Obtain access token  |
| POST   | /api/auth/refresh     | Refresh access token |
| POST   | /api/auth/logout      | Revoke refresh token |
| GET    | /api/auth/validate    | Validate token       |

Demo-specific protected endpoints:

| Method | URL                   | Required role |
|--------|-----------------------|---------------|
| GET    | /api/users/me         | any auth      |
| GET    | /api/users/token-info | any auth      |
| GET    | /api/admin/stats      | ADMIN         |
| GET    | /api/admin/users      | ADMIN         |

#### Demo accounts

| Username | Password | Roles           |
|----------|----------|-----------------|
| alice    | password | USER            |
| bob      | password | USER, ADMIN     |

#### Quick curl test

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password"}' | jq -r '.accessToken')

# 2. Call a protected endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/me
```

---

### Step 3 — Start the frontend (port 4200)

The frontend demo imports `ng-auth-lib` directly via relative TypeScript paths
(no build/publish step needed for local development).

```bash
cd demo-frontend-app
npm install
ng serve          # or: npx ng serve
```

Open **http://localhost:4200** — the Angular dev-server proxies `/api/**` to
`http://localhost:8080` (see `proxy.conf.json`).

#### Pages

| URL           | Guard           | Description                    |
|---------------|-----------------|--------------------------------|
| /login        | public          | Sign-in form                   |
| /dashboard    | authGuard       | User info + auth state JSON    |
| /profile      | authGuard       | Client-side JWT decode + token info |
| /admin        | authGuard + roleGuard(ADMIN) | Admin stats & user list |

---

## Architecture overview

```
Angular SPA (port 4200)
│   provideAuth({ ... })      ← ng-auth-lib EnvironmentProviders
│   jwtBearerInterceptor      ← adds Authorization: Bearer <token>
│   authErrorInterceptor      ← auto-refreshes on 401
│   authGuard / roleGuard     ← route protection
└── /api/**  → proxy →  Spring Boot (port 8080)
                            └── benatti-auth-starter (auto-configured)
                                ├── POST /api/auth/login
                                ├── POST /api/auth/refresh
                                ├── POST /api/auth/logout
                                └── GET  /api/auth/validate
```

---

## Customisation points

### Replace the in-memory user store (backend)

`DemoAuthConfig.java` provides a `UserDetailsProvider` bean backed by a
`ConcurrentHashMap`. To switch to a real database:

1. Add `spring-boot-starter-data-jpa` and your JDBC driver to `pom.xml`.
2. Create a `@Entity` user model.
3. Implement `UserDetailsProvider` by delegating to a `JpaRepository`.
4. Remove (or keep) the `InMemoryRefreshTokenStore` bean — if you add
   `benatti-auth-starter`'s JPA module, it will switch to JPA-backed tokens
   automatically.

### Change JWT settings (backend)

Edit `src/main/resources/application.yml`:

```yaml
auth:
  jwt-secret: <your-production-secret-at-least-32-chars>
  access-token-expiration-minutes: 15   # shorter for production
  refresh-token-expiration-days: 30
```

### Point the frontend at a different backend (frontend)

Edit `proxy.conf.json`:

```json
{
  "/api": {
    "target": "https://your-production-api.example.com",
    "secure": true,
    "changeOrigin": true
  }
}
```

Or, for a built production bundle, set the `apiEndpoint` in `main.ts`:

```ts
provideAuth({
  apiEndpoint: 'https://your-production-api.example.com',
  ...
})
```
