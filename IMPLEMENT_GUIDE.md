## 🎯 구현 가이드

### 📋 프로젝트 개요

**Java Reactive Gateway for AI Engine**
- Python AI Engine의 프로덕션 게이트웨이
- WebFlux 기반 비동기 처리
- MongoDB를 이용한 리액티브 데이터 관리
- Redis 캐싱으로 성능 최적화

---

## 🏗️ 기술 스택

### Core Framework
- **Spring Boot 3.x**
- **Spring WebFlux** (Reactive Web)
- **Spring Data MongoDB Reactive**
- **Project Reactor** (Mono/Flux)

### Database & Cache
- **MongoDB** (메인 DB - 채팅 히스토리, 세션)
- **Redis** (캐싱)

### Communication
- **WebClient** (Python AI Engine 호출)

### Security & Monitoring
- **Spring Security Reactive** (JWT 인증)
- **Spring Actuator** (헬스체크, 메트릭)
- **Micrometer** (프로메테우스 연동)


---

## 📁 프로젝트 구조 (실제 구현)

```
java-ai-gateway/
├── src/main/java/com/labg/aigateway/
│   ├── router/
│   │   └── ChatRouter.java        # 채팅 라우팅 ✅
│   │
│   ├── handler/
│   │   └── ChatHandler.java       # 채팅 처리 ✅
│   │
│   ├── service/
│   │   ├── AiEngineClient.java    # Python 호출 ✅
│   │   ├── SessionService.java    # 세션 관리 ✅
│   │   ├── ContextManager.java    # 컨텍스트 관리 ✅
│   │   └── CacheService.java      # 캐싱 로직 ✅
│   │
│   ├── repository/
│   │   └── ChatSessionRepository.java  # 세션 리포지토리 ✅
│   │
│   ├── domain/
│   │   ├── ChatSession.java       # 세션 엔티티 ✅
│   │   └── Message.java           # 메시지 ✅
│   │
│   ├── filter/                    # WebFilter
│   │   ├── AuthenticationFilter.java   # JWT 검증 (구현 예정)
│   │   ├── RateLimitFilter.java        # Rate Limiting (구현 예정)
│   │   └── LoggingFilter.java          # 요청/응답 로깅 (구현 예정)
│   │
│   └── config/
│       ├── WebFluxConfig.java     ✅
│       ├── MongoConfig.java       ✅
│       └── RedisConfig.java       ✅
│
├── src/main/resources/
│   └── application.yml            ✅
│
└── build.gradle                   ✅
```

---

## ✅ 구현 완료된 기능

### Phase 1: 기본 구조 ✅
- [x] Spring Boot 프로젝트 생성
- [x] WebFlux 의존성 추가
- [x] MongoDB 연결 설정
- [x] 기본 Router/Handler 구조 생성

### Phase 2: AI Engine 통신 ✅
- [x] WebClient로 Python 호출
- [x] 프록시 구현 (`/api/chat/query`)
- [x] 기본 에러 핸들링
- [x] Circuit Breaker 구현 (Resilience4j)

### Phase 3: 세션 관리 ✅
- [x] MongoDB 도메인 모델 정의
- [x] 세션 생성/조회 로직
- [x] 컨텍스트 저장/로드
- [x] 컨텍스트 윈도우 관리 (최근 N개 메시지)
- [x] 토큰 제한 처리 (최대 4000 토큰)

### Phase 4: 캐싱 및 최적화 ✅
- [x] Redis 연결
- [x] 응답 캐싱 (TTL 5분)
- [x] 세션 캐싱 (TTL 10분)
- [x] 캐시 무효화 로직

---

## 🚧 구현해야 할 기능 (우선순위)

### 🔴 High Priority (필수)

#### 1. Health Check API
**목적:** 배포/모니터링용 기본 헬스체크  
**예상 시간:** 0.5일  
**구현 내용:**
- `HealthRouter` + `HealthHandler` 생성
- MongoDB, Redis, Python AI Engine 연결 확인
- `/api/health` 엔드포인트

#### 2. 에러 처리 강화
**목적:** 표준화된 에러 응답  
**예상 시간:** 0.5일  
**구현 내용:**
- 전역 에러 핸들러 (WebFlux `ErrorWebExceptionHandler`)
- 표준 에러 응답 DTO
- 4xx vs 5xx 구분

#### 3. 프론트엔드 통합
**목적:** 단일 진입점 제공  
**예상 시간:** 1일  
**구현 내용:**
- Python의 static 파일을 `src/main/resources/static/`로 이동
- JS에서 엔드포인트를 상대 경로로 수정 (`/api/chat/query`)
- WebFlux 정적 리소스 서빙 확인

#### 4. JWT 인증
**목적:** 보안 강화 및 사용자 식별  
**예상 시간:** 2일  
**구현 내용:**
- `AuthenticationFilter` 구현 (WebFilter)
- JWT 토큰 생성/검증 로직
- Spring Security Reactive 통합
- `/api/auth/login` 엔드포인트 (선택)
- 인증된 사용자만 API 접근 가능하도록 설정

---

### 🟡 Medium Priority (권장)

#### 5. Rate Limiting
**예상 시간:** 1일  
**구현 내용:**
- Redis 기반 Rate Limiter
- IP/사용자별 제한
- 429 응답
- `RateLimitFilter` 구현

#### 6. 로깅 개선
**예상 시간:** 0.5일  
**구현 내용:**
- 구조화된 로깅 (JSON)
- 요청 ID (Trace ID)
- `LoggingFilter` 구현

---

## ❌ 제외된 기능 (Scope 축소)

다음 기능들은 **의도적으로 제외**하여 프로젝트를 간결하게 유지:

- **History 조회 API**: 세션 내 메시지는 이미 포함되어 있음
- **Session 조회 API**: 세션은 자동 관리되므로 별도 조회 불필요
- **Spring Actuator**: 기본 헬스체크만으로 충분
- **프로메테우스 메트릭**: 모니터링은 선택사항

---

## 🔄 핵심 플로우

```
[사용자 요청]
    ↓
[ChatHandler]
    ├─ 세션 조회/생성 (SessionService)
    ├─ 캐시 확인 (CacheService)
    │   ├─ HIT: 캐시된 응답 반환
    │   └─ MISS: AI Engine 호출
    ├─ 컨텍스트 추출 (ContextManager)
    ├─ AI Engine 호출 (AiEngineClient)
    ├─ 메시지 저장 (SessionService)
    └─ 캐시 저장 (CacheService)
    ↓
[사용자 응답]
```

---

## 📊 최종 아키텍처

```
[브라우저]
    ↓ HTTP
[Java Gateway - WebFlux]
    ├─ Filter (JWT 인증, Rate Limit)
    ├─ Router (요청 라우팅)
    ├─ Handler (비즈니스 로직)
    ├─ MongoDB (세션, 히스토리)
    ├─ Redis (캐시)
    └─ WebClient → [Python AI Engine]
        └─ Circuit Breaker (Resilience4j)
```

---

## 🎯 구현 목표

### 핵심 가치
1. **Java 백엔드 역량**
   - Spring WebFlux (최신 기술)
   - Reactive Programming
   - MongoDB Reactive
   - 비동기 처리

2. **실무적 고민**
   - 세션 관리
   - 캐싱 전략
   - 에러 핸들링
   - Circuit Breaker 패턴
   - JWT 인증/인가

3. **아키텍처 설계**
   - 폴리글랏 아키텍처
   - 책임 분리 (AI vs Business)

### README 스토리
```markdown
## Why Two Services?

**Python AI Engine**: AI/ML 프로토타입
- 빠른 개발과 검증에 집중
- LangChain, ChromaDB 등 Python 생태계 활용

**Java Gateway**: 프로덕션 안정성
- 세션 관리, 캐싱, JWT 인증 등 엔터프라이즈 기능
- WebFlux 기반 고성능 비동기 처리
- Circuit Breaker로 장애 격리
- MongoDB를 통한 대화 컨텍스트 영구 저장

각 언어의 강점을 살린 Polyglot Architecture
```

---

## 📝 다음 단계

1. **Health Check API** 구현 (0.5일)
2. **에러 처리 강화** (0.5일)
3. **프론트엔드 통합** (1일) - 필수
4. **JWT 인증** (2일) - 필수

**총 예상 시간: 4일**

---

**중요:** 프로젝트를 간결하게 유지하여 핵심 기능에 집중하세요! 🚀
