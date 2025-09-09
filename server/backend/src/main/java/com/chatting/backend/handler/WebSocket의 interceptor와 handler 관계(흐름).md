WebSocket 연결 과정에서 interceptor -> handler로 흘러가는데, 

WebSocketHandler와 WebSOcketHttpSessionHandShakeInterceptor로 보면 된다. 

# 흐름 순서(WebSocket 연결 과정)

1. 클라이언트(WebSocket 연결 요청)
   - 브라우저나 앱이 ws:// or wss:/... 주소로 서버에 WebSocket 연결을 요청
2. Spring의 Handshake 과정 시작
   - WebSocket은 처음에는 HTTP 프로토콜로 Upgrade: websocket 요청을 보낸다
   - 이때 HandShakeInterceptor들이 먼저 실행된다.
   - 즉, WebSocketHttpSessionHandlerInterceptor.beforeHandshake()가 제일 먼저 실행된다. 
3. Interceptor(beforeHandshake)
   - HttpSession, SecurityContext 등 HTTP 기반 정보를 읽을 수 있는 마지막 타이밍.
   - 여기서 attributes라는 Map에 데이터를 넣어둔다. 
```java
attributes.put(Constants.USER_ID.getValue(), new UserId(messageUserDetails.getUserId()));
attributes.put(Constants.HTTP_SESSION_ID.getValue(), httpSession.getId());
```
이 attributes Map은 WebSocketSession에 그대로 복사되어 전달된다
4. Handshake 성공 -> WebSocket 연결 생성
   - interceptor가 true를 반환하면 핸드세이크 성공
   - 이제 WebSocket 연결이 확정되고 WebSocketSession 객체가 만들어진다
   - 이때 interceptor에서 넣은 attributes가 WebSocketSession에 탑재된다.
5. WebSocketHandler 동작
   - 이제부터 메시지 송수신, 연결 이벤트(connected/closed)는 전부 WebSocketHandler가 처리한다. 
   - afterConnectionEstablished()가 실행되면서 session.getAttributes()에서 interceptor가 심어둔 USER_ID, HTTP_SESSION_ID를 꺼낼 수 있다. 
   - 그래서 아래와 같이 UserId 객체로 꺼낼 수 있다 (intercpetor에서 UserId로 감싸서 넣었기 떄문에 가능하다)
```java
UserId userId = (UserId) session.getAttributes().get(Constants.USER_ID.getValue());
```

# 동작도(흐름)
```java
[Client] 
   │  (HTTP Upgrade 요청)
   ▼
[Spring Handshake 과정 시작]
   │
   │--> (1) WebSocketHttpSessionHandshakeInterceptor.beforeHandshake()
   │         - 인증(Authentication) 확인
   │         - HttpSession 확인
   │         - attributes에 USER_ID, HTTP_SESSION_ID 저장
   │
   ▼
[Handshake 성공 → WebSocketSession 생성]
   │
   │--> (2) WebSocketHandler.afterConnectionEstablished()
   │         - session.getAttributes()에서 USER_ID, HTTP_SESSION_ID 꺼냄
   │         - WebSocketSessionManager에 등록
   │
   ▼
[이후]
   │
   │--> handleTextMessage() 호출
   │         - 들어오는 메시지 처리
   │         - dispatcher로 분배
   │
   │--> handleTransportError(), afterConnectionClosed() 등 이벤트 발생 시 처리

```

# 정리
Interceptor가 먼저 실행되고, 거기서 attributes를 심는다.

그 후에 WebSocketHandler가 실행되며, session.getAttributes()에서 Interceptor가 심어둔 데이터를 그대로 꺼내 쓴다.

따라서 Interceptor에서 넣을 때 new UserId(...)로 감싸서 넣었으니, Handler에서 (UserId)로 형변환해서 꺼낼 수 있는 것.

이 둘은 attributes라는 매개체로 연결되어 있다.

# 전체 흐름
1. 사용자 로그인(HTTP 기반)
   - 사용자가 /login 같은 엔드포인트를 통해 로그인
   - Spring Security가 로그인 성공 시, HttpSession을 생성(혹은 기존 세션 재사용)
   - 이 HttpSession은 세션ID(JSESSIONID)를 발급
2. 클라이언트가 WebSocket 연결 요청
   - 브라우저는 ws://localhost:8080/ws/chat 같은 경로로 WebSocket 핸드세이크(Upgrade) 요청을 보낸다
   - 이 요청에는 기존 로그인 세션(JSESSIONID 쿠키)도 함께 전송된다
   - 그래서 서버는 "이 사용자가 누구인지" 확인할 수 있다
3. Spring이 Handshake Interceptor 실행
   - 서번느 WebSocket 연결을 수락하기 전에 HandshakeInterceptor들을 실행한다. 
   - 이때 실행되는 것이 "WebSocketHttpSessionHandshakeInterceptor.beforeHandshake()"이다. 
   - 여기서 하는일: 
     - SecurityContextHolder를 통해 로그인한 사용자 인증 정보(Authentication) 확인
     - ServletServerHttpRequest.getServletRequest().getSession(false)를 호출해서 이미 존재하는 HttpSession을 가져온다(false를 주었으므로 새로 생성하지 않고, 기존 로그인 세션만 가져온다)
     - 즉, 여기서 가져오는 HttpSession은 "**로그인할 때 생성된 세션**"이다. 
     - 그 HttpSession의 ID와 UserId를 꺼내서 **WebSocket의 attributes(Map)에 심어둔다
   ```java
   attributes.put(Constants.USER_ID.getValue(), new UserId(messageUserDetails.getUserId()));
   attributes.put(Constants.HTTP_SESSION_ID.getValue(), httpSession.getId());
   ```
4. Handshake 성공 -> WebSocketSession 생성
   - Handshake가 정상적으로 끝나면 이제 WebSocketSession 객체가 만들어진다. 
   - 이 WebSOcketSession 안에, Interceptor가 attribuetes에 넣은 값들이 복사되어 들어간다. 
5. WebSocketHandler 실행
   - 이제부터 연결 이벤트는 WebSocketHandler가 담당한다. 
   - 예) afterConnectionEstablished(), handleTextMesasge(), afterConnectionClosed()..
   - 여기서 session.getAttributes()를 통해 interceptor가 넣어둔 값에 접근 가능하다
```java
UserId userId = (UserId) session.getAttributes().get(Constants.USER_ID.getValue());
String httpSessionId = (String) session.getAttributes().get(Constants.HTTP_SESSION_ID.getValue());
```

# 질문)
3번 interceptor 생성하는 곳 설명을 보면 WebSocket의 attributes(map)에 심어둔다고 한다. 그리고 4번 WebSocketSession이 생성된다고 하는데 WebSocketSession이 생성되고 나서 attributes에 넣는게 아니라 attributes에 넣어두고 나서 WebSocketSession이 생성되는 것인가


- Spring WebSocket은 WebSocketSession을 만들기 직전에 모든 HandshakeInterceptor를 호출한다.
  - 이 단계에서 인자로 넘어오는 attributes는 실제로는 곧 만들어질 **WebSocketSession에 복사될 준비용 Map**이다.
  - 즉, 아직 WebSocketSession은 존재하지 않고, Interceptor들이 이 Map에 데이터를 담아두는 거다.
```java
public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes) { // 👈 준비용 Map
    // 여기서 attributes.put("USER_ID", new UserId(...)) 해둠
}
```
- Handshake가 성공적으로 끝나면,Spring이 새로운 WebSocketSession 객체를 생성한다.
  - 그리고 이때 Interceptor에서 attributes에 넣어둔 값들을 WebSocketSession의 attributes로 복사한다. 


- 이제 Handler 호출한다; afterConnectionEstablished(WebSocketSession session)이 호출된다
  - 이 시점에 넘겨받은 session에는 이미 attributes가 채워져 있다. 


- 즉, 순서: Interceptor → attributes(Map)에 데이터 넣음 → Handshake 성공 → WebSocketSession 생성 & attributes 복사 → WebSocketHandler 실행
  - WebSocketSession이 생성된 뒤에 attributes를 넣는 게 아니라,Interceptor가 먼저 attributes(Map)에 심어두고, WebSocketSession이 만들어질 때 그게 복사되어 들어가는 구조인 것!!!





