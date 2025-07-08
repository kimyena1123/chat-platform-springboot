package com.messagesystem.backend.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.messagesystem.backend.BackendApplication
import com.messagesystem.backend.dto.Message
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
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
 * SpringBootTest를 이용한 통합 테스트 클래스
 * 실제 서버를 랜덤 포트에 띄우고 WebSocket 연결 및 송수신을 테스트함
 */
@SpringBootTest(classes = BackendApplication, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MessageHandlerSpec extends Specification {

    @LocalServerPort
    private int port

    private ObjectMapper objectMapper = new ObjectMapper(); // JSON 처리용 객체

    /**
     * 실제 테스트 케이스 메서드
     */
    def "Direct Chat Basic Test"() {
        given: //테스트 준비 단계

        //테스트용 WebSocket 서버 주소 생성, localhost + 실제 랜덤 포트 + 경로
        def url = "ws://localhost:${port}/ws/v1/message"

        //메시지 수신을 위한 큐 생성, 크기 10
        BlockingQueue<String> leftQueue = new ArrayBlockingQueue<>(10)
        BlockingQueue<String> rightQueue = new ArrayBlockingQueue<>(10)

        // 각 클라이ㅇ너트가 서버에 접속할 때 보낼 헤더 생성 및 이름(name) 지정
        def headers1 = new WebSocketHttpHeaders()
        headers1.add("name", "yena1")

        def headers2 = new WebSocketHttpHeaders()
        headers2.add("name", "yena2")

        //WebSocket 클라이언트 생성(왼쪽 클라이언트)
        def leftClient = new StandardWebSocketClient()
        //서버에 접속하고 메시지 수신 시 동작할 핸들러 지정
        def leftWebSocketSession = leftClient.execute(new TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                        //서버로부터 메시지를 받으면 JSON 파싱
                        def json = objectMapper.readTree(message.payload)
                        def content = json.get("content").asText()

                        // 입장 메시지 ("님이 입장하셨습니다.")는 테스트에서 무시하고 실제 채팅 메시지만 큐에 저장
                        if (!content.contains("님이 입장하셨습니다.")) {
                            leftQueue.put(message.payload) //큐에 메시지 저장(동기적 처리)
                        }
                    }
                }, headers1, URI.create(url)  // 헤더와 URL 같이 넘겨서 연결
        ).get() // 연결이 완료될 때까지 기다림 (동기 처리)

        // WebSocket 클라이언트 생성 (오른쪽 클라이언트)
        def rightClient = new StandardWebSocketClient()
        def rightWebSocketSession = rightClient.execute(
                new TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                        def json = objectMapper.readTree(message.payload)
                        def content = json.get("content").asText()
                        if (!content.contains("님이 입장하셨습니다.")) {
                            rightQueue.put(message.payload)
                        }
                    }
                }, headers2, URI.create(url)
        ).get()

        when:// 테스트 대상(행위) 수행 단계

        // 왼쪽 클라이언트가 서버에 "안녕하세요." 메시지 전송
        leftWebSocketSession.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(new Message("안녕하세요.", "yena1"))
        ))

        // 오른쪽 클라이언트가 서버에 "Hello." 메시지 전송
        rightWebSocketSession.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(new Message("Hello.", "yena2"))
        ))

        then: // 기대 결과 검증 단계

        // 오른쪽 클라이언트가 왼쪽이 보낸 "안녕하세요." 메시지를 받았는지 확인 (1초 대기)
        rightQueue.poll(1, TimeUnit.SECONDS).contains("안녕하세요.")

        and:
        // 왼쪽 클라이언트가 오른쪽이 보낸 "Hello." 메시지를 받았는지 확인 (1초 대기)
        leftQueue.poll(1, TimeUnit.SECONDS).contains("Hello.")

        cleanup: // 테스트 종료 후 정리 단계

        // 각각 연결 세션을 안전하게 종료
        leftWebSocketSession?.close()
        rightWebSocketSession?.close()
    }
}
