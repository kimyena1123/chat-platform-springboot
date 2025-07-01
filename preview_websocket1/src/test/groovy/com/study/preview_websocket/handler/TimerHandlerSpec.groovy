package com.study.preview_websocket.handler

import com.study.preview_websocket.PreviewWebsocketApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

// WebSocket 서버를 실제로 띄우고 테스트하기 위한 설정
@SpringBootTest(classes = PreviewWebsocketApplication, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT )
class TimerHandlerSpec extends Specification {

    // Spring이 배정한 랜덤 포트 값을 주입받음
    @LocalServerPort
    private int port;

    def "timer 동작 테스트"() {
        given:
        // 실제로 WebSocket으로 연결할 URL (ex: ws://localhost:1234/ws/timer)
        def url = "ws://localhost:${port}/ws/timer"

        // 서버로부터 받은 메시지를 저장해두기 위한 큐 (생산자-소비자 방식)
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(1)

        // WebSocket 클라이언트 객체 생성
        StandardWebSocketClient client = new StandardWebSocketClient()

        // WebSocket 연결 시 사용할 핸들러 정의 (메시지를 수신하면 queue에 넣음)
        WebSocketSession webSocketSession = client.execute(new TextWebSocketHandler(){
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception{
                queue.put(message.payload)
            }
        }, url) // 위에서 정의한 WebSocket URL로 연결 시도
            .get() // 비동기 작업의 결과를 기다림 (WebSocket 연결 완료될 때까지 대기)

        when:
        // 클라이언트가 서버에 메시지 전송 ("2"초 타이머 요청)
        webSocketSession.sendMessage(new TextMessage("2"))

        then:
        // 첫 번째 메시지는 "타이머 등록 완료" 메시지여야 함 (1초 이내 수신 기대)
        queue.poll(1, TimeUnit.SECONDS).contains("등록 완료")

        and:
        // 두 번째 메시지는 "타이머 완료" 메시지여야 함 (3초 이내 수신 기대)
        queue.poll(3, TimeUnit.SECONDS). contains("타이머 완료")

        cleanup:
        // 테스트가 끝난 후 연결 닫기
        webSocketSession.close()
    }
}
