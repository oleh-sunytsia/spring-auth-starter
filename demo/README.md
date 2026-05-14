# Demo Applications

End-to-end demonstration of the Benatti Auth Framework.

```
demo/
├── backend/    Spring Boot REST API  — uses benatti-auth-starter from Maven Central
└── frontend/   Angular 17 SPA        — uses @benatti/ng-auth-lib from npm
```

---

## Prerequisites

| Tool       | Version |
|------------|---------|
| Java (JDK) | 17+     |
| Maven      | 3.9+    |
| Node.js    | 18+     |
| npm        | 9+      |

---

## Quick Start

### 1 — Start the backend (port 8080)

```bash
cd backend
mvn spring-boot:run
```

Auth endpoints auto-registered by `benatti-auth-starter`:

| Method | URL                    | Description          |
|--------|------------------------|----------------------|
| POST   | `/api/auth/login`      | Obtain tokens        |
| POST   | `/api/auth/refresh`    | Refresh access token |
| POST   | `/api/auth/logout`     | Invalidate tokens    |
| GET    | `/api/auth/validate`   | Validate token       |

Demo accounts pre-seeded in H2:

| Username | Password | Roles       |
|----------|----------|-------------|
| alice    | password | USER        |
| bob      | password | USER, ADMIN |

### 2 — Start the frontend (port 4200)

```bash
cd frontend
npm install
npm start
```

Open [http://localhost:4200](http://localhost:4200) — the Angular dev server proxies `/api` → `http://localhost:8080`.

---

## Dependencies

### Backend — `backend/pom.xml`

```xml
<dependency>
  <groupId>io.github.benatti-dev</groupId>
  <artifactId>benatti-auth-starter</artifactId>
  <version>1.0.1</version>
</dependency>
```

### Frontend — `frontend/package.json`

```json
"@benatti/ng-auth-lib": "^1.0.1"
```

---

## Project Structure

```
backend/
├── pom.xml
└── src/main/java/com/benatti/demo/
    ├── DemoApplication.java
    ├── config/DemoAuthConfig.java          ← single-class integration
    └── controller/
        ├── UserController.java             ← /api/user/* (authenticated)
        └── AdminController.java            ← /api/admin/* (ADMIN role)

frontend/
├── package.json
├── angular.json
├── proxy.conf.json
└── src/app/
    ├── app.component.ts                    ← navbar + logout
    ├── app.routes.ts                       ← guards from @benatti/ng-auth-lib
    └── pages/
        ├── login/login.component.ts
        ├── dashboard/dashboard.component.ts
        ├── profile/profile.component.ts
        └── admin/admin.component.ts
```
