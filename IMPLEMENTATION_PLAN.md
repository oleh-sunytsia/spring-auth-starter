# Auth System Architecture Implementation Plan
## Java + Angular Authentication & Authorization Framework

**Project Goal**: Create two independent, production-ready, reusable security libraries:
- **`spring-auth-starter`** (Java Backend) - Spring Boot 3.x starter for auth/authz
- **`ng-auth-lib`** (Angular Frontend) - Angular library for client-side security

---

## Phase 1: Architecture Design & Contracts

### 1.1 High-Level Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Client Application                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │           Angular App (using ng-auth-lib)               │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │ Auth Module                                        │  │   │
│  │  │ - AuthService                                      │  │   │
│  │  │ - TokenStorageService                             │  │   │
│  │  │ - AuthGuard (canActivate)                         │  │   │
│  │  │ - HttpInterceptor (JWT Bearer)                    │  │   │
│  │  │ - 401 Error Handler                               │  │   │
│  │  └────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                           │                                       │
│                    HTTP/HTTPS Requests                           │
│                    (with JWT Bearer Token)                       │
│                           ↓                                       │
├─────────────────────────────────────────────────────────────────┤
│                      NETWORK BOUNDARY                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│                    Backend Server (Spring Boot)                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │      spring-auth-starter (Auto-configured)             │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │ AuthService (Token Generation, Validation)        │  │   │
│  │  │ JwtProvider                                        │  │   │
│  │  │ JwtTokenStore                                      │  │   │
│  │  │ RefreshTokenService                                │  │   │
│  │  │ SecurityConfig (Spring Security)                  │  │   │
│  │  │ Endpoints: /api/auth/login, /api/auth/refresh    │  │   │
│  │  │           /api/auth/logout, /api/auth/validate   │  │   │
│  │  └────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │   Developer's Application (Custom Implementation)       │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │ Custom UserDetailsService                         │  │   │
│  │  │ Custom Repository (UserEntity, RoleEntity)       │  │   │
│  │  │ Custom Controllers/Endpoints                      │  │   │
│  │  │ Custom SecurityEventHandler                       │  │   │
│  │  └────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                           ↓                                       │
│                    ┌──────────────┐                              │
│                    │   Database   │                              │
│                    └──────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 JWT Token Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                      LOGIN FLOW (Auth Flow)                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ CLIENT                        SERVER                                │
│   │                             │                                   │
│   │─ POST /api/auth/login ────>│                                   │
│   │  {username, password}      │                                   │
│   │                             │                                   │
│   │                        [Validate credentials]                  │
│   │                        [Load User via UserDetailsService]      │
│   │                        [Generate Access Token]                 │
│   │                        [Generate Refresh Token]                │
│   │                        [Store Refresh Token]                   │
│   │                             │                                   │
│   │<─ HTTP 200 ────────────────│                                   │
│   │  {                         │                                   │
│   │   "accessToken": "...",    │                                   │
│   │   "refreshToken": "...",   │                                   │
│   │   "expiresIn": 3600,       │                                   │
│   │   "user": { ... }          │                                   │
│   │  }                         │                                   │
│   │                             │                                   │
│   │ [Store tokens locally]     │                                   │
│   │                             │                                   │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│               AUTHENTICATED REQUEST FLOW                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ CLIENT (HttpInterceptor)      SERVER (Spring Security)             │
│   │                             │                                   │
│   │ [Get token from storage]   │                                   │
│   │ [Add Authorization header] │                                   │
│   │                             │                                   │
│   │─ GET /api/users ──────────>│                                   │
│   │  Authorization: Bearer ... │                                   │
│   │                             │                                   │
│   │                        [Validate JWT Token]                    │
│   │                        [Extract Claims]                        │
│   │                        [Check Permissions]                     │
│   │                             │                                   │
│   │<─ HTTP 200 ────────────────│                                   │
│   │  { data: [...] }           │                                   │
│   │                             │                                   │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│             TOKEN REFRESH FLOW                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ CLIENT (HttpInterceptor)      SERVER                                │
│   │                             │                                   │
│   │─ GET /api/users ──────────>│                                   │
│   │  Authorization: Bearer ... │                                   │
│   │  (expired token)            │                                   │
│   │                             │                                   │
│   │                        [Token expired]                         │
│   │<─ HTTP 401 ────────────────│                                   │
│   │  Authorization Required     │                                   │
│   │                             │                                   │
│   │ [Catch 401 Error]          │                                   │
│   │ [Call refresh endpoint]     │                                   │
│   │                             │                                   │
│   │─ POST /api/auth/refresh ──>│                                   │
│   │  {refreshToken: "..."}      │                                   │
│   │                             │                                   │
│   │                        [Validate refresh token]                │
│   │                        [Generate new access token]             │
│   │                             │                                   │
│   │<─ HTTP 200 ────────────────│                                   │
│   │  {accessToken: "...", expiresIn: 3600}                        │
│   │                             │                                   │
│   │ [Update stored token]       │                                   │
│   │ [Retry original request]    │                                   │
│   │                             │                                   │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 JWT Token Structure & Claims

```
Access Token (Short-lived: ~1 hour):
Header:  {alg: "HS256", typ: "JWT"}
Payload: {
  sub: "user-id",
  username: "john.doe",
  email: "john@example.com",
  roles: ["USER", "ADMIN"],
  permissions: ["READ:USERS", "WRITE:USERS"],
  iat: 1700000000,
  exp: 1700003600,
  iss: "auth-server"
}

Refresh Token (Long-lived: ~30 days):
Header:  {alg: "HS256", typ: "JWT"}
Payload: {
  sub: "user-id",
  iat: 1700000000,
  exp: 1702592000,
  iss: "auth-server",
  type: "refresh"
}
```

### 1.4 Data Models & Contracts

#### Backend DTO Contracts

```java
// Request DTOs
LoginRequest {
  String username
  String password
}

RefreshTokenRequest {
  String refreshToken
}

// Response DTOs
AuthResponse {
  String accessToken
  String refreshToken
  long expiresIn
  UserResponse user
}

UserResponse {
  String id
  String username
  String email
  List<String> roles
  List<String> permissions
  Instant lastLogin
}

// Token Claims
JwtClaims {
  String subject (user ID)
  String username
  String email
  List<String> roles
  List<String> permissions
  Instant issuedAt
  Instant expiresAt
  String issuer
}
```

#### Frontend Type Contracts

```typescript
// Angular Types
interface AuthToken {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

interface AuthUser {
  id: string;
  username: string;
  email: string;
  roles: string[];
  permissions: string[];
  lastLogin?: Date;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: AuthUser;
}

interface AuthConfig {
  apiEndpoint: string;
  loginUrl: string;
  refreshUrl: string;
  logoutUrl: string;
  validateUrl: string;
  tokenStorageKey: string;
  whitelistedUrls: string[];
  blacklistedUrls: string[];
}
```

---

## Phase 2: Java Backend Development (spring-auth-starter)

### 2.1 Project Structure

```
spring-auth-starter/
├── pom.xml                              # Maven configuration
├── src/main/resources/
│   ├── META-INF/
│   │   └── spring/
│   │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── application-defaults.yml         # Default auto-configuration properties
├── src/main/java/com/auth/
│   ├── autoconfigure/
│   │   ├── AuthAutoConfiguration.java   # Main auto-config class
│   │   ├── JwtAutoConfiguration.java
│   │   ├── SecurityAutoConfiguration.java
│   │   └── AuthProperties.java          # @ConfigurationProperties
│   │
│   ├── jwt/
│   │   ├── JwtProvider.java             # Interface
│   │   ├── HmacJwtProvider.java         # Default implementation
│   │   ├── RsaJwtProvider.java          # RSA variant
│   │   ├── JwtTokenStore.java           # Token generation/validation
│   │   ├── JwtPayload.java              # Token claims model
│   │   └── JwtException.java
│   │
│   ├── auth/
│   │   ├── AuthService.java             # Interface
│   │   ├── DefaultAuthService.java      # Default implementation
│   │   ├── RefreshTokenService.java     # Interface
│   │   ├── DefaultRefreshTokenService.java
│   │   ├── UserDetailsProvider.java     # Interface for customization
│   │   └── DefaultUserDetailsProvider.java
│   │
│   ├── security/
│   │   ├── SecurityConfig.java          # Spring Security configuration
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtAuthenticationProvider.java
│   │   ├── JwtAuthenticationToken.java
│   │   ├── AuthenticationFailureHandler.java
│   │   └── AuthenticationSuccessHandler.java
│   │
│   ├── controller/
│   │   └── AuthController.java          # Endpoints
│   │
│   ├── event/
│   │   ├── AuthenticationEvent.java     # Base event
│   │   ├── LoginSuccessEvent.java
│   │   ├── LoginFailureEvent.java
│   │   ├── TokenRefreshEvent.java
│   │   └── LogoutEvent.java
│   │
│   ├── storage/
│   │   ├── RefreshTokenRepository.java  # Interface
│   │   ├── RefreshTokenEntity.java      # Base JPA entity
│   │   ├── InMemoryRefreshTokenStore.java
│   │   └── JpaRefreshTokenStore.java    # Implementation
│   │
│   ├── user/
│   │   ├── AuthUserDetails.java         # Interface extending UserDetails
│   │   ├── DefaultAuthUserDetails.java  # Default implementation
│   │   └── UserDetailsServiceAdapter.java
│   │
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── RefreshTokenRequest.java
│   │   ├── RefreshTokenResponse.java
│   │   ├── ValidateTokenResponse.java
│   │   └── UserDTO.java
│   │
│   └── exception/
│       ├── AuthException.java           # Base
│       ├── InvalidCredentialsException.java
│       ├── TokenExpiredException.java
│       ├── RefreshTokenExpiredException.java
│       ├── UnauthorizedException.java
│       └── GlobalExceptionHandler.java
│
└── src/test/java/com/auth/
    ├── jwt/
    │   └── JwtProviderTest.java
    ├── auth/
    │   └── AuthServiceTest.java
    ├── security/
    │   └── SecurityConfigTest.java
    └── controller/
        └── AuthControllerTest.java
```

### 2.2 Core Interface Definitions

#### JWT Provider Interface

```java
package com.auth.jwt;

public interface JwtProvider {
    /**
     * Generate a JWT token with claims
     */
    String generateToken(JwtPayload payload);
    
    /**
     * Validate and parse JWT token
     */
    JwtPayload validateAndParseToken(String token) throws JwtException;
    
    /**
     * Validate token signature and expiration
     */
    boolean isValidToken(String token);
    
    /**
     * Get remaining time to expiration (in seconds)
     */
    long getExpirationSeconds(String token);
}

public class JwtPayload {
    private String subject;           // User ID
    private String username;
    private String email;
    private List<String> roles;
    private List<String> permissions;
    private Instant issuedAt;
    private Instant expiresAt;
    private String issuer;
    private String type;              // "access" or "refresh"
    
    // getters/setters
}

public class JwtException extends RuntimeException {
    public enum ErrorType {
        INVALID_SIGNATURE, EXPIRED, MALFORMED, UNSUPPORTED, CLAIMS_EMPTY
    }
    
    private ErrorType errorType;
}
```

#### Authentication Service Interface

```java
package com.auth.auth;

public interface AuthService {
    /**
     * Authenticate user and return JWT tokens
     */
    LoginResponse login(String username, String password) throws AuthException;
    
    /**
     * Refresh access token using refresh token
     */
    LoginResponse refreshToken(String refreshToken) throws AuthException;
    
    /**
     * Validate access token
     */
    boolean validateToken(String token);
    
    /**
     * Logout user (invalidate refresh token)
     */
    void logout(String userId, String refreshToken);
    
    /**
     * Retrieve user details from token
     */
    AuthUserDetails getUserDetailsFromToken(String token);
}

public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserDTO user;
    
    // getters/setters
}
```

#### User Details Customization Interface

```java
package com.auth.user;

/**
 * Interface for developers to provide custom user loading logic
 */
public interface UserDetailsProvider {
    /**
     * Load user by username (for authentication)
     */
    AuthUserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
    
    /**
     * Load user by ID (for token validation)
     */
    AuthUserDetails loadUserById(String userId) throws UserNotFoundException;
}

public interface AuthUserDetails extends UserDetails {
    String getUserId();
    String getEmail();
    List<String> getPermissions();
    Instant getLastLogin();
}
```

#### Refresh Token Storage Interface

```java
package com.auth.storage;

public interface RefreshTokenRepository {
    /**
     * Save refresh token
     */
    void save(RefreshToken token);
    
    /**
     * Find valid refresh token by token string
     */
    Optional<RefreshToken> findByTokenAndValid(String token);
    
    /**
     * Find all tokens by user ID
     */
    List<RefreshToken> findByUserId(String userId);
    
    /**
     * Invalidate refresh token
     */
    void invalidate(String token);
    
    /**
     * Cleanup expired tokens
     */
    void deleteExpired();
}

public class RefreshToken {
    private String id;
    private String userId;
    private String token;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean revoked;
}
```

### 2.3 Default Implementation Classes

#### JwtTokenStore (Core JWT Logic)

```java
package com.auth.jwt;

@Component
public class JwtTokenStore {
    private final JwtProvider jwtProvider;
    private final AuthProperties properties;
    
    public JwtTokenStore(JwtProvider jwtProvider, AuthProperties properties) {
        this.jwtProvider = jwtProvider;
        this.properties = properties;
    }
    
    /**
     * Generate access token (short-lived)
     */
    public String generateAccessToken(AuthUserDetails user) {
        JwtPayload payload = new JwtPayload();
        payload.setSubject(user.getUserId());
        payload.setUsername(user.getUsername());
        payload.setEmail(user.getEmail());
        payload.setRoles(new ArrayList<>(user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority).collect(Collectors.toList())));
        payload.setPermissions(user.getPermissions());
        payload.setIssuedAt(Instant.now());
        payload.setExpiresAt(Instant.now()
            .plus(Duration.ofMinutes(properties.getAccessTokenExpirationMinutes())));
        payload.setIssuer(properties.getTokenIssuer());
        payload.setType("access");
        
        return jwtProvider.generateToken(payload);
    }
    
    /**
     * Generate refresh token (long-lived)
     */
    public String generateRefreshToken(AuthUserDetails user) {
        JwtPayload payload = new JwtPayload();
        payload.setSubject(user.getUserId());
        payload.setIssuedAt(Instant.now());
        payload.setExpiresAt(Instant.now()
            .plus(Duration.ofDays(properties.getRefreshTokenExpirationDays())));
        payload.setIssuer(properties.getTokenIssuer());
        payload.setType("refresh");
        
        return jwtProvider.generateToken(payload);
    }
    
    /**
     * Validate and extract claims
     */
    public JwtPayload validateToken(String token) throws JwtException {
        return jwtProvider.validateAndParseToken(token);
    }
}
```

#### DefaultAuthService Implementation

```java
package com.auth.auth;

@Service
@Conditional(OnMissingBeanCondition.class)
public class DefaultAuthService implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsProvider userDetailsProvider;
    private final JwtTokenStore jwtTokenStore;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthProperties properties;
    
    @Override
    public LoginResponse login(String username, String password) throws AuthException {
        try {
            // Authenticate using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            
            // Load user details
            AuthUserDetails userDetails = 
                userDetailsProvider.loadUserByUsername(username);
            
            // Generate tokens
            String accessToken = jwtTokenStore.generateAccessToken(userDetails);
            String refreshToken = jwtTokenStore.generateRefreshToken(userDetails);
            
            // Store refresh token
            RefreshToken storedToken = new RefreshToken();
            storedToken.setUserId(userDetails.getUserId());
            storedToken.setToken(refreshToken);
            storedToken.setCreatedAt(Instant.now());
            storedToken.setExpiresAt(Instant.now()
                .plus(Duration.ofDays(properties.getRefreshTokenExpirationDays())));
            refreshTokenRepository.save(storedToken);
            
            // Publish success event
            eventPublisher.publishEvent(
                new LoginSuccessEvent(userDetails.getUserId(), username)
            );
            
            // Build response
            return new LoginResponse(
                accessToken,
                refreshToken,
                properties.getAccessTokenExpirationMinutes() * 60,
                mapToUserDTO(userDetails)
            );
            
        } catch (BadCredentialsException e) {
            eventPublisher.publishEvent(
                new LoginFailureEvent(username, "Invalid credentials")
            );
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }
    
    @Override
    public LoginResponse refreshToken(String refreshToken) throws AuthException {
        try {
            // Validate refresh token
            JwtPayload payload = jwtTokenStore.validateToken(refreshToken);
            
            if (!"refresh".equals(payload.getType())) {
                throw new AuthException("Invalid token type");
            }
            
            // Verify token is in repository and not revoked
            Optional<RefreshToken> storedToken = 
                refreshTokenRepository.findByTokenAndValid(refreshToken);
            
            if (storedToken.isEmpty()) {
                throw new RefreshTokenExpiredException("Refresh token not found or revoked");
            }
            
            // Load user details
            AuthUserDetails userDetails = 
                userDetailsProvider.loadUserById(payload.getSubject());
            
            // Generate new access token
            String newAccessToken = jwtTokenStore.generateAccessToken(userDetails);
            
            // Publish event
            eventPublisher.publishEvent(
                new TokenRefreshEvent(userDetails.getUserId())
            );
            
            return new LoginResponse(
                newAccessToken,
                refreshToken,  // Same refresh token
                properties.getAccessTokenExpirationMinutes() * 60,
                mapToUserDTO(userDetails)
            );
            
        } catch (JwtException e) {
            throw new RefreshTokenExpiredException("Invalid refresh token");
        }
    }
    
    @Override
    public void logout(String userId, String refreshToken) {
        refreshTokenRepository.invalidate(refreshToken);
        eventPublisher.publishEvent(new LogoutEvent(userId));
    }
    
    private UserDTO mapToUserDTO(AuthUserDetails userDetails) {
        // Map implementation
    }
}
```

#### SecurityConfig with Auto-Configuration

```java
package com.auth.autoconfigure;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@ConditionalOnProperty(name = "auth.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {
    
    /**
     * Only create if not provided by application
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtProvider jwtProvider(AuthProperties properties) {
        if ("RSA".equalsIgnoreCase(properties.getJwtAlgorithm())) {
            return new RsaJwtProvider(properties);
        }
        return new HmacJwtProvider(properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public UserDetailsProvider userDetailsProvider() {
        return new DefaultUserDetailsProvider();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RefreshTokenRepository refreshTokenRepository() {
        // Return in-memory by default, can be overridden
        return new InMemoryRefreshTokenStore();
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenStore tokenStore,
            UserDetailsProvider userDetailsProvider) {
        return new JwtAuthenticationFilter(tokenStore, userDetailsProvider);
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        
        http
            .csrf().disable()
            .cors().and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh")
                    .permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(jwtAuthenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, 
                UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

#### AuthProperties Configuration

```java
package com.auth.autoconfigure;

@ConfigurationProperties(prefix = "auth")
@Data
public class AuthProperties {
    private boolean enabled = true;
    
    // JWT Configuration
    private String jwtSecret;
    private String jwtAlgorithm = "HS256";        // HS256 or RSA
    private int accessTokenExpirationMinutes = 60;
    private int refreshTokenExpirationDays = 30;
    private String tokenIssuer = "auth-server";
    
    // Storage
    private String refreshTokenStorage = "in-memory";  // in-memory, jpa, redis
    
    // CORS
    private List<String> allowedOrigins = List.of("http://localhost:4200");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE");
    
    // Security
    private int maxRefreshTokensPerUser = 5;
    private boolean enableTokenRotation = true;
    
    // Endpoints
    private String loginEndpoint = "/api/auth/login";
    private String refreshEndpoint = "/api/auth/refresh";
    private String logoutEndpoint = "/api/auth/logout";
    private String validateEndpoint = "/api/auth/validate";
}
```

### 2.4 AuthController Implementation

```java
package com.auth.controller;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(
            request.getUsername(),
            request.getPassword()
        );
        return ResponseEntity.ok(
            ApiResponse.success(response, "Login successful")
        );
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(
            request.getRefreshToken()
        );
        return ResponseEntity.ok(
            ApiResponse.success(response, "Token refreshed")
        );
    }
    
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout() {
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal().toString();
        authService.logout(userId, null);
        return ResponseEntity.ok(
            ApiResponse.success(null, "Logout successful")
        );
    }
    
    @GetMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ValidateTokenResponse>> validateToken() {
        Authentication auth = SecurityContextHolder.getContext()
            .getAuthentication();
        ValidateTokenResponse response = new ValidateTokenResponse(
            true,
            ((AuthUserDetails) auth.getPrincipal()).getUserId(),
            auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### 2.5 Extensibility Patterns for Java

#### Pattern 1: Override UserDetailsProvider

```java
// Developer's custom implementation
@Configuration
public class CustomAuthConfig {
    
    @Bean
    public UserDetailsProvider customUserDetailsProvider(UserRepository userRepository) {
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
        };
    }
}
```

#### Pattern 2: Custom Token Claims

```java
// Extend AuthUserDetails for custom claims
public interface CustomAuthUserDetails extends AuthUserDetails {
    Map<String, Object> getCustomClaims();
}

// Provide custom JwtPayload builder
@Component
public class CustomJwtPayloadBuilder {
    public void enrichPayload(JwtPayload payload, AuthUserDetails details) {
        if (details instanceof CustomAuthUserDetails) {
            payload.setCustomClaims(
                ((CustomAuthUserDetails) details).getCustomClaims()
            );
        }
    }
}
```

#### Pattern 3: Custom Events Handler

```java
// Listen to auth events
@Component
public class CustomAuthEventHandler {
    
    @EventListener(LoginSuccessEvent.class)
    public void onLoginSuccess(LoginSuccessEvent event) {
        // Custom logic: update last login, audit log, etc.
    }
    
    @EventListener(TokenRefreshEvent.class)
    public void onTokenRefresh(TokenRefreshEvent event) {
        // Custom logic: track token refreshes
    }
}
```

#### Pattern 4: Custom Refresh Token Store

```java
// Spring Data JPA implementation
@Configuration
public class CustomStorageConfig {
    
    @Bean
    public RefreshTokenRepository refreshTokenRepository(
            JpaRefreshTokenStoreRepository jpaRepo) {
        return new JpaRefreshTokenStore(jpaRepo);
    }
}
```

---

## Phase 3: Angular Frontend Development (ng-auth-lib)

### 3.1 Project Structure (Nx Workspace)

```
auth-angular-workspace/
├── nx.json
├── package.json
├── tsconfig.base.json
├── apps/
│   └── demo-app/                        # Demo application
│       ├── src/
│       │   ├── app/
│       │   │   ├── app.component.ts
│       │   │   ├── app.routes.ts
│       │   │   ├── pages/
│       │   │   │   ├── login.component.ts
│       │   │   │   ├── dashboard.component.ts
│       │   │   │   └── profile.component.ts
│       │   │   └── app.config.ts
│       │   └── main.ts
│       └── project.json
│
└── libs/
    └── ng-auth-lib/
        ├── project.json
        ├── tsconfig.json
        ├── ng-package.json              # Angular library packaging
        ├── README.md
        ├── src/
        │   ├── public-api.ts            # Public API barrel export
        │   ├── index.ts                 # Entry point
        │   └── lib/
        │       ├── auth.module.ts       # Main module (if using NgModule)
        │       │
        │       ├── services/
        │       │   ├── auth.service.ts
        │       │   ├── token-storage.service.ts
        │       │   ├── token-decoder.service.ts
        │       │   └── auth-http.service.ts
        │       │
        │       ├── guards/
        │       │   ├── auth.guard.ts
        │       │   ├── public.guard.ts
        │       │   └── role.guard.ts
        │       │
        │       ├── interceptors/
        │       │   ├── jwt-bearer.interceptor.ts
        │       │   ├── auth-error.interceptor.ts
        │       │   └── token-refresh.interceptor.ts
        │       │
        │       ├── models/
        │       │   ├── auth-response.model.ts
        │       │   ├── auth-user.model.ts
        │       │   ├── auth-config.model.ts
        │       │   ├── auth-token.model.ts
        │       │   └── login-request.model.ts
        │       │
        │       ├── config/
        │       │   ├── auth.config.ts
        │       │   ├── auth-config.factory.ts
        │       │   └── injection-tokens.ts
        │       │
        │       ├── utils/
        │       │   ├── jwt.utils.ts
        │       │   └── token-expiry.utils.ts
        │       │
        │       └── components/
        │           ├── auth-redirect/auth-redirect.component.ts
        │           └── login/login.component.ts
        │
        └── src/test/
            ├── auth.service.spec.ts
            ├── auth.guard.spec.ts
            ├── jwt-bearer.interceptor.spec.ts
            └── token-storage.service.spec.ts
```

### 3.2 Core Models & Types

#### Authentication Models

```typescript
// models/auth-token.model.ts
export interface AuthToken {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

// models/auth-user.model.ts
export interface AuthUser {
  id: string;
  username: string;
  email: string;
  roles: string[];
  permissions: string[];
  lastLogin?: Date;
}

// models/auth-response.model.ts
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: AuthUser;
}

// models/login-request.model.ts
export interface LoginRequest {
  username: string;
  password: string;
}

// models/auth-config.model.ts
export interface AuthConfig {
  apiEndpoint: string;
  loginUrl: string;
  refreshUrl: string;
  logoutUrl: string;
  validateUrl: string;
  tokenStorageKey?: string;
  refreshTokenKey?: string;
  whitelistedUrls?: string[];
  blacklistedUrls?: string[];
  accessTokenExpirationMinutes?: number;
}

// models/decoded-token.model.ts
export interface DecodedToken {
  sub: string;           // User ID
  username: string;
  email: string;
  roles: string[];
  permissions: string[];
  iat: number;           // Issued at (seconds)
  exp: number;           // Expires at (seconds)
  iss: string;           // Issuer
}
```

### 3.3 Injection Tokens & Configuration

```typescript
// config/injection-tokens.ts
import { InjectionToken } from '@angular/core';
import { AuthConfig } from '../models/auth-config.model';

export const AUTH_CONFIG = new InjectionToken<AuthConfig>('auth.config');
export const AUTH_INTERCEPTOR_SKIP_TOKEN = 
  new InjectionToken<string[]>('auth.interceptor.skip');

// config/auth.config.ts
export const DEFAULT_AUTH_CONFIG: AuthConfig = {
  apiEndpoint: 'http://localhost:8080',
  loginUrl: '/api/auth/login',
  refreshUrl: '/api/auth/refresh',
  logoutUrl: '/api/auth/logout',
  validateUrl: '/api/auth/validate',
  tokenStorageKey: 'auth_token',
  refreshTokenKey: 'refresh_token',
  accessTokenExpirationMinutes: 60,
  whitelistedUrls: [],
  blacklistedUrls: ['/api/auth/login', '/api/auth/refresh']
};
```

### 3.4 Core Services

#### AuthService

```typescript
// services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { 
  AUTH_CONFIG, 
  AuthResponse, 
  AuthUser, 
  LoginRequest 
} from '../models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  
  constructor(
    private http: HttpClient,
    @Inject(AUTH_CONFIG) private config: AuthConfig,
    private tokenStorage: TokenStorageService,
    private tokenDecoder: TokenDecoderService
  ) {
    this.initializeAuthState();
  }
  
  /**
   * Initialize authentication state from stored token
   */
  private initializeAuthState(): void {
    const token = this.tokenStorage.getAccessToken();
    if (token && this.isTokenValid(token)) {
      try {
        const decodedToken = this.tokenDecoder.decode(token);
        const user: AuthUser = {
          id: decodedToken.sub,
          username: decodedToken.username,
          email: decodedToken.email,
          roles: decodedToken.roles,
          permissions: decodedToken.permissions,
          lastLogin: new Date(decodedToken.iat * 1000)
        };
        this.currentUserSubject.next(user);
        this.isAuthenticatedSubject.next(true);
      } catch (error) {
        console.error('Failed to decode token:', error);
        this.logout();
      }
    }
  }
  
  /**
   * Login with username and password
   */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    const url = `${this.config.apiEndpoint}${this.config.loginUrl}`;
    
    return this.http.post<AuthResponse>(url, credentials).pipe(
      tap(response => {
        this.handleAuthResponse(response);
      }),
      catchError(error => {
        console.error('Login failed:', error);
        return throwError(() => error);
      })
    );
  }
  
  /**
   * Refresh access token
   */
  refreshAccessToken(): Observable<AuthResponse> {
    const refreshToken = this.tokenStorage.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }
    
    const url = `${this.config.apiEndpoint}${this.config.refreshUrl}`;
    
    return this.http.post<AuthResponse>(url, { 
      refreshToken: refreshToken 
    }).pipe(
      tap(response => {
        this.handleAuthResponse(response);
      }),
      catchError(error => {
        this.logout();
        return throwError(() => error);
      })
    );
  }
  
  /**
   * Logout user
   */
  logout(): void {
    const url = `${this.config.apiEndpoint}${this.config.logoutUrl}`;
    
    this.http.post(url, {}).subscribe({
      next: () => this.clearAuthState(),
      error: () => this.clearAuthState()  // Clear even if logout fails
    });
  }
  
  /**
   * Validate token with backend
   */
  validateToken(): Observable<boolean> {
    const url = `${this.config.apiEndpoint}${this.config.validateUrl}`;
    
    return this.http.get<any>(url).pipe(
      map(response => response.valid),
      catchError(() => {
        this.logout();
        return [false];
      })
    );
  }
  
  /**
   * Get current user
   */
  getCurrentUser(): AuthUser | null {
    return this.currentUserSubject.value;
  }
  
  /**
   * Check if user has specific role
   */
  hasRole(role: string): boolean {
    const user = this.currentUserSubject.value;
    return user ? user.roles.includes(role) : false;
  }
  
  /**
   * Check if user has specific permission
   */
  hasPermission(permission: string): boolean {
    const user = this.currentUserSubject.value;
    return user ? user.permissions.includes(permission) : false;
  }
  
  /**
   * Get access token
   */
  getAccessToken(): string | null {
    return this.tokenStorage.getAccessToken();
  }
  
  /**
   * Check if authenticated
   */
  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }
  
  /**
   * Handle auth response and store tokens
   */
  private handleAuthResponse(response: AuthResponse): void {
    this.tokenStorage.setAccessToken(response.accessToken);
    this.tokenStorage.setRefreshToken(response.refreshToken);
    
    const user: AuthUser = response.user;
    this.currentUserSubject.next(user);
    this.isAuthenticatedSubject.next(true);
  }
  
  /**
   * Clear auth state
   */
  private clearAuthState(): void {
    this.tokenStorage.clear();
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
  }
  
  /**
   * Check if token is still valid (not expired)
   */
  private isTokenValid(token: string): boolean {
    try {
      const decodedToken = this.tokenDecoder.decode(token);
      const currentTime = Math.floor(Date.now() / 1000);
      return decodedToken.exp > currentTime;
    } catch (error) {
      return false;
    }
  }
}
```

#### TokenStorageService

```typescript
// services/token-storage.service.ts
import { Injectable } from '@angular/core';
import { Inject } from '@angular/core';
import { AUTH_CONFIG } from '../config/injection-tokens';
import { AuthConfig } from '../models/auth-config.model';

@Injectable({
  providedIn: 'root'
})
export class TokenStorageService {
  constructor(
    @Inject(AUTH_CONFIG) private config: AuthConfig
  ) {}
  
  /**
   * Set access token in storage
   */
  setAccessToken(token: string): void {
    localStorage.setItem(
      this.config.tokenStorageKey || 'auth_token',
      token
    );
  }
  
  /**
   * Get access token from storage
   */
  getAccessToken(): string | null {
    return localStorage.getItem(
      this.config.tokenStorageKey || 'auth_token'
    );
  }
  
  /**
   * Set refresh token in storage
   */
  setRefreshToken(token: string): void {
    localStorage.setItem(
      this.config.refreshTokenKey || 'refresh_token',
      token
    );
  }
  
  /**
   * Get refresh token from storage
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(
      this.config.refreshTokenKey || 'refresh_token'
    );
  }
  
  /**
   * Clear all tokens from storage
   */
  clear(): void {
    localStorage.removeItem(this.config.tokenStorageKey || 'auth_token');
    localStorage.removeItem(this.config.refreshTokenKey || 'refresh_token');
  }
  
  /**
   * Check if token exists
   */
  hasToken(): boolean {
    return this.getAccessToken() !== null;
  }
}
```

#### TokenDecoderService

```typescript
// services/token-decoder.service.ts
import { Injectable } from '@angular/core';
import { DecodedToken } from '../models/decoded-token.model';

@Injectable({
  providedIn: 'root'
})
export class TokenDecoderService {
  /**
   * Decode JWT token without verification
   * WARNING: This only decodes the payload, doesn't verify signature
   */
  decode(token: string): DecodedToken {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        throw new Error('Invalid token format');
      }
      
      const payload = parts[1];
      const decoded = JSON.parse(this.base64UrlDecode(payload));
      
      return decoded as DecodedToken;
    } catch (error) {
      throw new Error(`Failed to decode token: ${error}`);
    }
  }
  
  /**
   * Get token expiration date
   */
  getExpirationDate(token: string): Date | null {
    try {
      const decoded = this.decode(token);
      return new Date(decoded.exp * 1000);
    } catch (error) {
      return null;
    }
  }
  
  /**
   * Check if token is expired
   */
  isTokenExpired(token: string): boolean {
    const expirationDate = this.getExpirationDate(token);
    if (!expirationDate) return true;
    
    return expirationDate <= new Date();
  }
  
  /**
   * Get remaining time in milliseconds
   */
  getTimeUntilExpiry(token: string): number {
    const expirationDate = this.getExpirationDate(token);
    if (!expirationDate) return 0;
    
    const now = new Date();
    return expirationDate.getTime() - now.getTime();
  }
  
  /**
   * Base64 URL decode
   */
  private base64UrlDecode(str: string): string {
    let output = str.replace(/-/g, '+').replace(/_/g, '/');
    switch (output.length % 4) {
      case 0:
        break;
      case 2:
        output += '==';
        break;
      case 3:
        output += '=';
        break;
      default:
        throw new Error('Invalid base64url string');
    }
    
    try {
      return decodeURIComponent(atob(output).split('').map((c) => {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));
    } catch (err) {
      return atob(output);
    }
  }
}
```

### 3.5 Guards & Interceptors

#### AuthGuard

```typescript
// guards/auth.guard.ts
import { Injectable } from '@angular/core';
import {
  CanActivate,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router
} from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}
  
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    
    if (this.authService.isAuthenticated()) {
      return true;
    }
    
    // Not authenticated, redirect to login
    this.router.navigate(['/login'], { 
      queryParams: { returnUrl: state.url } 
    });
    return false;
  }
}
```

#### RoleGuard

```typescript
// guards/role.guard.ts
import { Injectable } from '@angular/core';
import {
  CanActivate,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router
} from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}
  
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    
    const requiredRoles = route.data['roles'] as string[];
    
    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }
    
    const userHasRequiredRole = requiredRoles.some(role =>
      this.authService.hasRole(role)
    );
    
    if (userHasRequiredRole) {
      return true;
    }
    
    // Not authorized, redirect to forbidden page
    this.router.navigate(['/forbidden']);
    return false;
  }
}
```

#### JwtBearerInterceptor

```typescript
// interceptors/jwt-bearer.interceptor.ts
import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { Inject } from '@angular/core';
import { AUTH_CONFIG } from '../config/injection-tokens';
import { AuthConfig } from '../models/auth-config.model';
import { AuthService } from '../services/auth.service';

@Injectable()
export class JwtBearerInterceptor implements HttpInterceptor {
  constructor(
    private authService: AuthService,
    @Inject(AUTH_CONFIG) private config: AuthConfig
  ) {}
  
  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    
    // Check if this URL should be skipped
    if (this.shouldSkipInterceptor(request.url)) {
      return next.handle(request);
    }
    
    // Get token
    const token = this.authService.getAccessToken();
    
    // Clone request and add Authorization header if token exists
    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    
    return next.handle(request);
  }
  
  /**
   * Check if URL should be skipped (based on blacklist/whitelist)
   */
  private shouldSkipInterceptor(url: string): boolean {
    // Check blacklist first
    if (this.config.blacklistedUrls) {
      for (const blacklistedUrl of this.config.blacklistedUrls) {
        if (url.includes(blacklistedUrl)) {
          return true;
        }
      }
    }
    
    return false;
  }
}
```

#### AuthErrorInterceptor

```typescript
// interceptors/auth-error.interceptor.ts
import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Injectable()
export class AuthErrorInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}
  
  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    
    return next.handle(request).pipe(
      catchError(error => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          return this.handle401Error(request, next);
        }
        
        return throwError(() => error);
      })
    );
  }
  
  /**
   * Handle 401 Unauthorized errors
   */
  private handle401Error(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      
      return this.authService.refreshAccessToken().pipe(
        switchMap(() => {
          this.isRefreshing = false;
          // Retry original request with new token
          return next.handle(request);
        }),
        catchError((err) => {
          this.isRefreshing = false;
          // Refresh failed, logout and redirect to login
          this.authService.logout();
          this.router.navigate(['/login']);
          return throwError(() => err);
        })
      );
    }
    
    // Already refreshing, logout and redirect
    this.authService.logout();
    this.router.navigate(['/login']);
    return throwError(() => new Error('Unauthorized'));
  }
}
```

### 3.6 Standalone Configuration Function

```typescript
// config/provide-auth.ts
import { 
  HTTP_INTERCEPTORS, 
  provideHttpClient, 
  withInterceptors 
} from '@angular/common/http';
import { APP_INITIALIZER, Provider } from '@angular/core';
import { 
  AUTH_CONFIG, 
  DEFAULT_AUTH_CONFIG 
} from './config';
import { AuthConfig } from '../models/auth-config.model';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';
import { TokenDecoderService } from '../services/token-decoder.service';
import { JwtBearerInterceptor } from '../interceptors/jwt-bearer.interceptor';
import { AuthErrorInterceptor } from '../interceptors/auth-error.interceptor';

/**
 * Provide authentication services and configuration
 * 
 * Usage in main.ts:
 * bootstrapApplication(AppComponent, {
 *   providers: [
 *     provideAuth({
 *       apiEndpoint: 'http://localhost:8080',
 *       loginUrl: '/api/auth/login'
 *     })
 *   ]
 * });
 */
export function provideAuth(config?: Partial<AuthConfig>): Provider[] {
  const mergedConfig: AuthConfig = {
    ...DEFAULT_AUTH_CONFIG,
    ...config
  };
  
  return [
    { provide: AUTH_CONFIG, useValue: mergedConfig },
    provideHttpClient(
      withInterceptors([]) // Note: using old-style interceptors below for compatibility
    ),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: JwtBearerInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthErrorInterceptor,
      multi: true
    },
    AuthService,
    TokenStorageService,
    TokenDecoderService
  ];
}

/**
 * Initialize authentication on app startup
 */
export function initializeAuth(authService: AuthService): () => void {
  return () => {
    // Validate existing token on app init
    const token = authService.getAccessToken();
    if (token) {
      return authService.validateToken().toPromise();
    }
    return Promise.resolve();
  };
}
```

### 3.7 Extensibility Patterns for Angular

#### Pattern 1: Custom Token Storage

```typescript
// Custom implementation extending TokenStorageService
@Injectable()
export class SessionTokenStorage extends TokenStorageService {
  // Use sessionStorage instead of localStorage
  setAccessToken(token: string): void {
    sessionStorage.setItem(this.config.tokenStorageKey, token);
  }
  
  getAccessToken(): string | null {
    return sessionStorage.getItem(this.config.tokenStorageKey);
  }
}

// Provide in app config
provideAuth({
  apiEndpoint: 'http://localhost:8080',
  loginUrl: '/api/auth/login'
}, {
  provide: TokenStorageService,
  useClass: SessionTokenStorage
})
```

#### Pattern 2: Custom Auth Guards

```typescript
// Extend existing guard
@Injectable()
export class AdminGuard extends RoleGuard {
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    const isAdmin = this.authService.hasRole('ADMIN');
    
    if (!isAdmin) {
      this.router.navigate(['/access-denied']);
      return false;
    }
    
    return super.canActivate(route, state);
  }
}
```

#### Pattern 3: Custom Interceptor Chain

```typescript
// Create custom interceptor extending error handler
@Injectable()
export class CustomAuthErrorInterceptor extends AuthErrorInterceptor {
  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    
    return super.intercept(request, next).pipe(
      catchError(error => {
        // Custom error handling
        if (error.status === 403) {
          this.router.navigate(['/forbidden']);
        }
        return throwError(() => error);
      })
    );
  }
}
```

#### Pattern 4: Custom Auth Service Enhancement

```typescript
// Extend AuthService with custom methods
@Injectable()
export class EnhancedAuthService extends AuthService {
  constructor(
    http: HttpClient,
    @Inject(AUTH_CONFIG) config: AuthConfig,
    tokenStorage: TokenStorageService,
    tokenDecoder: TokenDecoderService,
    private auditService: AuditService
  ) {
    super(http, config, tokenStorage, tokenDecoder);
  }
  
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return super.login(credentials).pipe(
      tap(response => {
        // Custom: audit login
        this.auditService.logLogin(response.user.id);
      })
    );
  }
}
```

---

## Phase 4: Demo Project Integration

### 4.1 Backend Demo Application Structure

```
demo-backend-app/
├── pom.xml
├── src/main/resources/
│   ├── application.yml
│   └── application-dev.yml
├── src/main/java/com/demo/
│   ├── DemoBackendApplication.java
│   ├── config/
│   │   └── CustomAuthConfig.java
│   ├── model/
│   │   ├── User.java
│   │   ├── Role.java
│   │   └── Permission.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── RoleRepository.java
│   │   └── RefreshTokenRepository.java
│   ├── service/
│   │   └── CustomUserDetailsProvider.java
│   ├── controller/
│   │   └── UserController.java
│   └── exception/
│       └── DemoExceptionHandler.java
└── src/test/
    └── java/com/demo/
        └── AuthIntegrationTest.java
```

#### Custom Auth Configuration for Backend Demo

```java
// config/CustomAuthConfig.java
@Configuration
public class CustomAuthConfig {
    
    @Bean
    public UserDetailsProvider customUserDetailsProvider(
            UserRepository userRepository) {
        return new UserDetailsProvider() {
            @Override
            public AuthUserDetails loadUserByUsername(String username) 
                    throws UsernameNotFoundException {
                User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> 
                        new UsernameNotFoundException("User not found"));
                return new DefaultAuthUserDetails(user);
            }
            
            @Override
            public AuthUserDetails loadUserById(String userId) 
                    throws UserNotFoundException {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
                return new DefaultAuthUserDetails(user);
            }
        };
    }
    
    @Bean
    public RefreshTokenRepository refreshTokenRepository(
            JpaRefreshTokenStoreRepository jpaRepo) {
        return new JpaRefreshTokenStore(jpaRepo);
    }
}
```

#### application.yml Configuration

```yaml
spring:
  application:
    name: demo-backend-app
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_demo
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  cors:
    allowed-origins: http://localhost:4200
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: "*"

auth:
  enabled: true
  jwt-secret: your-super-secret-jwt-key-min-32-chars-long!
  jwt-algorithm: HS256
  access-token-expiration-minutes: 60
  refresh-token-expiration-days: 30
  token-issuer: demo-auth-server
  refresh-token-storage: jpa
  login-endpoint: /api/auth/login
  refresh-endpoint: /api/auth/refresh
  logout-endpoint: /api/auth/logout
  validate-endpoint: /api/auth/validate
```

### 4.2 Frontend Demo Application (Angular)

```typescript
// main.ts - Bootstrap with auth configuration
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideAuth } from '@ng-auth-lib';
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
});
```

#### App Routes

```typescript
// app.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard } from '@ng-auth-lib';
import { RoleGuard } from '@ng-auth-lib';
import { LoginComponent } from './pages/login.component';
import { DashboardComponent } from './pages/dashboard.component';
import { AdminComponent } from './pages/admin.component';
import { ProfileComponent } from './pages/profile.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'admin',
    component: AdminComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'profile',
    component: ProfileComponent,
    canActivate: [AuthGuard]
  },
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  }
];
```

#### Login Component

```typescript
// pages/login.component.ts
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@ng-auth-lib';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <div class="login-container">
      <form [formGroup]="loginForm" (ngSubmit)="onLogin()">
        <h2>Login</h2>
        
        <div class="form-group">
          <label>Username</label>
          <input 
            type="text" 
            formControlName="username"
            class="form-control"
          />
        </div>
        
        <div class="form-group">
          <label>Password</label>
          <input 
            type="password" 
            formControlName="password"
            class="form-control"
          />
        </div>
        
        <div *ngIf="errorMessage" class="alert alert-danger">
          {{ errorMessage }}
        </div>
        
        <button 
          type="submit" 
          class="btn btn-primary"
          [disabled]="loginForm.invalid || isLoading"
        >
          {{ isLoading ? 'Logging in...' : 'Login' }}
        </button>
      </form>
    </div>
  `
})
export class LoginComponent {
  loginForm: FormGroup;
  isLoading = false;
  errorMessage: string | null = null;
  
  constructor(
    private authService: AuthService,
    private router: Router,
    private formBuilder: FormBuilder
  ) {
    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }
  
  onLogin(): void {
    if (this.loginForm.invalid) return;
    
    this.isLoading = true;
    this.errorMessage = null;
    
    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error?.error?.message || 'Login failed';
      }
    });
  }
}
```

---

## Phase 5: CI/CD & Publication

### 5.1 Backend Publication (Maven Central)

#### pom.xml Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.authlib</groupId>
    <artifactId>spring-auth-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>Spring Auth Starter</name>
    <description>Production-ready Spring Boot starter for JWT authentication</description>
    <url>https://github.com/your-org/spring-auth-starter</url>
    
    <licenses>
        <license>
            <name>MIT License</name>
            <url>https://opensource.org/licenses/MIT</url>
        </license>
    </licenses>
    
    <developers>
        <developer>
            <id>your-id</id>
            <name>Your Name</name>
            <email>your.email@example.com</email>
            <organization>Your Organization</organization>
        </developer>
    </developers>
    
    <scm>
        <connection>scm:git:https://github.com/your-org/spring-auth-starter.git</connection>
        <developerConnection>scm:git:https://github.com/your-org/spring-auth-starter.git</developerConnection>
        <url>https://github.com/your-org/spring-auth-starter</url>
        <tag>HEAD</tag>
    </scm>
    
    <!-- Distribution Management -->
    <distributionManagement>
        <snapshotRepository>
            <id>ossrh</id>
            <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
        </snapshotRepository>
        <repository>
            <id>ossrh</id>
            <url>https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/</url>
        </repository>
    </distributionManagement>
    
    <!-- Build Plugins -->
    <build>
        <plugins>
            <!-- Source Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>3.2.1</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar-no-fork</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            
            <!-- Javadoc Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>3.3.2</version>
                <executions>
                    <execution>
                        <id>attach-javadocs</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            
            <!-- GPG Plugin (for signing artifacts) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-gpg-plugin</artifactId>
                <version>3.0.1</version>
                <executions>
                    <execution>
                        <id>sign-artifacts</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>sign</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

#### GitHub Actions CI/CD Workflow

```yaml
# .github/workflows/publish-java.yml
name: Publish Java Library to Maven Central

on:
  push:
    tags:
      - 'v[0-9]+.[0-9]+.[0-9]+'

jobs:
  publish:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Set up Maven
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      
      - name: Decrypt GPG key
        env:
          GPG_SECRET_KEYS: ${{ secrets.GPG_SECRET_KEYS }}
          GPG_OWNERTRUST: ${{ secrets.GPG_OWNERTRUST }}
        run: |
          echo "$GPG_SECRET_KEYS" | base64 -d | gpg --import
          echo "$GPG_OWNERTRUST" | base64 -d | gpg --import-ownertrust
      
      - name: Publish to Maven Central
        env:
          MAVEN_USERNAME: ${{ secrets.MAVEN_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.MAVEN_PASSWORD }}
          GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
        run: |
          mvn clean deploy -P release \
            -DskipTests \
            -Dgpg.passphrase="${GPG_PASSPHRASE}"
      
      - name: Create Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref }}
          release_name: Release ${{ github.ref }}
          draft: false
          prerelease: false
```

### 5.2 Frontend Publication (npm Registry)

#### package.json Configuration

```json
{
  "name": "@authlib/ng-auth-lib",
  "version": "1.0.0",
  "description": "Production-ready Angular library for JWT authentication",
  "main": "./dist/ng-auth-lib/index.js",
  "exports": {
    ".": {
      "import": "./dist/ng-auth-lib/index.mjs",
      "require": "./dist/ng-auth-lib/index.js",
      "types": "./dist/ng-auth-lib/index.d.ts"
    }
  },
  "files": [
    "dist/ng-auth-lib"
  ],
  "scripts": {
    "build": "ng build ng-auth-lib",
    "publish": "npm publish --access public"
  },
  "keywords": [
    "angular",
    "authentication",
    "jwt",
    "authorization",
    "auth",
    "security"
  ],
  "author": "Your Name",
  "license": "MIT",
  "repository": {
    "type": "git",
    "url": "https://github.com/your-org/ng-auth-lib.git"
  },
  "peerDependencies": {
    "@angular/common": "^16.0.0",
    "@angular/core": "^16.0.0",
    "@angular/platform-browser": "^16.0.0",
    "@angular/router": "^16.0.0"
  },
  "devDependencies": {
    "@angular/cli": "^16.0.0",
    "@angular/compiler-cli": "^16.0.0",
    "ng-packagr": "^16.0.0",
    "typescript": "~5.0.0"
  },
  "publishConfig": {
    "registry": "https://registry.npmjs.org/"
  }
}
```

#### GitHub Actions for Angular

```yaml
# .github/workflows/publish-angular.yml
name: Publish Angular Library to npm

on:
  push:
    tags:
      - 'ng-v[0-9]+.[0-9]+.[0-9]+'

jobs:
  publish:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          registry-url: 'https://registry.npmjs.org'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Build library
        run: npm run build ng-auth-lib
      
      - name: Publish to npm
        run: npm publish --workspace=libs/ng-auth-lib --access public
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
      
      - name: Create Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref }}
          release_name: Angular Library Release ${{ github.ref }}
          draft: false
          prerelease: false
```

### 5.3 Release Process Checklist

#### Before Release
- [ ] Update version in `pom.xml` and `package.json`
- [ ] Update `CHANGELOG.md` with version details
- [ ] Run full test suite (unit + integration tests)
- [ ] Update documentation and README files
- [ ] Review and merge all PRs

#### Release Steps
- [ ] Create git tag: `git tag v1.0.0` or `git tag ng-v1.0.0`
- [ ] Push tag: `git push origin v1.0.0`
- [ ] Monitor GitHub Actions workflow
- [ ] Verify on Maven Central (wait 30 mins for sync)
- [ ] Verify on npm registry
- [ ] Create GitHub Release with changelog

#### After Release
- [ ] Update version to next SNAPSHOT/pre-release
- [ ] Announce release on channels (GitHub, email, etc.)
- [ ] Monitor for issues/feedback

---

## Technical Specifications Summary

### Key Interfaces to Implement

**Backend:**
1. `JwtProvider` - JWT generation/validation
2. `AuthService` - Authentication orchestration
3. `UserDetailsProvider` - User loading abstraction
4. `RefreshTokenRepository` - Token storage abstraction
5. `AuthUserDetails` - User details model

**Frontend:**
1. `AuthConfig` - Configuration interface
2. `AuthService` - Authentication orchestration
3. `TokenStorageService` - Token storage abstraction
4. `TokenDecoderService` - JWT decoding
5. Guards: `AuthGuard`, `RoleGuard`
6. Interceptors: `JwtBearerInterceptor`, `AuthErrorInterceptor`

### Default Implementations

**Backend:**
- `HmacJwtProvider` & `RsaJwtProvider`
- `DefaultAuthService`
- `DefaultUserDetailsProvider`
- `JpaRefreshTokenStore` & `InMemoryRefreshTokenStore`

**Frontend:**
- Default implementations provided via `provideAuth()` function

### Extensibility Hooks

**Backend:**
- `@ConditionalOnMissingBean` for overriding services
- Event-driven architecture for custom handlers
- Strategy pattern for JWT providers
- Repository abstraction for custom storage

**Frontend:**
- Dependency injection for custom implementations
- Extended guard/interceptor classes
- Configuration provider function
- Observable-based state management

---

## Technology Stack

### Backend
- Spring Boot 3.x
- Spring Security 6.x
- Java 17+
- JWT (JJWT library recommended)
- Spring Data JPA (optional, for refresh token storage)
- Lombok (optional, for reducing boilerplate)

### Frontend
- Angular 16+
- RxJS
- TypeScript 5.x
- Angular Router
- Angular Common HTTP
- Angular Forms

### Build & Deployment
- Maven 3.8+ (Backend)
- npm/yarn (Frontend)
- GitHub Actions (CI/CD)
- Git (Version control)

---

## Success Metrics

1. **Reusability**: Can integrate both libraries into new projects with minimal configuration
2. **Extensibility**: Developers can override core components without forking
3. **Security**: Implements OAuth2/OIDC best practices
4. **Performance**: Token refresh doesn't block user interaction
5. **Documentation**: Clear examples for common use cases
6. **Maintainability**: Clean separation of concerns, high test coverage (>80%)

