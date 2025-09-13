# 💬 Real-Time Chat Platform (Spring Boot / WebSocket / Redis)

<img width="1720" height="900" alt="readme" src="https://github.com/user-attachments/assets/0b1e09c5-cbc1-45d1-ad21-7f6c19b32ce7" />

<br><br>

# 프로젝트 소개
- 인증 시스템으로 시작하는 친구 기능 , 1:1 채팅 기능, 단체 채팅, 목업 푸쉬 알림 서비스를 제공하는 프로젝트입니다.
- 대규모 트래픽을 견딜 수 있는 실시간 채팅 플랫폼으로 실제 메신저 서비스와 유사한 구조로 개발했습니다.
- 단일 서버에서 MSA로의 단계별 전환 과정을 통해 확장성과 안정성을 확보했습니다.
- **Spring Boot + WebSocket + Spring Security + Spring Session (Redis) + JPA(Hibernate)** 기반으로 설계되었습니다.

### 핵심 목표
- 실시간 메시지 처리 시스템 구현
- 대용량 트래픽 분산 처리 아키텍처 설계
- 고가용성(High Availability) 시스템 구축
- 모놀리틱에서 MSA로의 점진적 전환 실습

### 핵심 포인트
- **WebSocket 기반 양방향 통신**: 요청/응답 + 서버 푸시(Notification) 구조.
- **친구(연결) 관리 도메인**: 초대/수락/거절/끊기까지의 전 lifecycle 보장.
- **채널(대화방) 도메인**: Direct 채널 생성, 입장, 세션-채널 활성 상태 관리.
- **데이터 무결성·경합 제어**: 복합키, 비관적 락, 정규화 기준(canonical ordering).
- **확장성 로드맵 내재**: 캐시·리드 리플리카·카프카·샤딩·LB·서비스 디스커버리.
  

<br>

---

<br>

# ⚙️ 기술 스택

- **Backend**: Spring Boot, Spring Security, Spring WebSocket, Spring Data JPA
- **Frontend**: Vue.js
- **DB**: MySQL, Redis, ShardingSphere
- **Messaging**: Apache Kafka, WebSocket
- **LB & Service Discovery**: Nginx, Consul
- **Infra&DevOps**: Docker, Docker Compose, Nginx, Consul
- **Testing**: JUnit, Spock
- **Etc.**: Gradle, Git, GitHub

<br>

---

<br>

# 구현한 기능

### **연결(친구) 도메인**

- **초대 (Invite)**: InviteCode 기반, 상태 `PENDING` 저장.
- **수락 (Accept)**: 초대한 사람 검증 + 락으로 connection_count 안전 증가.
- **거절 (Reject)**: `PENDING`일 때만 가능.
- **끊기 (Disconnect)**: 상태 `ACCEPTED` → `DISCONNECTED`.
- **상태 조회 (getUserByStatus)**: 특정 상태별 사용자 목록 조회.

### 채널(대화방) 도메인

- **채널 생성 (Create)**: Direct 채널 생성, 요청자/상대방 등록.
- **채널 입장 (Enter)**: 가입 여부 검증, 성공 시 활성 채널 Redis 기록.
- **채널 목록 조회**: 내가 속한 채널 리스트 반환.
- **채널 나가기**: 채널 탈퇴 및 `channel_user` 삭제.

### 메시징 도메인

- **메시지 작성 (WriteMessage)**: WebSocket 기반 전송.
- **읽음 상태 관리**: `last_read_msg_seq` 기반 안 읽은 메시지 수 계산.
- **오프라인 푸시 알림**: 접속하지 않은 사용자에게 알림 발송.

### 인프라/세션 관리

- **Spring Security 로그인/세션**
- **Redis TTL 기반 세션 관리**
- **Redis 캐싱**: 사용자·채널 메타데이터 캐시.
- **MySQL 리드 리플리카 연동**: 읽기 부하 분산.
- **Kafka 도입**: 알림 서비스 분리(단방향).
- **Kafka 양방향 연동**: 채팅·연결·알림·인증 서비스 간 통합.
- **인증 서비스 분리 + Nginx LB**
- **DB 샤딩 / Redis 클러스터링**
- **Nginx 다중 인스턴스 + Consul 서비스 디스커버리**

<br>

---

<br>


## 📂 Folder Structure
- server/backend : 채팅 프로젝트의 백엔드 코드가 들어있습니다. 
- server/message-client : 백엔드 코드에서 websocket 기능을 테스트 할 수 있는 코드가 들어있습니다.
- client : 프론트엔드 코드가 들어있습니다.
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

## 🗄 데이터베이스 스키마(요약)
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


<br>

---

<br>

# 🔄 WebSocket Protocol

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






