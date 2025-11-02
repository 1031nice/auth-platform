# Auth Server

Spring Boot + Java 21 + Gradle 기반 인증/인가 서버

## 기술 스택

- **Java**: 21
- **Spring Boot**: 3.2.0
- **Gradle**: 8.10.2
- **Spring Security**: JWT 기반 인증
- **JPA/Hibernate**: 데이터베이스 ORM
- **H2**: 인메모리 데이터베이스 (개발용)
- **PostgreSQL**: 프로덕션 데이터베이스
- **Lombok**: 보일러플레이트 코드 제거

## 프로젝트 구조

```
auth-server/
├── src/
│   ├── main/
│   │   ├── java/com/auth/server/
│   │   │   ├── config/           # 설정 클래스
│   │   │   │   ├── JwtConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/       # REST 컨트롤러
│   │   │   │   └── AuthController.java
│   │   │   ├── domain/           # 도메인 모델
│   │   │   │   ├── entity/       # 엔티티
│   │   │   │   ├── dto/          # DTO
│   │   │   │   │   ├── request/
│   │   │   │   │   └── response/
│   │   │   │   └── enums/        # 열거형
│   │   │   ├── exception/        # 예외 처리
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── repository/       # 데이터 접근 계층
│   │   │   │   └── UserRepository.java
│   │   │   ├── security/         # 보안 관련
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   ├── service/          # 비즈니스 로직
│   │   │   │   └── AuthService.java
│   │   │   └── AuthServerApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/auth/server/
├── build.gradle
├── settings.gradle
├── .gitignore
└── README.md
```

## 주요 기능

- [x] 회원가입 (Sign Up)
- [x] 로그인 (Login)
- [x] JWT 토큰 발급 및 검증
- [x] Spring Security 통합
- [x] 비밀번호 암호화 (BCrypt)
- [x] 역할 기반 접근 제어 (RBAC)
- [x] 전역 예외 처리

## 사전 요구사항

- **Java 21** (필수)
- Gradle 8.10.2+ (자동 설치)

### Java 21 설치 확인

```bash
java -version
# openjdk version "21.x.x" 확인
```

### Java 21로 설정 (macOS)

```bash
export JAVA_HOME=/Users/east/Library/Java/JavaVirtualMachines/temurin-21.0.8/Contents/Home
# 또는 자동 찾기
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

## 실행 방법

### 1. 프로젝트 클론 및 빌드

```bash
cd auth-server
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # Java 21 설정
./gradlew build
```

### 2. 애플리케이션 실행

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # Java 21 설정
./gradlew bootRun
```

또는

```bash
java -jar build/libs/auth-server-0.0.1-SNAPSHOT.jar
```

### 3. 접속 확인

- 애플리케이션: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console
- Health Check: http://localhost:8080/api/v1/auth/health

### 4. 코드 포맷팅

```bash
# Apply code formatting automatically
./gradlew spotlessApply

# Check if code is properly formatted
./gradlew spotlessCheck
```

## API 엔드포인트

### 회원가입

```bash
POST /api/v1/auth/signup
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123"
}
```

응답:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["ROLE_USER"]
}
```

### 로그인

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

응답:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["ROLE_USER"]
}
```

## 설정

`application.yml`에서 다음 설정을 변경할 수 있습니다:

```yaml
jwt:
  secret: your-secret-key  # 프로덕션에서는 환경 변수로 설정
  expiration: 86400000     # 24시간 (밀리초)

server:
  port: 8080
```

## 개발 환경 설정

### VS Code IDE 설정

**중요**: VS Code에서 Java 파일을 편집하기 전에 다음 단계를 수행하세요:

1. **명령 팔레트 열기**: `Cmd+Shift+P` (macOS) 또는 `Ctrl+Shift+P` (Windows/Linux)
2. **Java 프로젝트 재로드**: `Java: Clean Java Language Server Workspace` 선택
3. **재시작**: VS Code에서 "Restart and delete" 클릭

이렇게 하면 VS Code가 Gradle 프로젝트를 올바르게 인식하고 의존성을 로드합니다.

### VS Code 확장 프로그램

프로젝트를 열면 아래 확장 프로그램 설치가 자동으로 추천됩니다:

- **Java Extension Pack**: Java 개발 지원
- **Spring Boot Tools**: Spring Boot 개발 지원
- **IntelliJ IDEA Keybindings**: IntelliJ IDEA 단축키 지원 (선택)

확장 프로그램을 설치하려면:
1. VS Code 좌측 사이드바의 **확장** 아이콘 클릭
2. 또는 `Cmd+Shift+X` (macOS) / `Ctrl+Shift+X` (Windows/Linux)
3. "**추천**" 탭에서 설치 가능한 확장 프로그램 확인
4. "**설치**" 버튼 클릭

### 디버깅

IDE에서 디버깅하려면:
1. `Cmd+Shift+P` → `Java: Clean Java Language Server Workspace`
2. VS Code 재시작
3. `AuthServerApplication.java` 파일에서 좌측 여백의 **빨간 점** 클릭하거나 `F5` 키 누르기

**주의**: IDE에서 직접 실행하면 Gradle 의존성을 인식하지 못할 수 있습니다. 이 경우 터미널에서 `./gradlew bootRun` 명령어를 사용하세요.

## 다음 단계

- [ ] 리프레시 토큰 구현
- [ ] 토큰 블랙리스트 관리
- [ ] OAuth2 통합
- [ ] 이메일 인증
- [ ] 비밀번호 재설정
- [ ] 다중 팩터 인증 (MFA)
- [ ] API 문서화 (Swagger/OpenAPI)
- [ ] 단위 테스트 및 통합 테스트

## 라이선스

MIT
