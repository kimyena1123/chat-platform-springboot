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


@SpringBootTest(classes = BackendApplication, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MessageHandlerSpec extends Specification {

    @LocalServerPort
    int port

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserService userService

    @SpringBean
    ChannelService channelService = Stub()

    def "Group Chat Basic Test"() {
        given:
        //회원가입
        register("testuserA", "testpassA")
        register("testuserB", "testpassB")

        //로그인
        def sessionIdA = login("testuserA", "testpassA")
        def sessionIdB = login("testuserB", "testpassB")
        def (clientA, clientB) = [createClint(sessionIdA), createClint(sessionIdB)]

        channelService.getParticipantIds(_ as ChannelId) >> List.of(
                //getUserId() : username으로 userId를 찾는 메서드
                userService.getUserId("testuserA").get(),
                userService.getUserId("testuserB").get()
        )

        //현재 채널에 활동중인지
        channelService.isOnline(_ as UserId, _ as ChannelId) >> true

        when:
        //메시지 보내기
        clientA.session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new WriteMessage(new ChannelId(1), "안녕하세요. A 입니다."))))
        clientB.session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new WriteMessage(new ChannelId(1), "안녕하세요. B 입니다."))))

        then:
        def resultA = clientA.queue.poll(1, TimeUnit.SECONDS)
        def resultB = clientB.queue.poll(1, TimeUnit.SECONDS)

        resultA.contains("testuserB")
        resultB.contains("testuserA")

        and:
        clientA.queue.isEmpty()
        clientB.queue.isEmpty()

        cleanup:
        unregister(sessionIdA)
        unregister(sessionIdB)
        clientA.session?.close()
        clientB.session?.close()
    }


    def register(String username, String password) {
        def url = "http://localhost:${port}/api/v1/auth/register"
        def headers = new HttpHeaders(["Content-Type": "application/json"])

        def jsonBody = objectMapper.writeValueAsString([username: username, password: password])
        def httpEntity = new HttpEntity(jsonBody, headers)

        try {
            new RestTemplate().exchange(url, HttpMethod.POST, httpEntity, String)
        } catch (Exception ignore) {
        }
    }

    def unregister(String sessionId) {
        def url = "http://localhost:${port}/api/v1/auth/unregister"
        def headers = new HttpHeaders()

        headers.add("Content-Type", "application/json")
        headers.add("Cookie", "SESSION=${sessionId}")

        def httpEntity = new HttpEntity(headers)
        def responseEntity = new RestTemplate().exchange(url, HttpMethod.POST, httpEntity, String)
        responseEntity.body
    }

    def login(String username, String password) {
        def url = "http://localhost:${port}/api/v1/auth/login"
        def headers = new HttpHeaders(["Content-Type": "application/json"])
        def jsonBody = objectMapper.writeValueAsString([username: username, password: password])
        def httpEntity = new HttpEntity(jsonBody, headers)
        def responseEntity = new RestTemplate().exchange(url, HttpMethod.POST, httpEntity, String)
        def sessionId = responseEntity.body
        sessionId
    }

    def createClint(String sessionId) {
        def url = "ws://localhost:${port}/ws/v1/message"
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(5)

        def webSocketHttpHeaders = new WebSocketHttpHeaders()
        webSocketHttpHeaders.add("Cookie", "SESSION=${sessionId}")

        def client = new StandardWebSocketClient()
        def webSocketSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                blockingQueue.put(message.payload)
            }
        }, webSocketHttpHeaders, new URI(url)).get()

        [queue: blockingQueue, session: webSocketSession]
    }
}
