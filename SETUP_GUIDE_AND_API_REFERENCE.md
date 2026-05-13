# Setup Guide & API Reference

## Quick Start Guide

### Backend (Java) - 5-Minute Setup

#### Step 1: Create Maven Project

```bash
mvn archetype:generate -DgroupId=com.example -DartifactId=auth-demo-app \
  -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
cd auth-demo-app
```

#### Step 2: Add spring-auth-starter Dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.authlib</groupId>
    <artifactId>spring-auth-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Also add Spring Boot and PostgreSQL -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.1.0</version>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.6.0</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <version>3.1.0</version>
</dependency>
```

#### Step 3: Create application.yml

```yaml
spring:
  application:
    name: auth-demo-app
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_demo
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

auth:
  enabled: true
  jwt-secret: your-super-secret-jwt-key-min-32-chars-long!
  jwt-algorithm: HS256
  access-token-expiration-minutes: 60
  refresh-token-expiration-days: 30
  refresh-token-storage: jpa
  allowed-origins: http://localhost:4200
```

#### Step 4: Create User Entity

```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true)
    private String username;
    
    private String password;
    private String email;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}

@Entity
@Table(name = "roles")
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String name;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}

@Entity
@Table(name = "permissions")
@Data
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String name;
    private String description;
}
```

#### Step 5: Create Custom UserDetailsProvider

```java
@Configuration
public class AuthConfig {
    
    @Bean
    public UserDetailsProvider userDetailsProvider(UserRepository userRepository) {
        return new UserDetailsProvider() {
            @Override
            public AuthUserDetails loadUserByUsername(String username) {
                User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException(username));
                return mapToAuthUserDetails(user);
            }
            
            @Override
            public AuthUserDetails loadUserById(String userId) {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));
                return mapToAuthUserDetails(user);
            }
            
            private AuthUserDetails mapToAuthUserDetails(User user) {
                return new DefaultAuthUserDetails(
                    user.getId(),
                    user.getUsername(),
                    user.getPassword(),
                    user.getEmail(),
                    user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                        .collect(Collectors.toList()),
                    user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getName)
                        .collect(Collectors.toList())
                );
            }
        };
    }
}
```

#### Step 6: Create Main Application Class

```java
@SpringBootApplication
public class AuthDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthDemoApplication.class, args);
    }
}
```

#### Step 7: Test the Application

```bash
mvn spring-boot:run
```

Visit: `http://localhost:8080/api/auth/login`

---

### Frontend (Angular) - 5-Minute Setup

#### Step 1: Create Angular App

```bash
ng new auth-demo-app
cd auth-demo-app
```

#### Step 2: Install ng-auth-lib

```bash
npm install @authlib/ng-auth-lib
```

#### Step 3: Update main.ts

```typescript
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideAuth } from '@authlib/ng-auth-lib';

import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideAnimations(),
    provideAuth({
      apiEndpoint: 'http://localhost:8080',
      loginUrl: '/api/auth/login',
      refreshUrl: '/api/auth/refresh',
      logoutUrl: '/api/auth/logout',
      validateUrl: '/api/auth/validate'
    })
  ]
}).catch(err => console.error(err));
```

#### Step 4: Create Routes

```typescript
// app.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard, RoleGuard } from '@authlib/ng-auth-lib';
import { LoginComponent } from './pages/login.component';
import { DashboardComponent } from './pages/dashboard.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]
  },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' }
];
```

#### Step 5: Create Login Component

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@authlib/ng-auth-lib';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="login-container">
      <form [formGroup]="loginForm" (ngSubmit)="onLogin()">
        <h2>Login</h2>
        
        <input 
          type="text" 
          placeholder="Username"
          formControlName="username"
        />
        
        <input 
          type="password" 
          placeholder="Password"
          formControlName="password"
        />
        
        <button type="submit" [disabled]="loginForm.invalid">
          Login
        </button>
        
        <p *ngIf="errorMessage" class="error">{{ errorMessage }}</p>
      </form>
    </div>
  `,
  styles: [`
    .login-container {
      max-width: 400px;
      margin: 50px auto;
      padding: 20px;
      border: 1px solid #ccc;
      border-radius: 5px;
    }
    form {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    input {
      padding: 10px;
      border: 1px solid #ddd;
      border-radius: 3px;
    }
    button {
      padding: 10px;
      background-color: #007bff;
      color: white;
      border: none;
      border-radius: 3px;
      cursor: pointer;
    }
    button:disabled {
      background-color: #ccc;
      cursor: not-allowed;
    }
    .error {
      color: red;
      font-size: 12px;
    }
  `]
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string | null = null;
  
  constructor(
    private authService: AuthService,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }
  
  onLogin(): void {
    if (this.loginForm.invalid) return;
    
    this.authService.login(this.loginForm.value).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => this.errorMessage = 'Invalid credentials'
    });
  }
}
```

#### Step 6: Run the App

```bash
ng serve
```

Visit: `http://localhost:4200`

---

## Complete API Reference

### Backend API Endpoints

#### 1. POST /api/auth/login

**Request:**
```json
{
  "username": "john.doe",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john.doe",
    "email": "john@example.com",
    "roles": ["USER", "ADMIN"],
    "permissions": ["READ:USERS", "WRITE:USERS"],
    "lastLogin": "2024-05-13T10:30:00Z"
  }
}
```

**Response (401 Unauthorized):**
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Invalid username or password",
  "timestamp": "2024-05-13T10:30:00Z"
}
```

**Response (429 Too Many Requests):**
```json
{
  "error": "ACCOUNT_LOCKED",
  "message": "Too many failed attempts. Try again in 15 minutes.",
  "timestamp": "2024-05-13T10:30:00Z"
}
```

---

#### 2. POST /api/auth/refresh

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": { /* user object */ }
}
```

**Response (401 Unauthorized):**
```json
{
  "error": "INVALID_REFRESH_TOKEN",
  "message": "Refresh token expired or revoked",
  "timestamp": "2024-05-13T10:30:00Z"
}
```

---

#### 3. POST /api/auth/logout
**Requires:** Authorization: Bearer {accessToken}

**Request:**
```json
{}
```

**Response (200 OK):**
```json
{
  "message": "Logout successful",
  "timestamp": "2024-05-13T10:30:00Z"
}
```

---

#### 4. GET /api/auth/validate
**Requires:** Authorization: Bearer {accessToken}

**Response (200 OK):**
```json
{
  "valid": true,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "roles": ["USER", "ADMIN"],
  "permissions": ["READ:USERS", "WRITE:USERS"]
}
```

**Response (401 Unauthorized):**
```json
{
  "error": "UNAUTHORIZED",
  "message": "Token is invalid or expired"
}
```

---

### Frontend API Reference

#### AuthService

```typescript
export class AuthService {
  // Observables
  currentUser$: Observable<AuthUser | null>;
  isAuthenticated$: Observable<boolean>;
  
  // Methods
  login(credentials: LoginRequest): Observable<AuthResponse>;
  refreshAccessToken(): Observable<AuthResponse>;
  logout(): void;
  validateToken(): Observable<boolean>;
  
  getCurrentUser(): AuthUser | null;
  hasRole(role: string): boolean;
  hasPermission(permission: string): boolean;
  getAccessToken(): string | null;
  isAuthenticated(): boolean;
}
```

**Usage Examples:**

```typescript
// Check if user is authenticated
this.authService.isAuthenticated$.subscribe(isAuth => {
  if (isAuth) {
    console.log('User is logged in');
  }
});

// Get current user
const user = this.authService.getCurrentUser();
console.log(user.username);

// Check role
if (this.authService.hasRole('ADMIN')) {
  // Show admin panel
}

// Check permission
if (this.authService.hasPermission('DELETE:USERS')) {
  // Show delete button
}
```

---

#### TokenStorageService

```typescript
export class TokenStorageService {
  setAccessToken(token: string): void;
  getAccessToken(): string | null;
  setRefreshToken(token: string): void;
  getRefreshToken(): string | null;
  clear(): void;
  hasToken(): boolean;
}
```

---

#### TokenDecoderService

```typescript
export class TokenDecoderService {
  decode(token: string): DecodedToken;
  getExpirationDate(token: string): Date | null;
  isTokenExpired(token: string): boolean;
  getTimeUntilExpiry(token: string): number;
}
```

**Usage:**

```typescript
const token = this.authService.getAccessToken();
if (token) {
  const decodedToken = this.tokenDecoder.decode(token);
  console.log('User ID:', decodedToken.sub);
  console.log('Expires at:', decodedToken.exp);
  
  const timeUntilExpiry = this.tokenDecoder.getTimeUntilExpiry(token);
  console.log('Minutes until expiry:', timeUntilExpiry / 1000 / 60);
}
```

---

#### AuthGuard

```typescript
export class AuthGuard implements CanActivateFn {
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean;
}
```

**Usage in routing:**

```typescript
export const routes: Routes = [
  {
    path: 'admin',
    component: AdminComponent,
    canActivate: [AuthGuard]
  }
];
```

---

#### RoleGuard

```typescript
export class RoleGuard implements CanActivateFn {
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean;
}
```

**Usage in routing:**

```typescript
export const routes: Routes = [
  {
    path: 'admin',
    component: AdminComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  }
];
```

---

### Configuration Options

#### Backend: application.yml

```yaml
auth:
  # Enable/disable auth module
  enabled: true                                # default: true
  
  # JWT Configuration
  jwt-secret: your-secret-key                 # REQUIRED, min 32 chars
  jwt-algorithm: HS256                        # HS256 or RSA (default: HS256)
  access-token-expiration-minutes: 60         # default: 60
  refresh-token-expiration-days: 30           # default: 30
  token-issuer: auth-server                   # default: auth-server
  
  # Storage Backend
  refresh-token-storage: jpa                  # in-memory, jpa, redis
  
  # Security
  max-refresh-tokens-per-user: 5              # default: 5
  enable-token-rotation: true                 # default: true
  
  # Endpoints
  login-endpoint: /api/auth/login             # default: /api/auth/login
  refresh-endpoint: /api/auth/refresh         # default: /api/auth/refresh
  logout-endpoint: /api/auth/logout           # default: /api/auth/logout
  validate-endpoint: /api/auth/validate       # default: /api/auth/validate
  
  # CORS
  allowed-origins:
    - http://localhost:4200
    - http://localhost:3000
  allowed-methods: GET,POST,PUT,DELETE        # default: GET,POST,PUT,DELETE
```

#### Frontend: AuthConfig

```typescript
export interface AuthConfig {
  apiEndpoint: string;                        // REQUIRED
  loginUrl: string;                           // default: /api/auth/login
  refreshUrl: string;                         // default: /api/auth/refresh
  logoutUrl: string;                          // default: /api/auth/logout
  validateUrl: string;                        // default: /api/auth/validate
  tokenStorageKey?: string;                   // default: auth_token
  refreshTokenKey?: string;                   // default: refresh_token
  whitelistedUrls?: string[];                 // URLs to include Authorization header
  blacklistedUrls?: string[];                 // URLs to exclude Authorization header
  accessTokenExpirationMinutes?: number;      // default: 60
}
```

**Usage:**

```typescript
provideAuth({
  apiEndpoint: 'http://localhost:8080',
  loginUrl: '/api/auth/login',
  refreshUrl: '/api/auth/refresh',
  tokenStorageKey: 'my_app_token',
  whitelistedUrls: ['/api/users', '/api/posts'],
  blacklistedUrls: ['/api/public']
})
```

---

## Troubleshooting

### Backend Issues

#### Issue: "JWT secret too short"
**Solution:** Use at least 32 characters for jwt-secret in application.yml
```yaml
auth:
  jwt-secret: "your-super-secret-jwt-key-min-32-chars-long!"
```

#### Issue: "No matching UserDetailsProvider found"
**Solution:** Create a custom UserDetailsProvider bean:
```java
@Bean
public UserDetailsProvider userDetailsProvider() {
  return new UserDetailsProvider() {
    @Override
    public AuthUserDetails loadUserByUsername(String username) {
      // Implementation
    }
  };
}
```

#### Issue: "Token invalid or malformed"
**Solution:** Ensure token is being sent in Authorization header:
```
Authorization: Bearer <valid_jwt_token>
```

---

### Frontend Issues

#### Issue: "Token not being sent with requests"
**Solution:** Ensure JwtBearerInterceptor is registered:
```typescript
provideAuth({
  apiEndpoint: 'http://localhost:8080'
  // Interceptors are registered automatically
})
```

#### Issue: "401 Errors not triggering token refresh"
**Solution:** Ensure AuthErrorInterceptor is registered (included in provideAuth by default)

#### Issue: "CORS errors when calling API"
**Solution:** Configure CORS in backend:
```yaml
auth:
  allowed-origins:
    - http://localhost:4200
```

#### Issue: "localStorage not persisting tokens"
**Solution:** Check if localStorage is enabled and not in private/incognito mode. Use SessionTokenStorage if needed:
```typescript
provideAuth(config, [
  { provide: TokenStorageService, useClass: SessionTokenStorage }
])
```

---

## Performance Optimization Tips

### Backend
1. **Cache user details**: Use Spring Cache abstraction
2. **Async token refresh**: Mark refresh operations as @Async
3. **Database indexing**: Index on `user.username` and `refresh_token.user_id`
4. **Connection pooling**: Configure HikariCP pool size

### Frontend
1. **Lazy load auth module**: Use Angular routing lazy loading
2. **Token pre-refresh**: Refresh token 5 mins before expiry
3. **Memoize user checks**: Use RxJS shareReplay() for user$ observable
4. **Optimize interceptor**: Avoid unnecessary string operations

