## 🎯 구현 가이드

### 📋 프로젝트 개요

**Java Reactive Gateway for AI Engine**
- Python AI Engine의 프로덕션 게이트웨이
- WebFlux 기반 비동기 처리
- MongoDB를 이용한 리액티브 데이터 관리

---

## 🏗️ 기술 스택

### Core Framework
- **Spring Boot 3.x**
- **Spring WebFlux** (Reactive Web)
- **Spring Data MongoDB Reactive**
- **Project Reactor** (Mono/Flux)

### Database & Cache
- **MongoDB** (메인 DB - 채팅 히스토리, 세션)
- **Redis** (캐싱, Rate Limiting)

### Communication
- **WebClient** (Python AI Engine 호출)
- **Spring Cloud Gateway** (선택사항 - API Gateway 분리 시)

### Security & Monitoring
- **Spring Security Reactive** (JWT 인증)
- **Spring Actuator** (헬스체크, 메트릭)
- **Micrometer** (프로메테우스 연동)

### Development
- **Lombok** (보일러플레이트 제거)
- **Logback** (로깅)

---

## 📁 프로젝트 구조

```
java-ai-gateway/
├── src/main/java/com/example/aigateway/
│   ├── router/                    # RouterFunction 방식
│   │   ├── ChatRouter.java        # 채팅 라우팅
│   │   ├── HealthRouter.java      # 헬스체크
│   │   └── HistoryRouter.java     # 히스토리 조회
│   │
│   ├── handler/                   # 핸들러 (컨트롤러 대신)
│   │   ├── ChatHandler.java       # 채팅 처리
│   │   ├── SessionHandler.java    # 세션 관리
│   │   └── HistoryHandler.java    # 히스토리 조회
│   │
│   ├── service/
│   │   ├── AiEngineClient.java    # Python 호출
│   │   ├── SessionService.java    # 세션 관리
│   │   ├── ContextManager.java    # 대화 컨텍스트 관리
│   │   └── CacheService.java      # 캐싱 로직
│   │
│   ├── repository/
│   │   ├── ChatSessionRepository.java  # 세션 리포지토리
│   │   └── MessageHistoryRepository.java # 메시지 히스토리
│   │
│   ├── domain/                    # 도메인 모델
│   │   ├── ChatSession.java       # 세션 엔티티
│   │   ├── Message.java           # 메시지
│   │   └── Context.java           # 대화 컨텍스트
│   │
│   ├── filter/                    # WebFilter
│   │   ├── AuthenticationFilter.java   # JWT 검증
│   │   ├── RateLimitFilter.java        # Rate Limiting
│   │   └── LoggingFilter.java          # 요청/응답 로깅
│   │
│   └── config/
│       ├── WebFluxConfig.java
│       ├── MongoConfig.java
│       └── RedisConfig.java
│
├── src/main/resources/
│   ├── static/                    # 프론트엔드 (Python에서 이동)
│   │   ├── css/
│   │   ├── js/
│   │   └── index.html
│   └── application.yml
│
└── pom.xml / build.gradle
```

---

## 🔄 Java가 담당할 역할

### 1. **API Gateway 역할**
- 모든 외부 요청의 진입점
- 인증/인가 처리 (JWT)
- Rate Limiting
- CORS 처리
- 요청/응답 로깅

### 2. **세션 및 컨텍스트 관리** ⭐
```
MongoDB에 저장되는 구조:
ChatSession {
  sessionId: String
  userId: String
  context: {
    messages: [
      { role: "user", content: "...", timestamp: ... },
      { role: "assistant", content: "...", timestamp: ... }
    ]
    metadata: { ... }
  }
  createdAt: DateTime
  lastAccessedAt: DateTime
}
```

**역할:**
- 대화 히스토리 저장 및 조회
- 컨텍스트 윈도우 관리 (최근 N개 메시지만 유지)
- Python AI Engine에 컨텍스트 전달

### 3. **캐싱 레이어**
- 동일 쿼리 응답 캐싱 (Redis)
- 세션 정보 캐싱
- TTL 기반 자동 만료

### 4. **에러 처리 및 재시도**
- Python AI Engine 장애 시 재시도
- Circuit Breaker 패턴
- Fallback 응답

### 5. **모니터링 및 메트릭**
- 응답 시간 측정
- 에러율 추적
- AI Engine 헬스체크

---

## 💬 컨텍스트 관리 상세

### Q: Java에서 컨텍스트 관리가 힘들지 않나?

**A: 전혀 힘들지 않습니다!** 오히려 Java가 더 적합할 수 있어요.

### 컨텍스트 관리 플로우

```
[사용자 요청]
    ↓
[Java Gateway]
    ↓
1. MongoDB에서 세션 조회
    - sessionId로 기존 대화 내역 조회
    - 최근 N개 메시지만 추출 (컨텍스트 윈도우)
    ↓
2. Python AI Engine에 요청
    - 새 메시지 + 이전 컨텍스트 함께 전달
    {
      "message": "현재 질문",
      "session_id": "xxx",
      "context": [
        {"role": "user", "content": "이전 질문1"},
        {"role": "assistant", "content": "이전 답변1"},
        ...
      ]
    }
    ↓
3. AI Engine 응답 받기
    ↓
4. MongoDB에 저장
    - 새 메시지 (user + assistant) 추가
    - lastAccessedAt 업데이트
    ↓
[사용자 응답]
```

### Java의 장점

1. **타입 안정성**
   - 세션, 메시지 구조를 명확한 클래스로 정의
   - 컴파일 타임 에러 체크

2. **트랜잭션 관리**
   - MongoDB Reactive Transaction 지원
   - 메시지 저장 실패 시 롤백 가능

3. **동시성 처리**
   - Reactor의 Mono/Flux로 비동기 처리
   - 여러 세션 동시 처리 용이

4. **메모리 관리**
   - 오래된 컨텍스트 자동 정리
   - 메모리 효율적인 페이징

### Python은?

- 컨텍스트를 **받기만** 하면 됨
- 저장/관리 책임 없음
- 순수하게 AI 추론만 집중

---

## 🌐 프론트엔드 이동에 대한 의견

### ✅ **Java로 옮기는 것을 강력 추천합니다**

**이유:**

1. **단일 진입점**
   - 사용자는 Java Gateway만 접속
   - Python은 내부 서비스로 숨김
   - 보안상 유리 (Python 직접 노출 X)

2. **정적 파일 서빙**
   - WebFlux도 static resources 서빙 가능
   - `src/main/resources/static/` 에 두면 자동 서빙
   - Vue/React 몰라도 HTML/JS/CSS면 충분

3. **일관된 도메인**
   - 프론트엔드: `https://your-domain.com`
   - API: `https://your-domain.com/api/chat`
   - 간단하고 깔끔함

4. **CORS 문제 없음**
   - 같은 도메인에서 서빙하므로 CORS 불필요

### 프론트엔드 구조

```
src/main/resources/
├── static/
│   ├── index.html           # Python에서 이동
│   ├── css/
│   │   └── style.css        # Python에서 이동
│   └── js/
│       └── chat.js          # Python에서 이동
│           ↓ 수정 필요
│           fetch('http://localhost:8000/api/chat')  // Python 직접 호출
│           ↓
│           fetch('/api/chat')  // Java Gateway 호출 (상대 경로)
```

### 수정할 내용

**JavaScript에서:**
```javascript
// 변경 전 (Python 직접 호출)
fetch('http://localhost:8000/api/chat/query', ...)

// 변경 후 (Java Gateway 호출)
fetch('/api/chat/query', ...)
```

**이게 전부입니다!** Vue/React 몰라도 됨.

---

## 🚀 구현 순서 가이드

### Phase 1: 기본 구조 (1-2일)
- [ ] Spring Boot 프로젝트 생성
- [ ] WebFlux 의존성 추가
- [ ] MongoDB 연결 설정
- [ ] 기본 Router/Handler 구조 생성

### Phase 2: AI Engine 통신 (1일)
- [ ] WebClient로 Python 호출
- [ ] 간단한 프록시 구현
- [ ] 에러 핸들링

### Phase 3: 세션 관리 (2-3일)
- [ ] MongoDB 도메인 모델 정의
- [ ] 세션 생성/조회 로직
- [ ] 컨텍스트 저장/로드

### Phase 4: 캐싱 및 최적화 (1-2일)
- [ ] Redis 연결
- [ ] 응답 캐싱
- [ ] Rate Limiting

### Phase 5: 프론트엔드 통합 (1일)
- [ ] static 파일 이동
- [ ] JS에서 엔드포인트 수정
- [ ] 테스트

### Phase 6: 보안 및 모니터링 (선택사항)
- [ ] JWT 인증
- [ ] Actuator 설정
- [ ] 로깅 개선

**총 예상 시간: 1-2주**

---

## 📊 최종 아키텍처

```
[브라우저]
    ↓ HTTP
[Java Gateway - WebFlux]
    ├─ Router (요청 라우팅)
    ├─ Filter (인증, Rate Limit)
    ├─ Handler (비즈니스 로직)
    ├─ MongoDB (세션, 히스토리)
    ├─ Redis (캐시)
    └─ WebClient → [Python AI Engine]
                        ├─ LangGraph
                        ├─ Ollama
                        └─ RAG System
```

---

## 🎯 기대 효과

### 포트폴리오 관점
1. **Java 백엔드 역량**
   - Spring WebFlux (최신 기술)
   - Reactive Programming
   - MongoDB Reactive
   - 비동기 처리

2. **실무적 고민**
   - 세션 관리
   - 캐싱 전략
   - 에러 핸들링
   - 모니터링

3. **아키텍처 설계**
   - 폴리글랏 아키텍처
   - 책임 분리 (AI vs Business)
   - 확장 가능한 구조

### README 스토리
```markdown
## Why Two Services?

**Python AI Engine**: AI/ML 프로토타입
- 빠른 개발과 검증에 집중
- LangChain, ChromaDB 등 Python 생태계 활용

**Java Gateway**: 프로덕션 안정성
- 세션 관리, 인증, 캐싱 등 엔터프라이즈 기능
- WebFlux 기반 고성능 비동기 처리
- MongoDB를 통한 대화 컨텍스트 영구 저장

각 언어의 강점을 살린 Polyglot Architecture
```

---

**이 구조로 진행하면:**
- ✅ Python 코드 재사용
- ✅ Java 역량 어필
- ✅ 실무적인 문제 해결 경험
- ✅ 프론트엔드 통합 (Vue/React 불필요)
