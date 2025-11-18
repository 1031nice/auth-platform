# Auth Platform

인증 및 인가 플랫폼 프로젝트. OAuth2/OIDC 표준을 준수하는 인증 및 인가 서버를 제공하는 멀티모듈 프로젝트입니다.

## 프로젝트 구조

이 프로젝트는 다음 두 개의 서브모듈로 구성됩니다:

- **oauth2-server**: OAuth2 Authorization Server (포트: 8081) - 사용자 인증 및 토큰 발급
- **resource-server**: OAuth2 Resource Server (포트: 8082) - 보호된 리소스 제공

## 기술 스택
- Java 21
- Spring Boot 3.2
- Spring Security
- Spring Security OAuth2 Authorization Server
- JPA/Hibernate
- H2 (개발용 인메모리 DB)
- Redis (Rate Limiting용)
- Bucket4j (Rate Limiting 라이브러리)

## 현재 제공 기능

### oauth2-server (OAuth2 Authorization Server)
- **사용자 인증**: 폼 기반 로그인 (`/login`)
- **OAuth2 Authorization Server**: 표준 OAuth2/OIDC 엔드포인트 제공
- **OAuth2 Client 관리**: OAuth2 클라이언트 등록 및 관리 API
- **지원하는 Grant Types**: 
  - `authorization_code`: 인증 코드 플로우 (웹 애플리케이션용)
  - `client_credentials`: 클라이언트 자격 증명 플로우 (서버 간 통신용)
  - `refresh_token`: 리프레시 토큰 플로우 (토큰 갱신용)
- **JWK Set 엔드포인트**: `/oauth2/jwks` - Resource Server에서 토큰 검증에 사용
- **역할 기반 접근 제어**: 사용자 권한(RBAC) 적용
- **비밀번호 암호화**: BCrypt 이용
- **Refresh Token Rotation (RTR)**: Refresh Token 사용 시 자동 회전 및 재사용 감지
- **PKCE (Proof Key for Code Exchange)**: 공개 클라이언트 보안 강화
- **Audit Logging**: 인증 및 토큰 발급 이벤트 로깅
- **Rate Limiting**: Redis 기반 요청 제한

### resource-server (OAuth2 리소스 서버)
- **OIDC UserInfo 엔드포인트**: `/userinfo` - OAuth2 토큰으로 사용자 정보 제공
- **JWT 토큰 검증**: OAuth2 Authorization Server에서 발급한 토큰 검증
- **보호된 리소스 제공**: OAuth2 토큰 기반 인증이 필요한 리소스 제공

## 실행 방법

### 사전 요구사항
- Java 21
- Docker & Docker Compose (Redis 실행용)

### 1. Redis 실행
```bash
docker-compose up -d redis
```

### 2. 애플리케이션 실행

#### oauth2-server 실행
```bash
./gradlew :oauth2-server:bootRun
```

#### resource-server 실행
```bash
./gradlew :resource-server:bootRun
```

#### 전체 빌드
```bash
./gradlew build
```

### Redis 중지
```bash
docker-compose down
```

## 주요 엔드포인트

### oauth2-server 엔드포인트

#### 사용자 로그인
OAuth2 Authorization Code Flow를 사용할 때 사용자가 로그인하는 페이지입니다.

`GET /login` - 로그인 페이지
`POST /login` - 로그인 처리

#### OAuth2 Client 등록
OAuth2를 사용하기 전에 클라이언트를 등록해야 합니다.

`POST /api/v1/oauth2/clients`
```json
{
  "clientId": "my-client",
  "clientSecret": "my-secret",
  "redirectUris": ["http://localhost:3000/callback"],
  "scopes": ["read", "write"],
  "grantTypes": ["authorization_code", "refresh_token", "client_credentials"]
}
```

#### OAuth2 Client 관리
- `GET /api/v1/oauth2/clients/{clientId}` - 클라이언트 조회
- `GET /api/v1/oauth2/clients` - 전체 클라이언트 목록
- `DELETE /api/v1/oauth2/clients/{clientId}` - 클라이언트 삭제

#### OAuth2 Authorization Code Flow
1. **인증 요청**: 사용자를 인증 서버로 리다이렉트
```
GET /oauth2/authorize?client_id=my-client&response_type=code&redirect_uri=http://localhost:3000/callback&scope=read write
```

2. **토큰 발급**: 인증 코드로 액세스 토큰 교환
```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code={authorization_code}&client_id=my-client&client_secret=my-secret&redirect_uri=http://localhost:3000/callback
```

응답:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "refresh_token": "eyJhbGciOiJSUzI1NiJ9...",
  "scope": "read write"
}
```

#### OAuth2 Client Credentials Flow
서버 간 통신에 사용하는 플로우입니다.

```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {base64(client_id:client_secret)}

grant_type=client_credentials&scope=read write
```

응답:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "scope": "read write"
}
```

#### OAuth2 Token Refresh
리프레시 토큰으로 새로운 액세스 토큰을 발급받습니다.

```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {base64(client_id:client_secret)}

grant_type=refresh_token&refresh_token={refresh_token}
```

#### OAuth2 Grant Types 지원
- `authorization_code`: 인증 코드 플로우 (웹 애플리케이션용)
- `client_credentials`: 클라이언트 자격 증명 플로우 (서버 간 통신용)
- `refresh_token`: 리프레시 토큰 플로우 (토큰 갱신용)

### resource-server 엔드포인트

#### OIDC UserInfo
OAuth2 Authorization Server에서 발급한 액세스 토큰을 사용하여 사용자 정보를 조회합니다.

```
GET /userinfo
Authorization: Bearer {access_token}
```

응답:
```json
{
  "sub": "1",
  "name": "demo",
  "email": "demo@example.com",
  "email_verified": true,
  "preferred_username": "demo"
}
```

