# demo-frontend-app

Angular 17 standalone demo showing how to integrate `ng-auth-lib`.

## Prerequisites

- Node.js 18+
- npm 9+
- Angular CLI 17+ (`npm i -g @angular/cli`)
- Backend running on http://localhost:8080

## Setup

```bash
npm install
npm start          # starts on http://localhost:4200
```

The dev-server proxies `/api/**` → `http://localhost:8080` (see `proxy.conf.json`).

## Pages

| URL        | Guard                  | Description                            |
|------------|------------------------|----------------------------------------|
| /login     | public                 | Sign-in form                           |
| /dashboard | authGuard              | Profile info + auth state              |
| /profile   | authGuard              | Client-side JWT decode + server claims |
| /admin     | authGuard + roleGuard  | Admin stats & user list (ADMIN only)   |

## How ng-auth-lib is wired

```ts
// src/main.ts
bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideAuth({
      apiEndpoint: '',            // empty = same origin (proxied)
      loginUrl:    '/api/auth/login',
      refreshUrl:  '/api/auth/refresh',
      logoutUrl:   '/api/auth/logout',
      blacklistedUrls: ['/api/auth/login', '/api/auth/refresh'],
    }),
  ],
});
```

`provideAuth()` registers:
- `AuthService` — login / logout / refresh / state management
- `TokenStorageService` — localStorage access/refresh token persistence
- `TokenDecoderService` — client-side JWT decoding (no server round-trip)
- `jwtBearerInterceptor` — automatically adds `Authorization: Bearer …`
- `authErrorInterceptor` — transparently refreshes on 401
