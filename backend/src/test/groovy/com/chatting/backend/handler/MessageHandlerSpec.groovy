package com.chatting.backend.handler

import com.chatting.backend.BackendApplication
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = BackendApplication, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MessageHandlerSpec extends Specification {

    /*
    @LocalServerPort
    private int port

    private ObjectMapper objectMapper = new ObjectMapper();

    def "Group Chat Basic Test"() {
        given:
        def url = "ws://localhost:${port}/ws/v1/message"

        //client를 3명 만들거다.
        def (clientA, clientB, clientC) = [createClient(url), createClient(url), createClient(url)] //groovy에서 지원하는 형식


        when:
        clientA.session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Message("clientA", "안녕. 나는 A야"))))

        then:
        //clientA는 자신이 메시지를 보낸 것이기 때문에 queue가 비어있어야 한다. B와 C는 queue가 쌓여있어야 한다(A가 보낸 메시지가 queue에 있어야 함)
        clientA.queue.isEmpty()
        clientB.queue.poll(1, TimeUnit.SECONDS)?.contains("clientA") //B의 queue에는 1개가 있어야 햔다.
        clientC.queue.poll(1, TimeUnit.SECONDS)?.contains("clientA") //C의 queue에는 1개가 있어야 햔다.


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
     */
}
