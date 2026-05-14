# Feature Ideas

Ideas for future improvements to the Benatti Auth Framework.

---

## Backend (`benatti-auth-starter`)

### Device fingerprinting
Track which device issued a refresh token (user-agent, IP, platform).  
Show users a list of active sessions with the ability to revoke individual ones — like Google's "Manage devices" page.  
Useful for "Sign out everywhere" functionality.

### Redis refresh token storage
Currently supports `in-memory` and `jpa`. Add a `redis` adapter via `spring-data-redis`.  
Enables horizontal scaling — multiple backend instances share the same token store without sticky sessions.

### Pluggable password policy
Expose a `PasswordPolicyProvider` interface that the app implements.  
Library calls it during login/registration to enforce min length, complexity, breach database checks (via haveibeenpwned.com API), etc.

### Audit log events
Publish Spring `ApplicationEvent` on every auth action: `LoginSucceededEvent`, `LoginFailedEvent`, `TokenRefreshedEvent`, `LogoutEvent`.  
Apps can listen and write to their own audit tables — zero coupling to the library internals.

### Configurable rate limiting
Add `auth.max-failed-attempts` and `auth.lockout-duration-minutes` to `application.yml`.  
Lock the account (or IP) after N failed logins. Currently there is no brute-force protection built in.

### RSA key pair auto-generation
If `jwt-algorithm: RSA` is set and no key files are configured, auto-generate an ephemeral RSA key pair on startup (useful for dev).  
In production, require explicit `auth.rsa-private-key-path` so the key survives restarts.

---

## Frontend (`@benatti/ng-auth-lib`)

### Token expiry countdown signal
Expose an Angular `Signal<number>` that ticks down the seconds until the access token expires.  
Apps can use it to show a "Your session expires in 2 minutes" banner and offer a one-click extend.

### Role-based directive `*ifRole`
Structural directive analog of `roleGuard` for templates:
```html
<button *ifRole="'ADMIN'">Delete user</button>
```
Hides the element from the DOM entirely if the user lacks the role — not just CSS `display: none`.

### Idle timeout detection
Detect user inactivity (no mouse/keyboard/scroll events for N minutes) and auto-logout with a warning modal countdown.  
Configurable via `provideAuth({ idleTimeoutMinutes: 30 })`. Common compliance requirement in enterprise apps.

### Offline token caching with Service Worker
Intercept requests while offline, queue them, and replay once the connection is restored with a fresh token.  
Particularly useful for PWAs running on flaky mobile networks.

### Auth state persistence across tabs
Use the `BroadcastChannel` API to sync login/logout events between browser tabs instantly — no page refresh needed.  
If the user logs out in tab A, tab B redirects to `/login` automatically.

---

## Developer Experience

### Starter CLI (`benatti-auth init`)
A small Node.js CLI that scaffolds the entire integration:
- generates `AuthConfig.java` with a stub `UserDetailsProvider`
- adds the Maven/npm dependency to the existing build file
- creates a working `application.yml` snippet

One command to go from zero to compiling.

### Spring Initializr integration
Submit the starter to [start.spring.io](https://start.spring.io) as a community dependency so developers can include it with a checkbox — same UX as Spring Security or Spring Data.

### OpenAPI / Swagger auto-documentation
Auto-register Springdoc OpenAPI descriptions for all four auth endpoints when `springdoc-openapi-starter-webmvc-ui` is on the classpath.  
Zero extra configuration — the Swagger UI shows request/response schemas out of the box.
