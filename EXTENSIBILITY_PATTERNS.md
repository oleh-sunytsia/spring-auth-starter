# Extensibility Patterns & Advanced Architecture

## 1. Backend Extensibility Deep Dive

### 1.1 Bean Override Strategy with @ConditionalOnMissingBean

The core pattern for extensibility is using Spring's `@ConditionalOnMissingBean` to allow developers to provide their own implementations:

```java
@Configuration
public class AuthAutoConfiguration {
    
    // PATTERN 1: Default implementation, but can be overridden
    @Bean
    @ConditionalOnMissingBean(UserDetailsProvider.class)
    public UserDetailsProvider userDetailsProvider() {
        return new DefaultUserDetailsProvider();
    }
    
    // PATTERN 2: Conditional based on property
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        name = "auth.jwt-algorithm",
        havingValue = "RSA"
    )
    public JwtProvider rsaJwtProvider(AuthProperties props) {
        return new RsaJwtProvider(props);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        name = "auth.jwt-algorithm",
        havingValue = "HS256",
        matchIfMissing = true
    )
    public JwtProvider hmacJwtProvider(AuthProperties props) {
        return new HmacJwtProvider(props);
    }
}
```

**Developer Usage:**

```java
// In developer's project, add custom implementation
@Configuration
public class CustomAuthConfiguration {
    
    @Bean
    public UserDetailsProvider customUserDetailsProvider(UserRepository repo) {
        return username -> {
            User user = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
            
            return new AuthUserDetails() {
                @Override
                public String getUserId() { return user.getId(); }
                
                @Override
                public String getUsername() { return user.getUsername(); }
                
                @Override
                public String getEmail() { return user.getEmail(); }
                
                @Override
                public List<String> getPermissions() {
                    return user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getName)
                        .collect(Collectors.toList());
                }
                // ... other methods
            };
        };
    }
}
```

### 1.2 Event-Driven Architecture for Cross-Cutting Concerns

The library publishes domain events that developers can listen to:

```java
// Core events
public abstract class AuthenticationEvent extends ApplicationEvent {
    private final String userId;
    private final Instant timestamp;
    
    public AuthenticationEvent(Object source, String userId) {
        super(source);
        this.userId = userId;
        this.timestamp = Instant.now();
    }
}

public class LoginSuccessEvent extends AuthenticationEvent {
    private final String username;
    
    public LoginSuccessEvent(Object source, String userId, String username) {
        super(source, userId);
        this.username = username;
    }
}

public class TokenRefreshEvent extends AuthenticationEvent {
    private final String oldTokenExpiry;
    
    public TokenRefreshEvent(Object source, String userId, String oldTokenExpiry) {
        super(source, userId);
        this.oldTokenExpiry = oldTokenExpiry;
    }
}

// Developer can listen to these events
@Component
public class CustomAuditHandler {
    
    @EventListener(LoginSuccessEvent.class)
    @Async
    public void onLoginSuccess(LoginSuccessEvent event) {
        // Custom audit logging
        auditService.logLogin(
            event.getUserId(),
            event.getUsername(),
            event.getTimestamp()
        );
    }
    
    @EventListener(TokenRefreshEvent.class)
    public void onTokenRefresh(TokenRefreshEvent event) {
        // Update user's last activity
        userService.updateLastActivity(event.getUserId());
    }
    
    @EventListener(LoginFailureEvent.class)
    public void onLoginFailure(LoginFailureEvent event) {
        // Track failed attempts
        securityService.recordFailedAttempt(
            event.getUsername(),
            event.getReason()
        );
    }
}
```

### 1.3 Strategy Pattern for JWT Algorithms

Allow developers to implement custom JWT algorithms:

```java
public interface JwtProvider {
    String generateToken(JwtPayload payload);
    JwtPayload validateAndParseToken(String token) throws JwtException;
    boolean isValidToken(String token);
    long getExpirationSeconds(String token);
}

// Default HMAC implementation
@Component
@ConditionalOnProperty(name = "auth.jwt-algorithm", havingValue = "HS256", 
                       matchIfMissing = true)
public class HmacJwtProvider implements JwtProvider {
    private final JwtParser jwtParser;
    private final JwtBuilder jwtBuilder;
    
    public HmacJwtProvider(AuthProperties properties) {
        SecretKey key = Keys.hmacShaKeyFor(
            properties.getJwtSecret().getBytes(StandardCharsets.UTF_8)
        );
        this.jwtParser = Jwts.parserBuilder()
            .setSigningKey(key)
            .build();
        this.jwtBuilder = Jwts.builder()
            .signWith(key, SignatureAlgorithm.HS256);
    }
    
    @Override
    public String generateToken(JwtPayload payload) {
        return jwtBuilder
            .setSubject(payload.getSubject())
            .setIssuedAt(Date.from(payload.getIssuedAt()))
            .setExpiration(Date.from(payload.getExpiresAt()))
            .setIssuer(payload.getIssuer())
            .addClaims(buildClaims(payload))
            .compact();
    }
    
    @Override
    public JwtPayload validateAndParseToken(String token) throws JwtException {
        try {
            Jws<Claims> claimsJws = jwtParser.parseClaimsJws(token);
            return mapClaimsToPayload(claimsJws.getBody());
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token expired", JwtException.ErrorType.EXPIRED);
        }
    }
}

// Developer can create custom RSA implementation
@Component
@ConditionalOnProperty(name = "auth.jwt-algorithm", havingValue = "RSA")
public class RsaJwtProvider implements JwtProvider {
    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final JwtParser jwtParser;
    private final JwtBuilder jwtBuilder;
    
    public RsaJwtProvider(AuthProperties properties) {
        KeyPair keyPair = loadRsaKeyPair(properties.getJwtKeyPath());
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        this.jwtParser = Jwts.parserBuilder()
            .setSigningKey(publicKey)
            .build();
        this.jwtBuilder = Jwts.builder()
            .signWith(privateKey, SignatureAlgorithm.RS256);
    }
    
    @Override
    public String generateToken(JwtPayload payload) {
        // Similar to HMAC implementation
        return jwtBuilder
            .setSubject(payload.getSubject())
            .setIssuedAt(Date.from(payload.getIssuedAt()))
            .setExpiration(Date.from(payload.getExpiresAt()))
            .compact();
    }
}
```

### 1.4 Repository Pattern for Pluggable Storage

Allow developers to swap refresh token storage implementation:

```java
// Core interface
public interface RefreshTokenRepository {
    void save(RefreshToken token);
    Optional<RefreshToken> findByTokenAndValid(String token);
    List<RefreshToken> findByUserId(String userId);
    void invalidate(String token);
    void deleteExpired();
}

// In-memory implementation (default for testing)
@Component
@ConditionalOnProperty(name = "auth.refresh-token-storage", 
                       havingValue = "in-memory",
                       matchIfMissing = true)
public class InMemoryRefreshTokenStore implements RefreshTokenRepository {
    private final ConcurrentHashMap<String, RefreshToken> store = 
        new ConcurrentHashMap<>();
    
    @Override
    public void save(RefreshToken token) {
        store.put(token.getToken(), token);
    }
    
    @Override
    public Optional<RefreshToken> findByTokenAndValid(String token) {
        RefreshToken rt = store.get(token);
        if (rt == null || rt.isRevoked() || 
            rt.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(rt);
    }
}

// JPA implementation (production)
@Component
@ConditionalOnProperty(name = "auth.refresh-token-storage", havingValue = "jpa")
public class JpaRefreshTokenStore implements RefreshTokenRepository {
    private final JpaRefreshTokenStoreRepository repository;
    
    @Override
    public void save(RefreshToken token) {
        repository.save(token);
    }
    
    @Override
    public Optional<RefreshToken> findByTokenAndValid(String token) {
        return repository.findByTokenAndRevokedFalseAndExpiresAtAfter(
            token,
            Instant.now()
        );
    }
}

// Developer can create custom Redis implementation
@Component
@ConditionalOnProperty(name = "auth.refresh-token-storage", havingValue = "redis")
public class RedisRefreshTokenStore implements RefreshTokenRepository {
    private final RedisTemplate<String, RefreshToken> redisTemplate;
    
    @Override
    public void save(RefreshToken token) {
        long ttl = Duration.between(
            Instant.now(),
            token.getExpiresAt()
        ).getSeconds();
        
        redisTemplate.opsForValue().set(
            "refresh_token:" + token.getToken(),
            token,
            Duration.ofSeconds(ttl)
        );
    }
}
```

### 1.5 Custom Claims & Payload Extension

```java
// Allow developers to add custom claims to tokens
public interface JwtPayloadEnricher {
    void enrich(JwtPayload payload, AuthUserDetails userDetails);
}

@Component
public class DefaultJwtPayloadEnricher implements JwtPayloadEnricher {
    @Override
    public void enrich(JwtPayload payload, AuthUserDetails userDetails) {
        // No-op default implementation
    }
}

// In JWT generation
@Component
public class JwtTokenStore {
    private final List<JwtPayloadEnricher> enrichers;
    
    public String generateAccessToken(AuthUserDetails user) {
        JwtPayload payload = new JwtPayload();
        // ... basic payload setup
        
        // Allow enrichment
        enrichers.forEach(enricher -> enricher.enrich(payload, user));
        
        return jwtProvider.generateToken(payload);
    }
}

// Developer can implement custom enricher
@Component
public class CustomJwtPayloadEnricher implements JwtPayloadEnricher {
    @Override
    public void enrich(JwtPayload payload, AuthUserDetails userDetails) {
        if (userDetails instanceof CustomAuthUserDetails) {
            CustomAuthUserDetails custom = (CustomAuthUserDetails) userDetails;
            payload.setCustomClaims(custom.getOrgId(), custom.getTeamId());
        }
    }
}
```

---

## 2. Frontend Extensibility Deep Dive

### 2.1 Dependency Injection Token Pattern

```typescript
// config/injection-tokens.ts
import { InjectionToken } from '@angular/core';
import { TokenStorageService } from '../services/token-storage.service';
import { AuthService } from '../services/auth.service';

export const AUTH_CONFIG = new InjectionToken<AuthConfig>('auth.config');

export const TOKEN_STORAGE = new InjectionToken<TokenStorageService>(
  'token.storage'
);

export const AUTH_SERVICE = new InjectionToken<AuthService>(
  'auth.service'
);

// Allow developers to provide custom implementations
export const CUSTOM_AUTH_INTERCEPTOR = 
  new InjectionToken<HttpInterceptorFn[]>('custom.auth.interceptors', {
    factory: () => []
  });

export const CUSTOM_AUTH_GUARDS = 
  new InjectionToken<(CanActivateFn)[]>('custom.auth.guards', {
    factory: () => []
  });

// config/provide-auth.ts
export function provideAuth(
  config?: Partial<AuthConfig>,
  customProviders?: Provider[]
): Provider[] {
  const mergedConfig: AuthConfig = {
    ...DEFAULT_AUTH_CONFIG,
    ...config
  };
  
  return [
    { provide: AUTH_CONFIG, useValue: mergedConfig },
    
    // Default implementations
    {
      provide: TOKEN_STORAGE,
      useClass: TokenStorageService,
      multi: false
    },
    {
      provide: AuthService,
      useClass: AuthService
    },
    
    // HTTP interceptors
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
    
    // Allow custom providers to override defaults
    ...(customProviders || [])
  ];
}
```

### 2.2 Token Storage Strategy Pattern

```typescript
// services/token-storage.service.ts (Abstract)
export abstract class BaseTokenStorageService {
  abstract setAccessToken(token: string): void;
  abstract getAccessToken(): string | null;
  abstract setRefreshToken(token: string): void;
  abstract getRefreshToken(): string | null;
  abstract clear(): void;
  abstract hasToken(): boolean;
}

// Default localStorage implementation
@Injectable()
export class TokenStorageService extends BaseTokenStorageService {
  constructor(@Inject(AUTH_CONFIG) private config: AuthConfig) {
    super();
  }
  
  setAccessToken(token: string): void {
    localStorage.setItem(this.config.tokenStorageKey, token);
  }
  
  getAccessToken(): string | null {
    return localStorage.getItem(this.config.tokenStorageKey);
  }
}

// Developer can provide custom sessionStorage implementation
@Injectable()
export class SessionTokenStorage extends BaseTokenStorageService {
  constructor(@Inject(AUTH_CONFIG) private config: AuthConfig) {
    super();
  }
  
  setAccessToken(token: string): void {
    sessionStorage.setItem(this.config.tokenStorageKey, token);
  }
  
  getAccessToken(): string | null {
    return sessionStorage.getItem(this.config.tokenStorageKey);
  }
}

// Or in-memory implementation for testing
@Injectable()
export class InMemoryTokenStorage extends BaseTokenStorageService {
  private storage = new Map<string, string>();
  
  setAccessToken(token: string): void {
    this.storage.set('accessToken', token);
  }
  
  getAccessToken(): string | null {
    return this.storage.get('accessToken') || null;
  }
}

// Usage
provideAuth(
  { apiEndpoint: 'http://localhost:8080' },
  [
    {
      provide: TOKEN_STORAGE,
      useClass: SessionTokenStorage  // Use session instead of local
    }
  ]
);
```

### 2.3 Observable-Based State Management

```typescript
// services/auth-state.service.ts
@Injectable()
export class AuthStateService {
  private currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  
  private authErrorSubject = new Subject<AuthError>();
  public authError$ = this.authErrorSubject.asObservable();
  
  // Allow RxJS operations
  public canActivate$ = this.isAuthenticated$.pipe(
    switchMap(isAuth => isAuth ? of(true) : of(false))
  );
  
  public userPermissions$ = this.currentUser$.pipe(
    map(user => user?.permissions || []),
    distinctUntilChanged((prev, curr) => 
      JSON.stringify(prev) === JSON.stringify(curr)
    )
  );
  
  // Helper for checking specific role
  hasRole$(role: string): Observable<boolean> {
    return this.currentUser$.pipe(
      map(user => user?.roles.includes(role) || false)
    );
  }
}

// Developer can compose additional state streams
@Injectable()
export class EnhancedAuthStateService extends AuthStateService {
  public isAdmin$ = this.hasRole$('ADMIN');
  
  public canDeleteUsers$ = this.currentUser$.pipe(
    map(user => user?.permissions.includes('DELETE:USERS') || false)
  );
}
```

### 2.4 Custom Guard Pattern

```typescript
// Base guard with common logic
export abstract class BaseAuthGuard implements CanActivateFn {
  constructor(
    protected authService: AuthService,
    protected router: Router
  ) {}
  
  protected checkAuthentication(): Observable<boolean> {
    return this.authService.isAuthenticated$.pipe(
      tap(isAuth => {
        if (!isAuth) {
          this.router.navigate(['/login']);
        }
      })
    );
  }
}

// Developer can extend for custom logic
@Injectable()
export class SubscriptionGuard extends BaseAuthGuard {
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    return this.checkAuthentication().pipe(
      switchMap(isAuth => {
        if (!isAuth) return of(false);
        
        // Custom: Check subscription status
        return this.subscriptionService.getStatus().pipe(
          map(status => status.isActive),
          tap(isActive => {
            if (!isActive) {
              this.router.navigate(['/upgrade']);
            }
          })
        );
      })
    );
  }
}
```

### 2.5 Interceptor Chaining Pattern

```typescript
// Base interceptor with common logic
@Injectable()
export class BaseAuthInterceptor implements HttpInterceptor {
  constructor(
    protected authService: AuthService,
    @Inject(AUTH_CONFIG) protected config: AuthConfig
  ) {}
  
  protected shouldSkipInterceptor(url: string): boolean {
    if (this.config.blacklistedUrls) {
      return this.config.blacklistedUrls.some(blacklisted => 
        url.includes(blacklisted)
      );
    }
    return false;
  }
  
  protected addAuthHeader(
    request: HttpRequest<any>,
    token: string
  ): HttpRequest<any> {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }
}

// JWT Bearer interceptor
@Injectable()
export class JwtBearerInterceptor extends BaseAuthInterceptor {
  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    if (this.shouldSkipInterceptor(request.url)) {
      return next.handle(request);
    }
    
    const token = this.authService.getAccessToken();
    if (token) {
      request = this.addAuthHeader(request, token);
    }
    
    return next.handle(request);
  }
}

// Developer can create custom interceptor extending base
@Injectable()
export class CustomHeaderInterceptor extends BaseAuthInterceptor {
  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    if (this.shouldSkipInterceptor(request.url)) {
      return next.handle(request);
    }
    
    const token = this.authService.getAccessToken();
    if (token) {
      request = this.addAuthHeader(request, token).clone({
        setHeaders: {
          'X-Custom-Header': 'custom-value'
        }
      });
    }
    
    return next.handle(request);
  }
}

// Usage with multiple interceptors
provideAuth(
  { apiEndpoint: 'http://localhost:8080' },
  [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: CustomHeaderInterceptor,
      multi: true
    }
  ]
);
```

### 2.6 Feature Module Pattern

```typescript
// For developers using modules instead of standalone
import { NgModule } from '@angular/core';
import { AuthService } from './services/auth.service';
import { AuthGuard } from './guards/auth.guard';

@NgModule({
  providers: [
    AuthService,
    AuthGuard,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: JwtBearerInterceptor,
      multi: true
    }
  ]
})
export class AuthModule {
  static forRoot(config: AuthConfig): ModuleWithProviders<AuthModule> {
    return {
      ngModule: AuthModule,
      providers: [
        { provide: AUTH_CONFIG, useValue: config }
      ]
    };
  }
}

// App module usage
@NgModule({
  imports: [
    AuthModule.forRoot({
      apiEndpoint: 'http://localhost:8080'
    })
  ]
})
export class AppModule {}
```

---

## 3. Advanced Security Patterns

### 3.1 Token Rotation Strategy

```java
// Backend - Automatic token rotation on refresh
@Service
public class TokenRotationService {
    private final RefreshTokenRepository tokenRepository;
    private final AuthProperties properties;
    
    public RefreshTokenRotationResult rotateToken(String oldRefreshToken) {
        // Mark old token as revoked
        RefreshToken oldToken = tokenRepository
            .findByTokenAndValid(oldRefreshToken)
            .orElseThrow(() -> new InvalidTokenException());
        
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(Instant.now());
        tokenRepository.save(oldToken);
        
        // Generate new refresh token
        String newRefreshToken = generateNewRefreshToken();
        
        return new RefreshTokenRotationResult(
            oldRefreshToken,
            newRefreshToken,
            oldToken.getRevokedAt()
        );
    }
}
```

```typescript
// Frontend - Handle token rotation
@Injectable()
export class TokenRotationInterceptor {
  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      tap(event => {
        if (event instanceof HttpResponse) {
          const newRefreshToken = event.headers
            .get('X-New-Refresh-Token');
          
          if (newRefreshToken) {
            this.tokenStorage.setRefreshToken(newRefreshToken);
          }
        }
      })
    );
  }
}
```

### 3.2 Multi-Device Token Management

```java
@Service
public class MultiDeviceTokenService {
    private final RefreshTokenRepository tokenRepository;
    private final AuthProperties properties;
    
    public List<DeviceTokenInfo> getActiveTokens(String userId) {
        List<RefreshToken> tokens = tokenRepository
            .findByUserId(userId);
        
        return tokens.stream()
            .filter(t -> !t.isRevoked() && t.getExpiresAt()
                .isAfter(Instant.now()))
            .map(t -> new DeviceTokenInfo(
                t.getDeviceId(),
                t.getDeviceName(),
                t.getCreatedAt(),
                t.getLastUsedAt()
            ))
            .collect(Collectors.toList());
    }
    
    public void revokeDeviceToken(String userId, String deviceId) {
        List<RefreshToken> tokens = tokenRepository
            .findByUserId(userId);
        
        tokens.stream()
            .filter(t -> deviceId.equals(t.getDeviceId()))
            .forEach(t -> {
                t.setRevoked(true);
                tokenRepository.save(t);
            });
    }
    
    public void revokeAllDevices(String userId) {
        List<RefreshToken> tokens = tokenRepository
            .findByUserId(userId);
        
        tokens.forEach(t -> {
            t.setRevoked(true);
            tokenRepository.save(t);
        });
    }
}
```

### 3.3 Rate Limiting & Brute Force Protection

```java
@Component
public class LoginAttemptService {
    private final LoadingCache<String, Integer> failedAttempts;
    private final LoadingCache<String, Integer> successAttempts;
    
    public LoginAttemptService() {
        this.failedAttempts = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Integer>() {
                @Override
                public Integer load(String key) {
                    return 0;
                }
            });
        
        this.successAttempts = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Integer>() {
                @Override
                public Integer load(String key) {
                    return 0;
                }
            });
    }
    
    public void recordFailedAttempt(String username) {
        int attempts = failedAttempts.getUnchecked(username);
        failedAttempts.put(username, attempts + 1);
    }
    
    public void recordSuccessfulAttempt(String username) {
        failedAttempts.invalidate(username);
        int attempts = successAttempts.getUnchecked(username);
        successAttempts.put(username, attempts + 1);
    }
    
    public boolean isBlocked(String username) {
        return failedAttempts.getUnchecked(username) >= 5;
    }
}

// Use in auth controller
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    if (loginAttemptService.isBlocked(request.getUsername())) {
        throw new AccountLockedException("Too many failed attempts");
    }
    
    try {
        AuthResponse response = authService.login(
            request.getUsername(),
            request.getPassword()
        );
        loginAttemptService.recordSuccessfulAttempt(request.getUsername());
        return ResponseEntity.ok(response);
    } catch (BadCredentialsException e) {
        loginAttemptService.recordFailedAttempt(request.getUsername());
        throw e;
    }
}
```

### 3.4 CORS Security Configuration

```java
@Configuration
public class CorsSecurityConfiguration {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource(AuthProperties props) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(props.getAllowedOrigins());
        configuration.setAllowedMethods(props.getAllowedMethods());
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = 
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
```

---

## 4. Monitoring & Observability

### 4.1 Authentication Metrics (Backend)

```java
@Component
public class AuthenticationMetrics {
    private final MeterRegistry meterRegistry;
    
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter tokenRefreshCounter;
    private final Timer tokenValidationTimer;
    
    public AuthenticationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.loginSuccessCounter = Counter.builder(
            "auth.login.success"
        ).description("Number of successful logins")
            .register(meterRegistry);
        
        this.loginFailureCounter = Counter.builder(
            "auth.login.failure"
        ).description("Number of failed login attempts")
            .register(meterRegistry);
        
        this.tokenRefreshCounter = Counter.builder(
            "auth.token.refresh"
        ).description("Number of token refreshes")
            .register(meterRegistry);
        
        this.tokenValidationTimer = Timer.builder(
            "auth.token.validation"
        ).description("Time to validate token")
            .register(meterRegistry);
    }
    
    public void recordLoginSuccess() {
        loginSuccessCounter.increment();
    }
    
    public void recordLoginFailure() {
        loginFailureCounter.increment();
    }
    
    public void recordTokenRefresh() {
        tokenRefreshCounter.increment();
    }
    
    public <T> T recordTokenValidation(Supplier<T> supplier) {
        return tokenValidationTimer.recordCallable(() -> supplier.get());
    }
}
```

### 4.2 Authentication Logging (Frontend)

```typescript
@Injectable()
export class AuthLoggingService {
  constructor(private logger: LoggerService) {}
  
  logLogin(username: string): void {
    this.logger.info('[AUTH] User login', { username });
  }
  
  logLogout(userId: string): void {
    this.logger.info('[AUTH] User logout', { userId });
  }
  
  logTokenRefresh(): void {
    this.logger.debug('[AUTH] Token refreshed');
  }
  
  logTokenExpired(): void {
    this.logger.warn('[AUTH] Token expired');
  }
  
  logAuthError(error: any): void {
    this.logger.error('[AUTH] Authentication error', { error });
  }
}
```

---

## 5. Testing Strategies

### 5.1 Backend Unit Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
public class AuthServiceTest {
    
    @MockBean
    private UserDetailsProvider userDetailsProvider;
    
    @MockBean
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private AuthService authService;
    
    @Test
    public void testLoginSuccess() {
        // Given
        LoginRequest request = new LoginRequest("john", "password");
        AuthUserDetails userDetails = mock(AuthUserDetails.class);
        when(userDetailsProvider.loadUserByUsername("john"))
            .thenReturn(userDetails);
        
        // When
        LoginResponse response = authService.login("john", "password");
        
        // Then
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
    }
    
    @Test
    public void testRefreshTokenExpired() {
        // Given
        String expiredToken = "expired.token.here";
        when(refreshTokenRepository.findByTokenAndValid(expiredToken))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(RefreshTokenExpiredException.class, () ->
            authService.refreshToken(expiredToken)
        );
    }
}
```

### 5.2 Frontend Unit Tests

```typescript
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let tokenStorage: TokenStorageService;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        TokenStorageService,
        TokenDecoderService,
        {
          provide: AUTH_CONFIG,
          useValue: { apiEndpoint: 'http://localhost:8080' }
        }
      ]
    });
    
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    tokenStorage = TestBed.inject(TokenStorageService);
  });
  
  it('should login and store tokens', () => {
    const response: AuthResponse = {
      accessToken: 'access_token',
      refreshToken: 'refresh_token',
      expiresIn: 3600,
      user: { id: '1', username: 'john' }
    };
    
    service.login({ username: 'john', password: 'password' })
      .subscribe(result => {
        expect(result).toEqual(response);
        expect(tokenStorage.getAccessToken()).toBe('access_token');
      });
    
    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(response);
    
    httpMock.verify();
  });
});
```

