# Benatti Auth Starter

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-17%2B-brightgreen)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1%2B-brightgreen)]()

Production-ready Spring Boot 3.x starter library for JWT authentication and authorization.

## 📚 Features

- ✅ **JWT Authentication** - Support for HMAC and RSA algorithms
- ✅ **Token Refresh** - Long-lived tokens with lifecycle management
- ✅ **Multi-Device** - Support for multiple active sessions on devices
- ✅ **Extensibility** - 100% customizable through interfaces and DI
- ✅ **Security** - OWASP best practices, rate limiting, CORS
- ✅ **Event-Driven** - Event system for cross-cutting concerns
- ✅ **Production-Ready** - Metrics, logging, error handling

## 🚀 Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.benatti</groupId>
    <artifactId>benatti-auth-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configuration application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: postgres
    password: postgres

auth:
  enabled: true
  jwt-secret: your-super-secret-jwt-key-min-32-chars-long!
  jwt-algorithm: HS256
  access-token-expiration-minutes: 60
  refresh-token-expiration-days: 30
  refresh-token-storage: jpa
  allowed-origins: http://localhost:4200
```

### 3. Implement UserDetailsProvider

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
        };
    }
}
```

### 4. Run

```bash
mvn spring-boot:run
```

## 📖 Documentation

- [IMPLEMENTATION_PLAN.md](../IMPLEMENTATION_PLAN.md) - Full implementation plan
- [EXTENSIBILITY_PATTERNS.md](../EXTENSIBILITY_PATTERNS.md) - Extension patterns

## 🏗️ Project Structure

```
benatti-auth-starter/
├── pom.xml
├── PHASE_1_ARCHITECTURE.md
└── src/main/java/com/benatti/auth/
    ├── auth/           - Main services
    ├── jwt/            - JWT providers
    ├── user/           - User management
    ├── storage/        - Token storage
    ├── dto/            - Data Transfer Objects
    └── exception/      - Custom exceptions
```

## 🔐 API Endpoints

### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "john.doe",
  "password": "password123"
}
```

**Response (200 OK)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john.doe",
    "email": "john@example.com",
    "roles": ["USER", "ADMIN"],
    "permissions": ["READ:USERS", "WRITE:USERS"]
  }
}
```

### Refresh Token
```
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

### Validate Token
```
GET /api/auth/validate
Authorization: Bearer {accessToken}
```

**Response (200 OK)**:
```json
{
  "valid": true,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "roles": ["USER", "ADMIN"],
  "permissions": ["READ:USERS", "WRITE:USERS"]
}
```

## 🎯 Development Status

### Phase 1: ✅ Architecture (Complete)
- [x] Interfaces and contracts
- [x] DTO models
- [x] Exception classes

### Phase 2: ✅ Backend Implementation
- [x] JWT providers (HMAC, RSA)
- [x] AuthService implementation
- [x] Spring Security configuration
- [x] REST controllers
- [x] Unit tests

### Phase 3: ✅ Frontend (Angular)
- [x] ng-auth-lib library
- [x] Guards and Interceptors
- [x] AuthService and Storage
- [x] Unit tests

### Phase 4: Demo Integration
- [ ] Demo backend app
- [ ] Demo frontend app

### Phase 5: CI/CD & Publication
- [ ] GitHub Actions workflows
- [ ] Maven Central publication
- [ ] npm publication

## 🤝 Contributing

Contributions are welcome! Please create a Pull Request.

## 📄 License

MIT License - see [LICENSE](LICENSE) file

---

**Developer**: Benatti Dev
**Version**: 1.0.0 (In Development)
**Status**: Phase 1 Complete ✅
