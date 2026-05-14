# demo-backend-app

Spring Boot demo application showing how to integrate `benatti-auth-starter`.

## Prerequisites

- Java 17+
- Maven 3.9+
- `benatti-auth-starter` installed in local Maven repo (see step below)

## Setup

```bash
# 1. Install the starter library into local .m2 (run from repo root)
cd ../benatti-auth-starter && mvn install -DskipTests && cd ../demo-backend-app

# 2. Run the demo
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.

## Sample accounts

| Username | Password | Roles           |
|----------|----------|-----------------|
| alice    | password | USER            |
| bob      | password | USER, ADMIN     |

## Endpoints

### Auth (auto-registered by benatti-auth-starter)

```
POST /api/auth/login     — { "username": "alice", "password": "password" }
POST /api/auth/refresh   — { "refreshToken": "..." }
POST /api/auth/logout    — { "refreshToken": "..." }
GET  /api/auth/validate  — Authorization: Bearer <token>
```

### Demo (JWT required)

```
GET /api/users/me         — returns your profile
GET /api/users/token-info — returns token claims
GET /api/admin/stats      — ADMIN only
GET /api/admin/users      — ADMIN only
```

## Quick curl test

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password"}' | jq -r '.accessToken')

# Access protected endpoint
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/me | jq .
```
