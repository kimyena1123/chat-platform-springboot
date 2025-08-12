# WebSocketHttpSessionHandshakeInterceptor

- 이 클래스는 WebSocket 연결 직전에 로그인 세션(HttpSession) 정보를 WebSocket 세션으로 전달하는 역할을 한다. 
- 왜냐면 WebSocket은 HTTP처럼 매 요청마다 쿠키/세션 정보를 자동으로 보내지 않기 때문이다.
- 그래서 처음 WebSocket이 연결될 때 "이 사용자가 누군인지"를 확인하고, 로그인 상태를 유지할 수 있는 식별자를 WebSocket 세션에 직접 저장해둬야 한다. 

# 동작하는 시점

- beforeHandshake() 메서드는 WebSocket이 연결되기 직전에 호출된다
- 이 시점에서는 아직 HTTP 기반의 연결이므로, Spring Security의 SecurityContextHolder와 HttpSession에 접근할 수 있다. 
- 이 잠깐을 이용해서 필요한 로그인 정보를 WebSocket 세션에 옮겨 담는다.
-----

# 동작 흐름
```java
1. 클라이언트가 WebSocket 연결 요청을 보냄
2. beforeHandshake() 실행
    - 현재 요청이 Servlet 기반 HTTP 요청인지 확인
    - Spring Security에서 현재 로그인된 사용자 정보9Authentication) 가져온다
3. 로그인 상태 확인
    - Authenticaiton이 null이면 -> 로그인이 안된 상태
    - 그러면 401 Unauthorized로 응답하고 연결 거부
4. HttpSession 가져오기
    - 현재 로그인 세션(HttpSession)이 존재하는지 확인
없으면 -> 401 Unauthorized로 응답하고 연결 거부
5. 로그인한 사용자 ID 가져오기 
    - Authentication.getPrincipal() -> MessageUserDetails로 변환
    - 여기서 userId 추출
6. WebSocket 세션 속성(attributes)에 저장
    - attributes["HTTP_SESSION_ID"] = HttpSession ID
    - attributes["USER_ID"] = 로그인한 사용자 ID
7. 연결 허용
    - 이후 WebSocketHandler에서 attributes를 사용해 로그인 정보 확인 가능

```

# 왜 이런 과정을 거칠까? 
- HTTP 요청은 쿠키를 통해 세션을 자동으로 전송하지만, WebSocket은 연결 이후에는 별도의 세션 전송이 없다.
- 따라서 처음 연결할 때 사용자의 인증 정보를 WebSocket 세션에 직접 복사해둬야 나중에 메시지를 주고받을 때 "이 메시지를 보낸 사람이 누구인지" 알 수 있다. 

# 정리
```java
┌──────────────┐
│  Client      │  (이미 로그인해서 HttpSession 있음)
│              │
│  ws://...    │  WebSocket 연결 요청
└───────┬──────┘
        │
        ▼
┌────────────────────────────┐
│ beforeHandshake()          │
│                            │
│ ① 인증 객체(Authentication) 확인
│ ② HttpSession 확인
│ ③ UserId 꺼내기
│ ④ attributes에 저장
└───────┬────────────────────┘
        │
        ▼
┌────────────────────────────┐
│ WebSocketHandler           │
│ (채팅, 알림, 기타 실시간 처리) │
│                            │
│ attributes에서 UserId 사용   │
│ → 이 유저가 보낸 메시지인지 식별 │
└────────────────────────────┘

```

