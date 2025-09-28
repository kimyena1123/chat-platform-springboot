package com.chatting.backend.integration

import com.chatting.backend.BackendApplication
import com.chatting.backend.dto.domain.ChannelId
import com.chatting.backend.dto.domain.UserId
import com.chatting.backend.dto.websocket.inbound.WriteMessage
import com.chatting.backend.service.ChannelService
import com.chatting.backend.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit


/**
 * 이 테스트는 실제로 Spring Boot 애플리케이션을 띄워서
 * - REST API(/auth/register, /auth/login, /auth/unregister) 호출로 회원가입/로그인/탈퇴를 해보고,
 * - WebSocket(ws://.../ws/v1/message)으로 세 명의 사용자가 채팅방에 메시지를 보내면
 * - 서로에게 메시지가 도착하는지 검사하는 **통합 테스트**입니다.
 *
 * Spock은 Groovy로 작성하는 테스트 프레임워크이고,
 * 아래 @SpringBootTest 로 실제 서버를 랜덤 포트(RANDOM_PORT)로 기동합니다.
 */
@SpringBootTest(classes = BackendApplication, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketHandlerSpec extends Specification {

    // 서버가 랜덤 포트로 뜨기 때문에, 실제 할당된 포트를 @LocalServerPort 로 주입 받아 사용합니다.
    @LocalServerPort
    int port

    // 스프링 컨테이너에서 빈을 주입(@Autowired) 받습니다.
    // JSON 직렬화/역직렬화에 사용 (WriteMessage -> 문자열 JSON)
    @Autowired
    ObjectMapper objectMapper;

    // 테스트에서 실제 UserService(진짜 빈)를 사용합니다.
    // username -> userId 조회 등에 활용.
    @Autowired
    UserService userService;

    /**
     * @SpringBean + Stub()
     * - Spock이 제공하는 **테스트용 가짜 빈**(Test Double) 등록 방법입니다.
     * - 스프링 컨텍스트에 이미 같은 타입의 빈이 있어도 **이 테스트에서는 이 Stub으로 대체**됩니다.
     *
     * Stub() 이란?
     *  - "미리 정해둔 값만 반환하는 가짜 객체"
     *  - 목(Mock)과 달리 "호출 횟수/순서 검증"을 하지 않고, 지정한 입력에 대해 **정해진 값**만 돌려주는 역할에 집중합니다.
     *  - 여기서는 ChannelService의 실제 로직(예: DB/Redis 접근 등)을 우회하고,
     *    테스트 시나리오에 필요한 값만 간단히 "바로" 반환하게 해서
     *    테스트를 빠르고 안정적으로 만들려는 목적입니다.
     */
    @SpringBean
    ChannelService channelService = Stub()


    /**
     * Spock의 테스트 메서드 하나입니다.
     * "그룹 채팅 기초 시나리오"를 통합적으로 검증합니다.
     *
     * 시나리오 개요:
     *  1) 사용자 A,B,C 회원가입/로그인 → 각자의 HTTP 세션 ID 확보
     *  2) 그 세션 ID로 WebSocket에 접속(쿠키로 SESSION 전달)
     *  3) A/B/C가 같은 채널(1번)에 메시지를 각자 1개씩 보냄
     *  4) 각자 **상대방 두 명의 메시지만** 수신했는지 확인 (자기 메시지는 받지 않음)
     */
    def "Group Chat Basic Test"() {
        given: "A, B, C 회원가입 + 로그인 + WebSocket 접속, ChannelService는 Stub 응답 세팅"
        // 회원가입: REST API 호출 (실패 시 예외 무시 — 이미 있어도 넘어가게)
        register("testuserA", "testpassA")
        register("testuserB", "testpassB")
        register("testuserC", "testpassC")

        // 로그인: /auth/login 호출해서 **세션 ID**(문자열)를 받습니다.
        // 이 값은 이후 WebSocket 핸드셰이크 시 Cookie: SESSION=<id> 로 붙여 인증에 사용됩니다.
        def sessionIdA = login("testuserA", "testpassA")
        def sessionIdB = login("testuserB", "testpassB")
        def sessionIdC = login("testuserC", "testpassC")

        // 세션 ID를 쿠키로 넣어 WebSocket에 접속하는 "클라이언트"를 3개 만듭니다.
        // 반환값은 [queue: BlockingQueue<String>, session: WebSocketSession] 형태의 맵.
        def (clientA, clientB, clientC) = [createClint(sessionIdA), createClint(sessionIdB), createClint(sessionIdC)]


        /**
         * ChannelService Stub 응답 정의:
         * - 아래 두 줄은 "이 메서드가 어떤 인자로 호출되든(형식만 맞으면) 이 값을 반환해" 라는 뜻입니다.
         *
         *   1) getParticipantIds(ChannelId): 해당 채널 참여자 전체 목록을 반환
         *   2) getOnlineParticipantIds(ChannelId, List<UserId>): 온라인(현재 채널 화면을 보고 있다고 가정)인 사용자 목록 반환
         *
         * _ as ChannelId : "인자가 ChannelId 타입이면 아무 값이나 매칭" (Spock의 타입 매처)
         * _ as List<UserId> : "두 번째 인자가 List<UserId> 타입이면 아무 값이나 매칭"
         *
         * >> List.of(...) : Stub이 호출되면 **이 반환값을 돌려줘**라는 의미(고정 응답).
         *
         * 여기서는 "세 명 모두 참여자이자 온라인"이라고 가정해 테스트를 단순화합니다.
         * 이렇게 하면 Redis/DB 등 외부요인을 배제하고, **메시지 브로드캐스트 경로만** 검증할 수 있습니다.
         */
        channelService.getParticipantIds(_ as ChannelId) >> List.of(
                userService.getUserId("testuserA").get(),
                userService.getUserId("testuserB").get(),
                userService.getUserId("testuserC").get())

        channelService.getOnlineParticipantIds(_ as ChannelId, _ as List<UserId>) >> List.of(
                userService.getUserId("testuserA").get(),
                userService.getUserId("testuserB").get(),
                userService.getUserId("testuserC").get())

        when: "A, B, C가 채널 #1에 각자 메시지 한 개씩 전송"
        // WriteMessage DTO를 JSON으로 바꿔서 TextMessage로 보냅니다.
        // 채널 ID는 모두 1로 통일 → 한 채널 내에서의 메시지 브로드캐스트를 관찰하려는 목적
        clientA.session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new WriteMessage(new ChannelId(1), "안녕하세요. A 입니다."))))
        clientB.session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new WriteMessage(new ChannelId(1), "안녕하세요. B 입니다."))))
        clientC.session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new WriteMessage(new ChannelId(1), "안녕하세요. C 입니다."))))


        then: "각 클라이언트는 **다른 두 사람**의 메시지를 2개 수신한다"
        // 메시지 수신은 WebSocket handler에서 queue.put(payload)로 들어옵니다.
        // poll(1, SECONDS)는 1초 동안 대기하며 메시지를 꺼냅니다(없으면 null).
        // 두 번 poll 해서 이어붙인 문자열에 '상대방 두 명의 닉네임'이 포함되어 있는지 검사합니다.
        def resultA = clientA.queue.poll(1, TimeUnit.SECONDS) + clientA.queue.poll(1, TimeUnit.SECONDS)
        def resultB = clientB.queue.poll(1, TimeUnit.SECONDS) + clientB.queue.poll(1, TimeUnit.SECONDS)
        def resultC = clientC.queue.poll(1, TimeUnit.SECONDS) + clientC.queue.poll(1, TimeUnit.SECONDS)

        // A는 B와 C의 메시지를 받아야 함(자기 메시지는 보통 안 받는 설계가 많음)
        resultA.contains("testuserB") && resultA.contains("testuserC")
        // B는 A와 C의 메시지를 받아야 함
        resultB.contains("testuserA") && resultB.contains("testuserC")
        // C는 A와 B의 메시지를 받아야 함
        resultC.contains("testuserA") && resultC.contains("testuserB")


        and: "추가로 쌓인 메시지가 없어야 한다(정확히 2개만 받았는지 확인)"
        clientA.queue.isEmpty()
        clientB.queue.isEmpty()
        clientC.queue.isEmpty()

        cleanup: "마무리 - 탈퇴 요청 및 WebSocket 세션 정리"
        unregister(sessionIdA)
        unregister(sessionIdB)
        unregister(sessionIdC)

        clientA.session?.close()
        clientB.session?.close()
        clientC.session?.close()
    }


    // --------- 아래부터는 테스트 내부에서 쓰는 '헬퍼 메서드'들이다. ---------

    /**
     * 회원가입 REST 호출 (POST /api/v1/auth/register)
     * - 이미 같은 사용자로 가입돼 있을 수도 있어서, 실패(Exception)는 무시합니다.
     */
    def register(String username, String password) {
        def url = "http://localhost:${port}/api/v1/auth/register"
        def headers = new HttpHeaders(["Content-Type": "application/json"])
        def jsonBody = objectMapper.writeValueAsString([username: username, password: password])
        def httpEntity = new HttpEntity(jsonBody, headers)

        try {
            new RestTemplate().exchange(url, HttpMethod.POST, httpEntity, String)
        } catch (Exception ignore) {
            // 중복 가입 등은 테스트 편의상 무시
        }
    }


    /**
     * 회원탈퇴 REST 호출 (POST /api/v1/auth/unregister)
     * - 중요한 점: HTTP 요청에 **로그인 때 받은 세션 ID를 Cookie 로 넣어** 인증합니다.
     *   (스프링 시큐리티가 세션 기반 인증을 쓰고 있다는 가정)
     */
    def unregister(String sessionId) {
        def url = "http://localhost:${port}/api/v1/auth/unregister"
        def headers = new HttpHeaders()

        headers.add("Content-Type", "application/json")
        headers.add("Cookie", "SESSION=${sessionId}") // 세션 쿠키 붙이기

        def httpEntity = new HttpEntity(headers)
        def responseEntity = new RestTemplate().exchange(url, HttpMethod.POST, httpEntity, String)

        responseEntity.body
    }


    /**
     * 로그인 REST 호출 (POST /api/v1/auth/login)
     * - 바디에 username/password 를 보내고, 응답 바디로 **세션 ID**(문자열)를 받는 API라고 가정합니다.
     * - 반환된 세션 ID는 이후 WebSocket 연결에도 사용됩니다.
     */
    def login(String username, String password) {
        def url = "http://localhost:${port}/api/v1/auth/login"
        def headers = new HttpHeaders(["Content-Type": "application/json"])
        def jsonBody = objectMapper.writeValueAsString([username: username, password: password])
        def httpEntity = new HttpEntity(jsonBody, headers)
        def responseEntity = new RestTemplate().exchange(url, HttpMethod.POST, httpEntity, String)
        def sessionId = responseEntity.body

        sessionId
    }


    /**
     * 주어진 세션 ID로 **WebSocket** 클라이언트를 생성해 서버에 연결합니다.
     *
     * 핵심 포인트:
     * - WebSocket도 최초 '핸드셰이크'는 HTTP 위에서 진행되므로,
     *   이때 **Cookie: SESSION=<id>** 를 넣어주면 서버는 로그인된 사용자로 인식합니다.
     * - 서버가 실제로 /ws/v1/message 엔드포인트를 제공하고 있어야 하며,
     *   TextWebSocketHandler를 통해 들어오는 메시지를 **BlockingQueue**에 적재하여 테스트에서 꺼내볼 수 있게 합니다.
     */
    def createClint(String sessionId) { // (오타지만 테스트엔 영향 없음: createClient가 더 자연스럽긴 합니다)
        def url = "ws://localhost:${port}/ws/v1/message"

        // 테스트에서 수신 메시지를 임시로 담아둘 큐(최대 5개까지 저장).
        // poll(timeout) 으로 안전하게 꺼내며, 없으면 null을 반환함(테스트가 영원히 멈추지 않도록).
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(5)

        // WebSocket 핸드셰이크 시 보낼 HTTP 헤더
        def webSocketHttpHeaders = new WebSocketHttpHeaders()
        webSocketHttpHeaders.add("Cookie", "SESSION=${sessionId}") // 인증을 위해 세션 쿠키 추가

        // 표준 WebSocket 클라이언트
        def client = new StandardWebSocketClient()

        // 서버에 실제 연결을 수행.
        // 두 번째 인자로 웹소켓 핸들러를 넘기는데, 수신 메시지를 큐에 넣도록 오버라이드합니다.
        def webSocketSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                // 서버가 보낸 텍스트 메시지(payload)를 BlockingQueue에 넣어
                // 테스트 본문에서 poll()로 확인할 수 있게 합니다.
                blockingQueue.put(message.payload)
            }
        }, webSocketHttpHeaders, new URI(url)).get() // .get()으로 연결 완료까지 기다림(Future 대기)

        // 호출자가 사용하기 쉽게 '수신 큐'와 '세션 객체'를 맵으로 반환
        [queue: blockingQueue, session: webSocketSession]
    }
}