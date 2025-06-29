package com.study.integrationtest.integration

import com.study.integrationtest.entity.Book
import com.study.integrationtest.repository.BookRepository
import com.study.integrationtest.service.PushService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.spockframework.spring.SpringBean
import org.spockframework.spring.SpringSpy
import org.spockframework.spring.StubBeans
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

import java.time.Duration

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(topics = ["push.notification"], ports = [9092])
class LibraryServiceUsinAPIAndKafkaSpec extends Specification {

    @Autowired
    private MockMvc mockMvc

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker // 내장 Kafka 브로커

    @SpringSpy
    private PushService pushService // 실제 PushService는 그대로 두되, 호출 여부를 감시 가능하게 (Spy는 호출된지 확인 가능)

    @SpringBean
    private BookRepository bookRepository = Stub() // Repository는 DB 대신 Stub으로 처리


    def "도서 이용 가능 여부를 확인한다"() {
        given:
        // bookRepository에서 ISBN 1234로 조회하면 "Stub" 책을 리턴하도록 지정
        bookRepository.findBookByIsbn(_ as String) >> Optional.of(new Book("1234", "Stub", true))

        when:
        // 가상의 GET 요청 수행 (도서 이용 가능 여부 확인 API 호출)
        def resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/books/1234/availability"))

        then:
        // 응답 결과: 상태코드 200, 응답 본문: "1234 : 대출가능"
        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("1234 : 대출가능"))

        and:
        // 이 테스트에서는 Kafka 메시지가 전송되지 않아야 함
        0 * pushService.notification(_ as String)

    }

    def "대출 요청 시 도서 상태에 따른 처리 결과를 확인한다"() {

        /** <흐름 요약>
         *
         [Spock 테스트 코드]
            ↓
         [MockMvc 요청 수행: "/api/books/1234/borrow"]
            ↓
         [LibraryController 호출 → LibraryService → BookRepository + PushService]
            ↓
         [Kafka 메시지 전송 여부 확인 (Embedded Kafka)]
         */

        given:
        // 책 존재 여부에 따라 Optional<Book>을 리턴하도록 Stub 처리
        bookRepository.findBookByIsbn(_ as String) >> {
            bookExists ? Optional.of(new Book(isbn, title, available)) : Optional.empty()
        }

        // Kafka 테스트용 Consumer 생성 준비
        def topicName = "push.notification"
        def consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker)
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer)
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer)

        // Kafka Consumer 생성
        def consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer()

        // 지정한 토픽에 대한 메시지를 수신할 준비
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, true, topicName)

        when:
        // 가상의 POST 요청 수행 (책 대출 API 호출)
        def resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/books/${isbn}/borrow"))


        then:
        // HTTP 응답 코드 및 응답 본문 검증
        resultActions
                .andExpect(MockMvcResultMatchers.status().is(expectedStatus))
                .andExpect(MockMvcResultMatchers.content().string(expectedBody))

        and:
        // Kafka 메시지가 실제로 수신됐는지 확인
        def singleRecord = null
        try{
            // 1초 이내로 메시지 수신 대기
            singleRecord = KafkaTestUtils.getSingleRecord(consumer, topicName, Duration.ofSeconds(1))
        }catch(IllegalStateException ignored){

        }

        // 메시지가 있으면 값 비교, 없으면 null
        singleRecord?.value() == expectedMessage // null일 때는 value를 호출하지 않고 그냥 null을 리턴한다.

        cleanup:
        // 테스트 끝나면 Kafka consumer 닫기
        consumer.close()


        where:
        // === 파라미터화된 테스트 입력값 ===
        bookExists | isbn   | title        | available | expectedStatus | expectedBody   || expectedMessage
        true       | "1234" | "Spock"      | true      | 200            | "1234 : Spock" || "대출 완료: Spock"
        true       | "5678" | "bookTitle2" | false     | 200            | "5678 : 대출불가능" || null
        false      | "9999" | "Not used"   | true      | 200            | "9999 : 대출불가능" || null
    }

}
