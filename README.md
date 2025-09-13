# 💬 Real-Time Chat Platform (Spring Boot / WebSocket / Redis)

> 실시간 1:1 채팅, 친구 연결(초대/수락/거절/끊기), 채널 생성/입장, 메시지 송수신, 세션 TTL 연장을 구현한 백엔드 프로젝트입니다.  
> **Spring Boot + WebSocket + Spring Security + Spring Session (Redis) + JPA(Hibernate)** 기반으로 설계되었습니다.

---

## Table of Contents
- [Why This Project](#why-this-project)
- [Features (현재 구현 범위)](#features-현재-구현-범위)
- [Tech Stack](#tech-stack)
- [Folder Structure](#folter-structure)
- [Database Schema (요약)](#database-schema-요약)
- [Auth & Session Flow](#auth--session-flow)
- [WebSocket Protocol](#websocket-protocol)
- [Key Design Decisions](#key-design-decisions)

---


## 📌 Why This Project
- **실시간 통신 + 세션 관리**를 함께 다룸 (HTTP 로그인 → Redis 세션 공유 → WebSocket 인증 연계)
- **관계 상태머신**(`PENDING / ACCEPTED / REJECTED / DISCONNECTED`) 및 **비관적 락**을 통한 동시성 제어
- **확장성 고려**: Redis 기반 세션 공유, 핸들러 디스패처 구조로 메시지 타입 확장 용이

---

## 🚀 Features (현재 구현 범위)

### 🔑 인증 / 세션
- `POST /api/v1/auth/login` : JSON 로그인 (커스텀 필터 `RestApiLoginAuthFilter`)
  - 성공 → Spring Security 컨텍스트 & HTTP 세션 저장
  - 응답 본문에 **Base64 인코딩된 세션 ID** 반환
- `POST /api/v1/auth/logout` : 로그아웃 지원
- 세션 관리: **Spring Session + Redis**
  - TTL = 300초
  - `KEEP_ALIVE` 요청 시 TTL 자동 연장

### 👥 연결(친구) 관리
- **초대 (Invite)** : 초대코드로 상대 검색 → 상태 `PENDING`
- **수락 (Accept)** : 상태 `ACCEPTED` + `connectionCount` 증가
- **거절 (Reject)** : 상태 `REJECTED`
- **끊기 (Disconnect)** : 상태 `DISCONNECTED` + `connectionCount` 감소
- **목록 조회** : 상태별 연결 사용자 조회

### 📡 채널 / 메시징
- **채널 생성** : 상대 사용자 + 제목으로 채널 생성 및 참여자 알림
- **채널 입장** : 참여 시 응답 반환
- **메시지 전송** : DB 저장 + 모든 참여자에게 브로드캐스트 전송  
  _(현재는 송신자를 제외한 전체 전송, 채널 단위 분리는 향후 추가 예정)_

### ⚙️ 운영/인프라
- Swagger/OpenAPI 문서화 준비
- HikariCP 데이터소스 풀링
- Jackson + Security 모듈 직렬화 안정화

---

## 🛠 Tech Stack
- **Java 17+, Spring Boot 3**
- Spring WebSocket, Spring Security, Spring Session (Redis)
- JPA (Hibernate), MySQL
- Redis (세션 TTL & 세션 공유)
- Lombok, SLF4J

---

## 📂 Folder Structure
```
com.chatting.backend
├─ auth/ # Security 필터, UserDetailsService, Handshake 인터셉터
├─ config/ # Security / Swagger / Redis Session / WebSocket 설정
├─ constant/ # Enum 및 공용 상수 (MessageType, UserConnectionStatus 등)
├─ dto/
│ ├─ domain/ # User, UserId, Channel, Message 등 도메인 객체
│ ├─ projection/ # JPA Projection (필드 조회 최적화)
│ ├─ restapi/ # LoginRequest, UserRegisterRequest
│ └─ websocket/
│ ├─ inbound/ # Inbound Request DTO (JsonTypeInfo 사용)
│ └─ outbound/ # Response / Notification DTO
├─ entity/ # JPA 엔티티 (User, Connection, Channel, Message 등)
├─ handler/
│ ├─ websocket/ # 각 WebSocket Request 핸들러
│ └─ WebSocketHandler # 진입점 (TextWebSocketHandler 상속)
├─ json/ # ObjectMapper 유틸
├─ repository/ # Spring Data JPA Repository
├─ service/ # User / Connection / Session / Channel Service
└─ session/ # WebSocketSessionManager
```


---

## 🗄 Database Schema (요약)
- **message_user**  
  PK: `user_id` / Unique: `username`, `invite_code`  
  Fields: `connection_count`, `password`
- **user_connection**  
  PK: `(partner_a_user_id, partner_b_user_id)`  
  Fields: `status`, `inviter_user_id`
- **channel**  
  PK: `channel_id` / Fields: `title`, `invite_code`, `head_count`
- **channel_user**  
  PK: `(user_id, channel_id)` / Field: `last_read_msg_seq`
- **message**  
  PK: `message_sequence` / Fields: `username`, `content`, `created_at`

---

## 🔐 Auth & Session Flow
1. **로그인 (JSON)**
   ```http
   POST /api/v1/auth/login
   Content-Type: application/json

   { "username": "alice", "password": "pass123" }

- ✅ 성공: HTTP 200 + 세션 ID(Base64) 반환

- ❌ 실패: HTTP 401 "Not authenticated."

2. WebSocket 연결

- Endpoint: ws://localhost:8080/ws/v1/message

- 핸드셰이크 시 HTTP 세션 쿠키 함께 전송 필요

- 인증 없는 요청은 인터셉터에서 차단됨


---

## 🔄 WebSocket Protocol

Inbound (Client → Server)
```
{ "type": "INVITE_REQUEST", "userInviteCode": "xxxx" }
{ "type": "ACCEPT_REQUEST", "username": "bob" }
{ "type": "REJECT_REQUEST", "username": "bob" }
{ "type": "DISCONNECT_REQUEST", "username": "bob" }
{ "type": "CREATE_REQUEST", "title": "Chat with Bob", "participantUsername": "bob" }
{ "type": "WRITE_MESSAGE", "username": "alice", "content": "Hello!" }
{ "type": "KEEP_ALIVE" }
```

Outbound (Server → Client)
```
{ "type": "INVITE_RESPONSE", "status": "PENDING" }
{ "type": "NOTIFY_ACCEPT", "username": "alice" }
{ "type": "CREATE_RESPONSE", "channelId": { "id": 42 }, "title": "Chat with Bob" }
{ "type": "NOTIFY_MESSAGE", "username": "alice", "content": "Hello!" }
{ "type": "ERROR", "messageType": "INVITE_REQUEST", "message": "Already connected" }
```

---

## 📌 Key Design Decisions

- Spring Session + Redis로 분산 환경 세션 공유

- RequestDispatcher를 통한 WebSocket 요청 핸들러 자동 매핑

- 관계 테이블은 (minId, maxId) 정규화로 양방향 관계 단일화

- 연결 수 제한 (기본 1000명) + 비관적 락으로 동시성 제어

- JPA Projection으로 쿼리 성능 최적화







