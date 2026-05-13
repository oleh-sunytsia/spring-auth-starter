# Project Architecture Overview & Initialization Guide

## Complete Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CLIENT TIER (Angular App)                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    ng-auth-lib (Angular Library)                    │   │
│  │  ┌────────────────────────────────────────────────────────────────┐ │   │
│  │  │ Standalone Providers: provideAuth()                           │ │   │
│  │  │ - AuthService (State Management)                              │ │   │
│  │  │ - TokenStorageService (Storage Strategy)                      │ │   │
│  │  │ - TokenDecoderService (JWT Decoding)                          │ │   │
│  │  │ - Guards: AuthGuard, RoleGuard, etc.                          │ │   │
│  │  │ - Interceptors: JwtBearer, AuthError, TokenRefresh           │ │   │
│  │  └────────────────────────────────────────────────────────────────┘ │   │
│  │                              ↓                                         │   │
│  │  ┌────────────────────────────────────────────────────────────────┐ │   │
│  │  │              Application Components                            │ │   │
│  │  │ - LoginComponent (Form)                                        │ │   │
│  │  │ - Dashboard (Protected Route)                                 │ │   │
│  │  │ - UserProfile (Role-based Access)                             │ │   │
│  │  └────────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                               │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                        HTTP Layer                                    │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ HttpInterceptors Pipeline:                                  │   │  │
│  │  │ 1. JwtBearerInterceptor → Add Authorization Header         │   │  │
│  │  │ 2. AuthErrorInterceptor → Handle 401 + Retry with Refresh │   │  │
│  │  │ 3. Custom Interceptors (Developer-provided)                │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
                         HTTP/HTTPS Requests
                        With JWT Bearer Token
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                      APPLICATION TIER (Spring Boot)                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │               spring-auth-starter (Auto-configured)                 │   │
│  │  ┌────────────────────────────────────────────────────────────────┐ │   │
│  │  │ JWT Layer                                                      │ │   │
│  │  │ ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐ │ │   │
│  │  │ │ JwtProvider │→ │ JwtTokenStore│→ │ JwtPayload (Claims)   │ │ │   │
│  │  │ │             │  │              │  │ - Subject (User ID)   │ │ │   │
│  │  │ │ ├─ HMAC     │  │ Generate     │  │ - Roles               │ │ │   │
│  │  │ │ │ HS256     │  │ Validate     │  │ - Permissions         │ │ │   │
│  │  │ │ │ (Default) │  │ Parse        │  │ - Custom Claims       │ │ │   │
│  │  │ │ ├─ RSA      │  └──────────────┘  └────────────────────────┘ │ │   │
│  │  │ │ │ (Variant) │                                                 │ │   │
│  │  │ │ └─ Custom   │                                                 │ │   │
│  │  │ │   (Dev)     │                                                 │ │   │
│  │  │ └─────────────┘                                                 │ │   │
│  │  └────────────────────────────────────────────────────────────────┘ │   │
│  │                                                                      │   │
│  │  ┌────────────────────────────────────────────────────────────────┐ │   │
│  │  │ Security Layer (Spring Security 6.x)                          │ │   │
│  │  │ ┌────────────────────────────────────────────────────────────┐ │ │   │
│  │  │ │ JwtAuthenticationFilter                                    │ │ │   │
│  │  │ │ ├─ Extract JWT from Authorization header                  │ │ │   │
│  │  │ │ ├─ Validate token signature & expiry                      │ │ │   │
│  │  │ │ ├─ Load UserDetails via UserDetailsProvider              │ │ │   │
│  │  │ │ └─ Set SecurityContext                                   │ │ │   │
│  │  │ └────────────────────────────────────────────────────────────┘ │ │   │
│  │  │ ┌────────────────────────────────────────────────────────────┐ │ │   │
│  │  │ │ Authorization Configuration                               │ │ │   │
│  │  │ │ ├─ Permit: /api/auth/login, /api/auth/refresh            │ │ │   │
│  │  │ │ ├─ Deny: Everything else requires Authentication         │ │ │   │
│  │  │ │ ├─ Method Security: @PreAuthorize("hasRole('ADMIN')")    │ │ │   │
│  │  │ │ └─ CORS Configuration                                    │ │ │   │
│  │  │ └────────────────────────────────────────────────────────────┘ │ │   │
│  │  └────────────────────────────────────────────────────────────────┘ │   │
│  │                                                                      │   │
│  │  ┌────────────────────────────────────────────────────────────────┐ │   │
│  │  │ Authentication Service Layer                                  │ │   │
│  │  │ ┌────────────────────────────────────────────────────────────┐ │ │   │
│  │  │ │ AuthService (Interface)                                    │ │ │   │
│  │  │ │ ├─ login(username, password)                              │ │ │   │
│  │  │ │ ├─ refreshToken(refreshToken)                             │ │ │   │
│  │  │ │ ├─ logout(userId, refreshToken)                           │ │ │   │
│  │  │ │ ├─ validateToken(token)                                   │ │ │   │
│  │  │ │ └─ getUserDetailsFromToken(token)                         │ │ │   │
│  │  │ └────────────────────────────────────────────────────────────┘ │ │   │
│  │  │                           ↓                                      │ │   │
│  │  │ ┌────────────────────────────────────────────────────────────┐ │ │   │
│  │  │ │ UserDetailsProvider (Interface)                            │ │ │   │
│  │  │ │ ├─ loadUserByUsername(username)  [Dev customizes]        │ │ │   │
│  │  │ │ ├─ loadUserById(userId)          [Dev customizes]        │ │ │   │
│  │  │ │ └─ Connects to: UserRepository → Database                │ │ │   │
│  │  │ └────────────────────────────────────────────────────────────┘ │ │   │
│  │  │                           ↓                                      │ │   │
│  │  │ ┌────────────────────────────────────────────────────────────┐ │ │   │
│  │  │ │ AuthUserDetails (Interface extending UserDetails)         │ │ │   │
│  │  │ │ ├─ userId: String                                         │ │ │   │
│  │  │ │ ├─ email: String                                          │ │ │   │
│  │  │ │ ├─ roles: List<String>                                    │ │ │   │
│  │  │ │ ├─ permissions: List<String>                              │ │ │   │
│  │  │ │ └─ lastLogin: Instant                                     │ │ │   │
│  │  │ └────────────────────────────────────────────────────────────┘ │ │   │
│  │  └────────────────────────────────────────────────────────────────┘ │   │
│  │                                                                      │   │
│  │  ┌────────────────────────────────────────────────────────────────┐ │   │
│  │  │ Token Storage Layer                                            │ │   │
│  │  │ ┌────────────────────────────────────────────────────────────┐ │ │   │
│  │  │ │ RefreshTokenRepository (Interface)                         │ │ │   │
│  │  │ │ ├─ save(refreshToken)                                     │ │ │   │
│  │  │ │ ├─ findByTokenAndValid(token)                             │ │ │   │
│  │  │ │ ├─ invalidate(token)                                      │ │ │   │
│  │  │ │ └─ Implementation: In-Memory / JPA / Redis [Dev chooses]  │ │ │   │
│  │  │ └────────────────────────────────────────────────────────────┘ │ │   │
│  │  │                                                                  │ │   │
│  │  │ ┌────────────────────────────────────────────────────────────┐ │ │   │
│  │  │ │ RefreshToken Storage Options:                             │ │ │   │
│  │  │ │ 1. InMemoryRefreshTokenStore (Dev/Testing)               │ │ │   │
│  │  │ │ 2. JpaRefreshTokenStore (Production Database)            │ │ │   │
│  │  │ │ 3. RedisRefreshTokenStore (High-performance, Dev)        │ │ │   │
│  │  │ └────────────────────────────────────────────────────────────┘ │ │   │
│  │  └────────────────────────────────────────────────────────────────┘ │   │
│  │                                                                      │   │
│  │  ┌────────────────────────────────────────────────────────────────┐ │   │
│  │  │ Controller Layer                                               │ │   │
│  │  │ ┌────────────────────────────────────────────────────────────┐ │ │   │
│  │  │ │ AuthController (Built-in)                                 │ │ │   │
│  │  │ │ ├─ POST /api/auth/login                                   │ │ │   │
│  │  │ │ ├─ POST /api/auth/refresh                                 │ │ │   │
│  │  │ │ ├─ POST /api/auth/logout                                  │ │ │   │
│  │  │ │ └─ GET /api/auth/validate                                 │ │ │   │
│  │  │ └────────────────────────────────────────────────────────────┘ │ │   │
│  │  │                           ↓                                      │ │   │
│  │  │ ┌────────────────────────────────────────────────────────────┐ │ │   │
│  │  │ │ Developer's Controllers (Custom Endpoints)                │ │ │   │
│  │  │ │ ├─ GET /api/users                                         │ │ │   │
│  │  │ │ ├─ POST /api/users                                        │ │ │   │
│  │  │ │ └─ [@PreAuthorize] marks require authentication           │ │ │   │
│  │  │ └────────────────────────────────────────────────────────────┘ │ │   │
│  │  └────────────────────────────────────────────────────────────────┘ │   │
│  │                                                                      │   │
│  │  ┌────────────────────────────────────────────────────────────────┐ │   │
│  │  │ Event System (Domain Events)                                   │ │   │
│  │  │ ├─ LoginSuccessEvent     → Developer listens for audit        │ │   │
│  │  │ ├─ LoginFailureEvent     → Track failed attempts              │ │   │
│  │  │ ├─ TokenRefreshEvent     → Update user last activity          │ │   │
│  │  │ ├─ LogoutEvent           → Clear sessions                     │ │   │
│  │  │ └─ CustomEvent           → Developer defines custom events    │ │   │
│  │  └────────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DATA TIER (Database)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐ │
│  │      users          │  │    user_roles       │  │      roles          │ │
│  ├─────────────────────┤  ├─────────────────────┤  ├─────────────────────┤ │
│  │ id (UUID)           │  │ user_id (FK)        │  │ id (UUID)           │ │
│  │ username (UNIQUE)   │  │ role_id (FK)        │  │ name                │ │
│  │ email               │  └─────────────────────┘  │ description         │ │
│  │ password (hashed)   │                            └─────────────────────┘ │
│  │ created_at          │  ┌─────────────────────┐   ┌──────────────────────┐
│  │ updated_at          │  │ refresh_tokens      │   │ role_permissions    │
│  │ is_active           │  ├─────────────────────┤   ├──────────────────────┤
│  └─────────────────────┘  │ id (UUID)           │   │ role_id (FK)        │
│                            │ user_id (FK)        │   │ permission_id (FK)  │
│                            │ token (encrypted)   │   └──────────────────────┘
│                            │ device_id           │    ┌──────────────────────┐
│                            │ device_name         │    │   permissions       │
│                            │ created_at          │    ├──────────────────────┤
│                            │ expires_at          │    │ id (UUID)           │
│                            │ revoked (boolean)   │    │ name                │
│                            │ revoked_at          │    │ description         │
│                            │ last_used_at        │    └──────────────────────┘
│                            └─────────────────────┘
│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Module Dependency Graph

```
Application (Developer's Code)
    ↓
┌───────────────────────────────────────┐
│  ng-auth-lib / spring-auth-starter   │
├───────────────────────────────────────┤
│                                       │
│  Services Layer                       │
│  ├─ AuthService                       │
│  ├─ TokenService                      │
│  └─ UserService                       │
│          ↓                            │
│  Security Layer                       │
│  ├─ Guards                            │
│  ├─ Interceptors (Angular)            │
│  ├─ Filters (Java)                    │
│  └─ Configuration                     │
│          ↓                            │
│  JWT Provider Layer                   │
│  ├─ Token Generation                  │
│  ├─ Token Validation                  │
│  ├─ Claims Management                 │
│  └─ Encryption                        │
│          ↓                            │
│  Storage Layer                        │
│  ├─ Token Storage (localStorage)      │
│  ├─ Refresh Token Store (Database)    │
│  └─ Cache (Redis optional)            │
│                                       │
└───────────────────────────────────────┘
         ↓
Framework Layers
├─ Spring Boot 3.x / Angular 16+
├─ Spring Security 6.x / RxJS
└─ Jakarta Persistence / HTTPClient
```

---

## Project Initialization Checklist

### Phase 1: Environment Setup ✓

#### Backend Setup
- [ ] Java 17+ installed (`java -version`)
- [ ] Maven 3.8+ installed (`mvn -version`)
- [ ] PostgreSQL running (`psql -V`)
- [ ] Git installed (`git --version`)
- [ ] IDE: IntelliJ IDEA or VS Code with Java extensions

#### Frontend Setup
- [ ] Node.js 18+ (`node -v`)
- [ ] npm 9+ (`npm -v`)
- [ ] Angular CLI 16+ (`ng version`)
- [ ] Git installed

#### Verification Commands
```bash
# Backend
java -version
mvn -version
psql -V

# Frontend
node -v
npm -v
ng version
```

---

### Phase 2: Backend Project Initialization

- [ ] Create Maven project structure:
  ```bash
  mvn archetype:generate \
    -DgroupId=com.example \
    -DartifactId=auth-demo-app \
    -DarchetypeArtifactId=maven-archetype-quickstart
  ```

- [ ] Add `spring-auth-starter` dependency to `pom.xml`
  ```xml
  <dependency>
    <groupId>com.authlib</groupId>
    <artifactId>spring-auth-starter</artifactId>
    <version>1.0.0</version>
  </dependency>
  ```

- [ ] Add Spring Boot dependencies:
  ```bash
  spring-boot-starter-web
  spring-boot-starter-data-jpa
  postgresql
  ```

- [ ] Create `application.yml`:
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://localhost:5432/auth_demo
      username: postgres
      password: postgres
  auth:
    jwt-secret: your-super-secret-jwt-key-min-32-chars-long!
  ```

- [ ] Create database:
  ```bash
  createdb -U postgres auth_demo
  ```

- [ ] Create JPA entities:
  - [ ] `User.java`
  - [ ] `Role.java`
  - [ ] `Permission.java`

- [ ] Create repositories:
  - [ ] `UserRepository.java`
  - [ ] `RoleRepository.java`
  - [ ] `PermissionRepository.java`

- [ ] Create custom `AuthConfig.java`:
  - [ ] Implement `UserDetailsProvider`
  - [ ] Configure `RefreshTokenRepository`

- [ ] Test backend:
  ```bash
  mvn spring-boot:run
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}'
  ```

---

### Phase 3: Frontend Project Initialization

- [ ] Create Angular project:
  ```bash
  ng new auth-demo-app
  cd auth-demo-app
  ```

- [ ] Install `ng-auth-lib`:
  ```bash
  npm install @authlib/ng-auth-lib
  ```

- [ ] Update `main.ts`:
  - [ ] Add `provideAuth()` configuration
  - [ ] Set `apiEndpoint`

- [ ] Create routing:
  - [ ] `app.routes.ts` with auth guards

- [ ] Create components:
  - [ ] `LoginComponent`
  - [ ] `DashboardComponent`
  - [ ] `ProfileComponent`

- [ ] Configure HTTP client:
  - [ ] Add CORS headers if needed
  - [ ] Verify interceptors are applied

- [ ] Test frontend:
  ```bash
  ng serve
  # Navigate to http://localhost:4200/login
  ```

---

### Phase 4: Integration Testing

#### Backend Integration Tests
- [ ] Test login endpoint:
  ```bash
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"john","password":"password"}'
  ```

- [ ] Test token refresh:
  ```bash
  curl -X POST http://localhost:8080/api/auth/refresh \
    -H "Content-Type: application/json" \
    -d '{"refreshToken":"..."}'
  ```

- [ ] Test protected endpoints:
  ```bash
  curl -H "Authorization: Bearer <token>" \
    http://localhost:8080/api/auth/validate
  ```

#### Frontend Integration Tests
- [ ] Login flow
- [ ] Token storage in localStorage
- [ ] Automatic token refresh on 401
- [ ] Logout clears tokens
- [ ] Guards prevent unauthorized access
- [ ] Interceptors add Authorization header

---

### Phase 5: Security Verification

#### Backend
- [ ] CORS properly configured
- [ ] JWT secret is 32+ characters
- [ ] Passwords hashed with bcrypt
- [ ] SQL injection prevention (use parameterized queries)
- [ ] Rate limiting configured
- [ ] HTTPS enforced (production)
- [ ] Refresh tokens persisted and validated
- [ ] Tokens expire appropriately

#### Frontend
- [ ] Tokens stored securely (not in URL/cookies in localStorage)
- [ ] HttpOnly cookies if used (server-side)
- [ ] CSP headers configured (production)
- [ ] No credentials exposed in console logs
- [ ] CORS whitelist configured
- [ ] Guards prevent unauthorized access
- [ ] Error messages don't expose sensitive info

---

### Phase 6: Documentation

- [ ] Update README.md:
  - [ ] Installation instructions
  - [ ] Configuration options
  - [ ] Usage examples
  - [ ] Troubleshooting section

- [ ] Create API documentation:
  - [ ] All endpoints documented
  - [ ] Request/response examples
  - [ ] Error codes explained

- [ ] Create developer guide:
  - [ ] How to customize UserDetailsProvider
  - [ ] How to implement custom guards
  - [ ] How to extend services

- [ ] Add code comments:
  - [ ] Document complex logic
  - [ ] Explain security decisions

---

### Phase 7: Deployment Preparation

#### Backend
- [ ] Run full test suite: `mvn test`
- [ ] Run integration tests
- [ ] Code quality check: `mvn checkstyle:check`
- [ ] Build JAR: `mvn clean package`
- [ ] Create Docker image (optional)
- [ ] Set production environment variables
- [ ] Configure logging
- [ ] Setup monitoring/metrics

#### Frontend
- [ ] Run unit tests: `ng test`
- [ ] Run e2e tests: `ng e2e`
- [ ] Build production bundle: `ng build --configuration production`
- [ ] Optimize bundle size
- [ ] Configure environment files
- [ ] Setup CI/CD pipeline

---

### Phase 8: Publication

#### Backend - Maven Central
- [ ] Create Sonatype account
- [ ] Configure GPG signing
- [ ] Update pom.xml with:
  - [ ] License information
  - [ ] SCM details
  - [ ] Developer information
- [ ] Create GitHub repository
- [ ] Setup GitHub Actions workflow
- [ ] Create git tag and push
- [ ] Monitor Maven Central sync

#### Frontend - npm Registry
- [ ] Create npm account
- [ ] Configure `package.json`:
  - [ ] Correct version
  - [ ] Keywords
  - [ ] Description
  - [ ] Repository link
- [ ] Create GitHub repository
- [ ] Setup GitHub Actions workflow
- [ ] Publish to npm: `npm publish`
- [ ] Verify on npmjs.com

---

## Success Criteria

### Code Quality
- [ ] Unit test coverage > 80%
- [ ] No critical security vulnerabilities
- [ ] ESLint/PMD pass without warnings
- [ ] TypeScript strict mode enabled

### Security
- [ ] OWASP top 10 vulnerabilities checked
- [ ] Security headers configured
- [ ] Rate limiting implemented
- [ ] CSRF protection enabled
- [ ] Secrets management in place

### Performance
- [ ] Backend response time < 200ms (p95)
- [ ] Token validation < 10ms
- [ ] Frontend bundle size < 500KB (gzipped)
- [ ] No memory leaks on token refresh

### Documentation
- [ ] Setup guide is clear and complete
- [ ] API endpoints fully documented
- [ ] Extension patterns explained
- [ ] Troubleshooting guide included
- [ ] Real-world examples provided

### User Experience
- [ ] Login flow smooth and intuitive
- [ ] Error messages helpful
- [ ] Token refresh transparent to user
- [ ] Logout clears session properly
- [ ] Protected routes redirect appropriately

---

## Timeline Estimate

| Phase | Duration | Notes |
|-------|----------|-------|
| Environment Setup | 1-2 hours | One-time setup |
| Backend Development | 2-3 days | Core JWT logic, Security config |
| Frontend Development | 2-3 days | Guards, Interceptors, Services |
| Integration Testing | 1-2 days | End-to-end flows |
| Security Review | 1 day | Vulnerability check, compliance |
| Documentation | 1-2 days | API docs, guides |
| CI/CD Setup | 1 day | GitHub Actions workflows |
| Publication | 1 day | Maven Central, npm registry |
| **Total** | **10-15 days** | For complete implementation |

---

## Next Steps After Completion

1. **Monitor Production**
   - [ ] Setup monitoring dashboards
   - [ ] Configure alerts
   - [ ] Track authentication metrics

2. **Gather Feedback**
   - [ ] Collect developer feedback
   - [ ] Monitor GitHub issues
   - [ ] Track usage patterns

3. **Plan Updates**
   - [ ] Identify improvements
   - [ ] Plan v1.1 features
   - [ ] Schedule security patches

4. **Community Engagement**
   - [ ] Answer Stack Overflow questions
   - [ ] Contribute to discussions
   - [ ] Create blog posts/tutorials

5. **Continuous Improvement**
   - [ ] Regular security updates
   - [ ] Performance optimizations
   - [ ] New framework versions support

