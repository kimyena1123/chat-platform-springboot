# 질문1)
WebSocketSessionManager 클래스의 sendMessage 메서드에서 WebSocketSession session 파라미터가 있다.

이 session 파라미터는 보내는 사람의 세션을 의미하는 것인가? 아니면 받는 사람의 세션을 의미하는 것인가? 

# 질문2)

WriteMessageRequestHandler의 handleRequest()와 webSocketSessionManager의 sendMessage()의 역할 차이(기능 차이) 


# 질문3)
WriteMessageRequestHandler, InviteRequestHandler, WebSocketSessionmanager 관계와 흐름은?

---

# Q1)

항상 "메시지를 받을 대상(수신자)"의 세션이다!

이 메서드는 "어떻게 전송하느냐"만 담당하는 저수준 전송 유틸이라서, 호출하는 쪽에서 누구에게 보낼지 결정해서 그 대상의 WebSocketSession을 넘겨줘야 한다. 

그래서 상황별로 달라진다

- 보낸 사람에게만(ACK/응답) 보내고 싶으면 -> sendMessage(senderSessionm...) 
- 상대방에게 보내고 싶으면 -> 상대 세션은 찾아서 -> sendMessage(partnerSession, ...)
- 여러명에게(방 전체 브로드캐스트) 보내고 싶으면 -> 대상 세션들을 돌면서 각각 sendMessage(각 대상 session, ...)


# Q2)

메시지를 상대방에게 전달하는 역할! WriteMessageRequestHandler VS sendMessage()는 뭐가 다른가? 

=> 두 개의 책임(레이어)가 다르다. 

- WriteMessageRequestHander(비즈니스/오케스트레이션 레이어)
  - 무엇을/누가/누구에게 보낼지 결정
  - 도메인 객체 만들고, DB 저장하고, 수신자 목록을 결정하고, 그 다음 전송을 지시
  - 즉, "보낼 대상을 고르고", "보낼 내용을 만들고", "전송을 호출하는" 연출자


- WebSocketSessionManager.sendMessage 메서드(인프라/전송 레이어)
  - 어떻게 보낼지만 수행(JSON 직렬화 -> TextMessage -> WebSocketSession.sendMessage)
  - 네트워크 I/O 디테일 담당
  - 즉, "정해진 세션에 실재로 보내기:만 하는 집행자"


비유)

- WriteMessageRequestHandler = "누구에게 어떤 말을 전해라"라고 지시하는 팀장
- sendMessage = 그 말을 실제로 전하는 배달원


현재 WriteMessageRequestHandler 코드는

요청 해석 > Message 도메인 셍성 > DB 저장 > 대상 세션들 골라서 > 각 대상에 대해 sendMessage(...) 호출


# Q3)

각자의 역할

- WriteMessageRequestHandler
  - "채팅 메시지 보내기" 요청을 처리한느 비즈니스 핸들러
  - 메시지 도메인 생성, 저장, 수신자 선정(지금은 전체 브로드케스트), 전송 호출
- InviteRequestHandler
  - "초대하기" 요청을 처리하는 비즈니스 핸들러
  - UserConnectionService로 도메인 규칙(유효성/상태 전이) 처리
  - 보낸 사람에게는 **초대 결과 응답**을, 상대방에게는 **초대 알림**을 전송한다. 
  - 전송 자체는 WebSocketSessionManager.sendMessage()에게 위임
- WebSocketSessionManager
  - 세션 저장/ 조회/ 종료 + 실제 전송을 담당하는 인프라 매니저
  - key를 UserId로 해서 세션을 관리(putSessions, getSession, getSessions, closeSession)
  - sendMessage로 정해진 세션(수신자)에게 JSON 메시지 전송


### 전체 동작 흐름(예: 메시지 전송)

1. 클라이언트가 WriteMessageRequest JSON을 보낸다
2. WebSocketHandler.handleTextMessage -> JSON 파싱 -> RequestHandleDispatcher.dispatchRequest(...)호출
3. RequestHandlerDispatcher -> 요청 타입에 맞는 핸들러(WriteMessageRequestHandler) 찾아서 호출
4. WriteMessageRequestHandler.handleRequest
   - 도메인 Message 생성
   - MessageRepository에 저장
   - 수신자 세션들(지금은 getSessions()로 전체) 순회
   - 각 대상에 대해 webSocketSessionManager.sendMessage(대상 세션, 메시지) 호출
5. WebSocketSessionManager.sendMessage
   - 메시지 JSON 직렬화 -> TextMessage 생성 -> 대상 세션에 전송


### 초대 흐름(요약)
1. 클라이언트가 InviteRequest JSON 전송
2. 핸들러 디스패처가 InviteRequestHandler로 라우팅
3. InviteRequestHandler.handleRequest
   - UserConnectionService.invite(...) 로 도메인 로직 처리
   - 보낸 사람에게 InviteResponse 전송
   - 상대방 세션을 getSession(partnerUserId)로 찾아 초대 알림 전송
4. 전송은 모두 WebSocketSessionManager.sendMessage 가 실제 수행
















