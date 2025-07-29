package com.messagesystem.backend.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.messagesystem.backend.BackendApplication
import com.messagesystem.backend.dto.domain.Message
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

@SpringBootTest(classes = BackendApplication, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MessageHandlerSpec extends Specification {

    @LocalServerPort
    int port

    ObjectMapper objectMapper = new ObjectMapper();

    def "Group Chat Basic Test"() {
        given:
        def url = "ws://localhost:${port}/ws/v1/message"

        //client를 3명 만들거다.
        def (clientA, clientB, clientC) = [createClient(url), createClient(url), createClient(url)] //groovy에서 지원하는 형식


        when:
        clientA.session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Message("clientA", "안녕. 나는 A야"))))
        clientB.session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Message("clientB", "안녕. 나는 B야"))))
        clientC.session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Message("clientC", "안녕. 나는 C야"))))

        then:
        def resultA = clientA.queue.poll(1, TimeUnit.SECONDS) + clientA.queue.poll(1, TimeUnit.SECONDS)
        def resultB = clientB.queue.poll(1, TimeUnit.SECONDS) + clientB.queue.poll(1, TimeUnit.SECONDS)
        def resultC = clientC.queue.poll(1, TimeUnit.SECONDS) + clientC.queue.poll(1, TimeUnit.SECONDS)

        resultA.contains("clientB") && resultA.contains("clientC")
        resultB.contains("clientA") && resultA.contains("clientC")
        resultC.contains("clientA") && resultA.contains("clientB")

        and:
        clientA.queue.isEmpty()
        clientB.queue.isEmpty()
        clientC.queue.isEmpty()

        cleanup:
        clientA.session?.close()
        clientB.session?.close()
        clientC.session?.close()
    }

    static def createClient(String url){
        //client가 3명이라고 한다면(그룹채팅 참여자가 3명이라면) 나 제외한 최소 2명은 받을 것이다.
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(5)

        def client = new StandardWebSocketClient()
        def webSocketSession = client.execute(new TextWebSocketHandler(){
           @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception{
               blockingQueue.put(message.payload)
           }
        },url).get()

        //Map으로 리턴할 거다. key: value, keay: value 형식임(groovy)
        return [queue: blockingQueue, session: webSocketSession]
    }
}
